package com.ai.agent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ai.agent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 保存思考结果（当不需要调用工具时，直接返回 AI 的回答）
    private String thinkResult;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 执行单个步骤：思考和行动
     * 覆盖父类方法，当不需要行动时返回 AI 的实际回答
     */
    @Override
    public String step() {
        try {
            // 先思考
            boolean shouldAct = think();
            if (!shouldAct) {
                // 返回 AI 的实际回答，而不是固定提示
                return thinkResult != null ? thinkResult : "思考完成";
            }
            // 再行动
            return act();
        } catch (Exception e) {
            e.printStackTrace();
            return "步骤执行失败：" + e.getMessage();
        }
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);

            // 如果不需要调用工具，说明 AI 已经给出最终答案，结束循环
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                // 保存思考结果（AI 的实际回答）
                this.thinkResult = result;
                // 设置状态为完成，结束循环
                setState(AgentState.FINISHED);
                return false;
            } else {
                // 记录选择的工具（用于日志）
                log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
                String toolCallInfo = toolCallList.stream()
                        .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.joining("\n"));
                log.info(toolCallInfo);
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // 任务结束，更改状态
            setState(AgentState.FINISHED);
            // 终止时不需要润色，直接返回
            return "任务已完成";
        }

        // 收集工具执行结果
        List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();

        // 使用 AI 润色工具执行结果，生成人类可读的描述
        String polishedResult = polishToolResults(responses);

        log.info(polishedResult);
        return polishedResult;
    }

    /**
     * 获取最终结果（重写父类方法）
     * 优先返回 AI 的直接回答，只有在需要时才生成总结
     */
    @Override
    protected String getFinalResult() {
        // 首先尝试获取 AI 的直接回答（thinkResult）
        if (thinkResult != null && !thinkResult.isEmpty()) {
            return thinkResult;
        }

        // 尝试从最后一条助手消息获取结果
        String parentResult = super.getFinalResult();

        // 检查是否调用过工具（通过检查是否有 ToolResponseMessage）
        boolean hasToolCalls = false;
        StringBuilder fileLinks = new StringBuilder();
        List<Message> messages = getMessageList();

        for (Message message : messages) {
            if (message instanceof ToolResponseMessage) {
                hasToolCalls = true;
                ToolResponseMessage toolResponseMessage = (ToolResponseMessage) message;
                for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                    String toolName = response.name();
                    String toolResult = response.responseData();
                    // 检查是否包含文件相关的工具结果
                    if (toolResult != null && (
                            toolName.contains("write") ||
                            toolName.contains("download") ||
                            toolName.contains("Markdown") ||
                            toolResult.contains("/api/file/") ||
                            toolResult.contains("download") ||
                            toolResult.contains("preview"))) {
                        fileLinks.append("- 工具 ").append(toolName).append(" 结果: ").append(toolResult).append("\n");
                    }
                }
            }
        }

        // 如果没有调用过工具，且有有效的直接回答，直接返回
        if (!hasToolCalls && parentResult != null && !parentResult.isEmpty()) {
            return parentResult;
        }

        // 如果有有效的回答且没有文件链接需要处理，直接返回
        if (parentResult != null && parentResult.length() > 50 && fileLinks.length() == 0) {
            return parentResult;
        }

        // 只有在调用了工具的情况下，才让 AI 生成总结
        if (!hasToolCalls) {
            return parentResult != null ? parentResult : "";
        }

        // 让 AI 基于对话历史生成总结（包含文件链接）
        try {
            String fileLinksInfo = fileLinks.length() > 0
                ? "\n\n【重要】以下是本次任务中生成/下载的文件信息，必须在回答中包含这些链接：\n" + fileLinks.toString()
                : "";

            String summaryPrompt = String.format("""
                    请基于以上的对话和工具调用结果，生成一个完整的最终回答给用户。

                    要求：
                    1. 总结已经收集到的所有有用信息
                    2. 如果生成了文件或下载了资源，【必须】在回答中包含完整的预览链接和下载链接
                    3. 链接格式示例：[预览文件](/api/file/preview?fileName=xxx) | [下载文件](/api/file/download?fileName=xxx)
                    4. 给出有价值的建议或结论
                    5. 使用清晰的格式（可以使用 Markdown）
                    6. 包含风险提示（如果涉及投资建议）
                    %s
                    请直接给出回答，不要说"根据以上信息"之类的开头。
                    """, fileLinksInfo);

            List<Message> messagesWithSummaryRequest = new ArrayList<>(getMessageList());
            messagesWithSummaryRequest.add(new UserMessage(summaryPrompt));

            Prompt prompt = new Prompt(messagesWithSummaryRequest);
            String summary = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .call()
                    .content();

            return summary != null ? summary : "";
        } catch (Exception e) {
            log.warn("生成最终总结失败: " + e.getMessage());
            // 失败时尝试返回父类的结果
            return super.getFinalResult();
        }
    }

    /**
     * 使用 AI 润色工具执行结果，生成人类可读的描述
     *
     * @param responses 工具响应列表
     * @return 润色后的结果描述
     */
    private String polishToolResults(List<ToolResponseMessage.ToolResponse> responses) {
        if (responses.isEmpty()) {
            return "未执行任何工具";
        }

        // 构建工具执行摘要
        StringBuilder summary = new StringBuilder();
        for (ToolResponseMessage.ToolResponse response : responses) {
            String toolName = response.name();
            String toolResult = response.responseData();

            // 限制结果长度，避免过长
            String truncatedResult = toolResult.length() > 500
                ? toolResult.substring(0, 500) + "..."
                : toolResult;

            summary.append(String.format("工具名称: %s\n结果: %s\n\n", toolName, truncatedResult));
        }

        // 使用 AI 润色结果
        String polishPrompt = String.format("""
                请用简洁、易懂的语言总结以下工具执行的结果。

                要求：
                1. 用自然语言描述工具做了什么
                2. 提取关键信息（如搜索到了什么、下载了什么、生成了什么）
                3. 忽略技术细节（如 JSON 格式、HTML 代码）
                4. 保持简洁，1-2句话即可
                5. 使用第一人称（"我"）来描述

                工具执行结果：
                %s

                请用一句话总结：
                """, summary.toString());

        try {
            String polished = getChatClient().prompt()
                    .user(polishPrompt)
                    .call()
                    .content();

            return polished != null ? polished.trim() : summary.toString();
        } catch (Exception e) {
            log.warn("AI 润色失败，返回原始摘要: " + e.getMessage());
            // 如果 AI 润色失败，返回简化的原始摘要
            return responses.stream()
                    .map(response -> String.format("✓ 使用了 %s 工具", response.name()))
                    .collect(Collectors.joining("\n"));
        }
    }
}
