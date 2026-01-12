package com.ai.agent.app;

import com.ai.agent.advisor.LoggerAdvisor;
import com.ai.agent.agent.ToolCallAgent;
import com.ai.agent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 理财管理智能体（合并了对话记忆、RAG、工具调用、MCP服务和ReAct模式）
 */
@Component
@Slf4j
public class FinancialManagementApp {

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ToolCallback[] allTools;

    // 智能体对话记忆存储（基于文件持久化）
    private final FileBasedChatMemory agentMemory = new FileBasedChatMemory("./agent-memory");

    private static final String SYSTEM_PROMPT = """
            你是一个专业的AI理财管理助手。你的能力包括：
            1. 理解用户的财务目标和风险偏好
            2. 提供个性化的投资建议
            3. 分析市场趋势和投资机会
            4. 帮助用户制定预算和储蓄计划
            5. 解答理财相关问题

            开场时向用户表明身份，告知用户可以咨询理财问题。
            围绕用户的理财需求提问：
            - 询问用户的理财目标（储蓄、投资、退休规划等）
            - 了解用户的风险承受能力
            - 询问用户的收入和支出情况
            - 了解用户的投资期限

            引导用户详述他们的理财需求和困惑，以便给出专业的解决方案。

            在回答时，请保持专业、客观，并注意风险提示。投资有风险，建议仅供参考。
            """;

    /**
     * 初始化 ChatClient
     */
    public FinancialManagementApp(ChatModel dashscopeChatModel, ToolCallback[] allTools) {
        this.chatModel = dashscopeChatModel;
        this.allTools = allTools;

        // 初始化基于内存的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new LoggerAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 理财报告结构
     */
    public record FinancialReport(String title, List<String> suggestions, String riskLevel) {
    }

    /**
     * AI 理财报告功能（结构化输出）
     */
    public FinancialReport doChatWithReport(String message, String chatId) {
        FinancialReport report = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成理财报告，标题为{用户名}的理财报告，内容包括建议列表和风险等级评估")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(FinancialReport.class);
        log.info("financialReport: {}", report);
        return report;
    }

    // RAG 和 MCP 资源（用于智能体整合）

    // RAG 功能已禁用
    // @Resource
    // private VectorStore appVectorStore;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 创建超级智能体模式实例（ReAct 模式，整合记忆、RAG、工具调用、MCP）
     *
     * @param chatId 会话ID，用于持久化对话记忆
     */
    public ToolCallAgent createAgentInstance(String chatId) {
        // 1. 合并所有工具（本地工具 + MCP 工具）
        ToolCallback[] mcpTools = toolCallbackProvider.getToolCallbacks();
        ToolCallback[] combinedTools = new ToolCallback[allTools.length + mcpTools.length];
        System.arraycopy(allTools, 0, combinedTools, 0, allTools.length);
        System.arraycopy(mcpTools, 0, combinedTools, allTools.length, mcpTools.length);

        ToolCallAgent agent = new ToolCallAgent(combinedTools) {};
        agent.setName("FinancialAgent");

        String agentSystemPrompt = """
                你是一个专业的AI理财管理智能体，具备以下能力：

                核心能力：
                1. 理财知识问答 - 基于专业理财知识库回答用户问题
                2. 网络搜索 (searchWeb) - 搜索最新的财经资讯和市场动态
                3. 网页内容提取 (scrapeWebPage) - 从金融网站提取详细内容
                4. 文件操作 (readFile/writeFile) - 读写理财报告和分析文档
                5. 资源下载 (downloadResource) - 下载金融研究报告
                6. 终端命令 (executeTerminalCommand) - 执行系统命令
                7. Markdown报告 (generateMarkdown/readMarkdown) - 生成专业理财报告
                8. 任务终止 (doTerminate) - 完成任务后结束对话

                【最重要原则 - 简单问候处理】：
                当用户发送简单问候（如"你好"、"Hi"、"Hello"、"在吗"等）时：
                - 只需友好地打招呼并简短介绍自己
                - 【禁止】引用任何上下文信息或知识库内容
                - 【禁止】假设用户有任何具体需求
                - 【禁止】主动提供详细的理财建议
                - 回复示例："你好！我是AI理财智能体，可以帮你分析市场、生成报告、回答理财问题。请问有什么可以帮你的？"

                其他重要原则：
                1. 只有当用户明确提出具体问题或需求时，才使用知识库和工具
                2. 如果知识库没有相关信息，使用搜索工具获取最新资讯
                3. 高效完成任务，减少不必要的步骤
                4. 始终包含风险提示

                工具使用指南：
                - 简单问候 (你好、Hi) → 直接回答
                - 基础理财知识 (预算、储蓄、投资基础) → 使用知识库回答
                - 需要最新市场信息 → 使用 searchWeb
                - 生成报告 → 使用文件工具
                - 下载文件 → 先用 scrapeWebPage 提取直接下载链接，再用 downloadResource
                - 复杂分析任务 → 组合使用多个工具

                【下载文件注意事项】：
                - downloadResource 只能下载直接文件链接（如 .pdf、.xlsx、.zip）
                - 如果URL是网页，需要先用 scrapeWebPage 提取真实下载链接
                - 收到 "URL points to an HTML page" 错误时，说明需要提取链接

                【重要】最终结果返回规则：
                1. 无论任务成功还是失败，必须返回最终结果给用户
                2. 如果生成了文件或下载了文件，必须在最终结果中包含下载链接
                3. 如果任务失败，必须说明失败原因和已完成的部分
                4. 你最多只有6步来完成任务，如果接近步数限制仍未完成，必须：
                   - 总结已完成的工作
                   - 说明未能完成的原因
                   - 给出部分结果或建议
                5. 使用 terminate 工具时，必须提供完整的最终答案

                如果无法完成任务：
                - 明确告知用户无法完成
                - 解释缺少哪些能力
                - 建议替代方案

                记住：投资有风险，建议仅供参考。
                """;
        agent.setSystemPrompt(agentSystemPrompt);

        String nextStepPrompt = """
                根据用户需求，主动选择最合适的工具或工具组合。
                对于复杂任务，可以分解问题并逐步使用不同工具解决。
                每次使用工具后，评估是否已有足够信息回答用户问题。

                【严格步数限制】你最多只有6步，必须严格遵守以下规划：
                - 第1-3步：收集核心信息（不要过度探索）
                - 第4步：处理和生成内容（如生成报告）
                - 第5步：【最后机会】检查是否完成，如果完成必须调用 terminate，如果未完成则快速总结已有信息
                - 第6步：【强制结束】系统会强制结束，必须在第5步就调用 terminate

                【关键原则 - 避免超时】
                1. 简单任务（问答、咨询）：第1-2步就应该调用 terminate
                2. 中等任务（搜索+分析）：不超过3步，第4步必须 terminate
                3. 复杂任务（生成报告）：第4步生成，第5步必须 terminate
                4. 【禁止】在第6步才调用 terminate - 必须在第5步或之前完成

                【必须遵守】
                1. 如果信息足够，立即提供完整答案并使用 terminate 工具
                2. 如果生成了文件，最终结果必须包含下载链接
                3. 【关键】第4步后必须评估：如果接近完成，下一步必须 terminate
                4. 使用 terminate 工具时，reason 参数必须包含：
                   - 任务完成情况总结
                   - 生成的文件链接（如有）
                   - 关键信息或建议
                5. 不要浪费步骤在不必要的探索上
                6. 理财建议中包含风险提示
                """;
        agent.setNextStepPrompt(nextStepPrompt);
        agent.setMaxSteps(6);

        // 初始化 AI 对话客户端
        ChatClient agentChatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        new LoggerAdvisor(),
                        new QuestionAnswerAdvisor(appVectorStore)
                )
                .build();
        agent.setChatClient(agentChatClient);

        // 2. 加载历史对话记忆
        if (chatId != null) {
            List<Message> history = agentMemory.get(chatId);
            if (!history.isEmpty()) {
                agent.getMessageList().addAll(history);
                log.info("已加载 {} 条历史消息，chatId: {}", history.size(), chatId);
            }
        }

        // 3. 设置清理回调，保存对话记忆
        final String finalChatId = chatId;
        final FileBasedChatMemory memory = this.agentMemory;

        return new ToolCallAgent(combinedTools) {
            {
                // 复制配置
                setName(agent.getName());
                setSystemPrompt(agent.getSystemPrompt());
                setNextStepPrompt(agent.getNextStepPrompt());
                setMaxSteps(agent.getMaxSteps());
                setChatClient(agentChatClient);
                setMessageList(agent.getMessageList());
            }

            @Override
            protected void cleanup() {
                // 保存消息到文件
                if (finalChatId != null) {
                    List<Message> messages = getMessageList();
                    String nextStepPromptContent = getNextStepPrompt();
                    List<Message> filteredMessages = new ArrayList<>();

                    for (int i = 0; i < messages.size(); i++) {
                        Message msg = messages.get(i);

                        // 1. 过滤 nextStepPrompt UserMessage
                        if (msg instanceof UserMessage) {
                            String content = msg.getText();
                            if (content != null && nextStepPromptContent != null
                                && content.trim().equals(nextStepPromptContent.trim())) {
                                continue;
                            }
                            filteredMessages.add(msg);
                            continue;
                        }

                        // 2. 处理 AssistantMessage - 过滤只调用 doTerminate 的消息
                        if (msg instanceof AssistantMessage assistantMsg) {
                            List<AssistantMessage.ToolCall> toolCalls = assistantMsg.getToolCalls();
                            // 如果只调用了 doTerminate，跳过
                            if (toolCalls != null && toolCalls.size() == 1
                                && "doTerminate".equals(toolCalls.get(0).name())) {
                                continue;
                            }
                            // 截断工具参数（保留简要信息）
                            if (toolCalls != null && !toolCalls.isEmpty()) {
                                List<AssistantMessage.ToolCall> truncatedCalls = new ArrayList<>();
                                for (AssistantMessage.ToolCall call : toolCalls) {
                                    String args = call.arguments();
                                    // 截断过长的参数（如 generateMarkdown 的 content）
                                    if (args != null && args.length() > 200) {
                                        // 提取关键信息
                                        String truncated = args.length() > 200
                                            ? args.substring(0, 200) + "...[已截断]"
                                            : args;
                                        truncatedCalls.add(new AssistantMessage.ToolCall(
                                            call.id(), call.type(), call.name(), truncated));
                                    } else {
                                        truncatedCalls.add(call);
                                    }
                                }
                                // 创建新的 AssistantMessage（截断后）
                                AssistantMessage truncatedMsg = new AssistantMessage(
                                    assistantMsg.getText(), assistantMsg.getMetadata(), truncatedCalls);
                                filteredMessages.add(truncatedMsg);
                            } else {
                                filteredMessages.add(msg);
                            }
                            continue;
                        }

                        // 3. 处理 ToolResponseMessage - 过滤和截断
                        if (msg instanceof ToolResponseMessage toolMsg) {
                            List<ToolResponseMessage.ToolResponse> responses = toolMsg.getResponses();
                            // 过滤 doTerminate 响应
                            boolean isTerminateOnly = responses.stream()
                                .allMatch(r -> "doTerminate".equals(r.name()));
                            if (isTerminateOnly) {
                                continue;
                            }

                            // 截断响应数据
                            List<ToolResponseMessage.ToolResponse> truncatedResponses = new ArrayList<>();
                            for (ToolResponseMessage.ToolResponse resp : responses) {
                                if ("doTerminate".equals(resp.name())) {
                                    continue; // 跳过 terminate 响应
                                }
                                String data = resp.responseData();
                                // 过滤失败的响应（如百度安全验证）
                                if (data != null && (data.contains("百度安全验证")
                                    || data.contains("网络不给力"))) {
                                    data = "[请求失败]";
                                }
                                // 截断过长的响应
                                if (data != null && data.length() > 500) {
                                    data = data.substring(0, 500) + "...[已截断]";
                                }
                                truncatedResponses.add(new ToolResponseMessage.ToolResponse(
                                    resp.id(), resp.name(), data));
                            }

                            if (!truncatedResponses.isEmpty()) {
                                filteredMessages.add(new ToolResponseMessage(truncatedResponses, toolMsg.getMetadata()));
                            }
                            continue;
                        }

                        // 其他类型消息直接保留
                        filteredMessages.add(msg);
                    }

                    // 只保留最近的 20 条消息
                    int maxMessages = 20;
                    if (filteredMessages.size() > maxMessages) {
                        filteredMessages = new ArrayList<>(filteredMessages.subList(
                            filteredMessages.size() - maxMessages, filteredMessages.size()));
                    }

                    // 清除旧记录后添加新记录
                    memory.clear(finalChatId);
                    memory.add(finalChatId, filteredMessages);
                    log.info("已保存 {} 条消息到文件（已优化压缩），chatId: {}", filteredMessages.size(), finalChatId);
                }
                super.cleanup();
            }
        };
    }
}
