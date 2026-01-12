package com.ai.agent.agent;

import cn.hutool.core.util.StrUtil;
import com.ai.agent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * <p>
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 10;

    // LLM 大模型
    private ChatClient chatClient;

    // Memory 记忆（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        // 1、基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 2、执行，更改状态
        this.state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 执行循环
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3、清理资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时
        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            // 1、基础校验
            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
            // 2、执行，更改状态
            this.state = AgentState.RUNNING;
            // 记录消息上下文
            messageList.add(new UserMessage(userPrompt));
            // 保存结果列表
            List<String> results = new ArrayList<>();
            try {
                // 执行循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);

                    // 【关键优化】在倒数第二步时，给 AI 注入警告消息
                    if (stepNumber == maxSteps - 1 && state != AgentState.FINISHED) {
                        String warningMessage = String.format(
                            "【重要警告】你现在在第 %d 步，只剩下 1 步了！下一步（第 %d 步）必须是最后一步。" +
                            "如果你还没有完成任务，请立即：\n" +
                            "1. 停止继续收集信息\n" +
                            "2. 基于已有信息总结结果\n" +
                            "3. 在下一步使用 terminate 工具返回最终答案\n" +
                            "如果已经完成任务，请立即使用 terminate 工具。",
                            stepNumber, maxSteps);
                        messageList.add(new UserMessage(warningMessage));
                        log.warn("注入步数警告: {}", warningMessage);
                    }

                    // 单步执行
                    String stepResult = step();
                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);

                    // 检查状态，如果已经 FINISHED，跳出循环，不再发送消息
                    if (state == AgentState.FINISHED) {
                        break;
                    }

                    // 输出当前每一步的结果到 SSE（JSON格式）
                    String jsonMessage = String.format("{\"type\":\"thinking\",\"step\":%d,\"content\":\"%s\"}",
                        stepNumber, escapeJson(stepResult));
                    sseEmitter.send(jsonMessage);
                }
                // 检查是否超出步骤限制
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    results.add("Terminated: Reached max steps (" + maxSteps + ")");
                    String terminateMessage = String.format("{\"type\":\"terminate\",\"reason\":\"达到最大步骤（%d）\"}", maxSteps);
                    sseEmitter.send(terminateMessage);
                }
                // 发送最终结果（流式）
                String finalResultMessage = getFinalResult();
                if (StrUtil.isNotBlank(finalResultMessage)) {
                    // 发送开始标记
                    sseEmitter.send("{\"type\":\"result_start\"}");

                    // 流式输出最终结果
                    streamFinalResult(finalResultMessage, sseEmitter);

                    // 发送结束标记
                    sseEmitter.send("{\"type\":\"result_end\"}");
                }
                // 发送流结束标记（前端依赖这个标记来判断流是否结束）
                sseEmitter.send("[DONE]");
                // 正常完成
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                try {
                    sseEmitter.send("执行错误：" + e.getMessage());
                    sseEmitter.send("[DONE]");  // 即使出错也要发送结束标记
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 3、清理资源
                this.cleanup();
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }

    /**
     * 定义单个步骤
     *
     * @return
     */
    public abstract String step();

    /**
     * 获取最终结果（供子类重写）
     */
    protected String getFinalResult() {
        // 默认从最后一条助手消息中获取结果
        if (messageList.isEmpty()) {
            return "";
        }
        // 从后往前找第一条助手消息
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message message = messageList.get(i);
            if (message instanceof org.springframework.ai.chat.messages.AssistantMessage) {
                org.springframework.ai.chat.messages.AssistantMessage assistantMessage =
                    (org.springframework.ai.chat.messages.AssistantMessage) message;
                String content = assistantMessage.getText();
                if (StrUtil.isNotBlank(content)) {
                    return content;
                }
            }
        }
        return "";
    }

    /**
     * 流式输出最终结果
     *
     * @param content 要输出的内容
     * @param sseEmitter SSE发送器
     */
    protected void streamFinalResult(String content, SseEmitter sseEmitter) throws IOException {
        if (StrUtil.isBlank(content)) {
            return;
        }

        // 按字符逐个发送，模拟打字效果
        // 为了性能，每次发送多个字符
        int chunkSize = 3; // 每次发送3个字符
        for (int i = 0; i < content.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, content.length());
            String chunk = content.substring(i, end);
            String resultJson = String.format("{\"type\":\"result_chunk\",\"content\":\"%s\"}",
                escapeJson(chunk));
            sseEmitter.send(resultJson);

            // 添加小延迟，让打字效果更自然
            try {
                Thread.sleep(20); // 20ms 延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f");
    }

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }
}
