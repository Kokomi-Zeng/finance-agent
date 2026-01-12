package com.ai.agent.controller;

import com.ai.agent.agent.ToolCallAgent;
import com.ai.agent.app.FinancialManagementApp;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private FinancialManagementApp financialManagementApp;

    /**
     * 同步调用 AI 理财管理应用
     */
    @GetMapping("/financial/chat/sync")
    public String doChatSync(String message, String chatId) {
        return financialManagementApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI 理财管理应用
     */
    @GetMapping(value = "/financial/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatSSE(String message, String chatId) {
        return financialManagementApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 理财管理应用（ServerSentEvent 格式）
     */
    @GetMapping(value = "/financial/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatServerSentEvent(String message, String chatId) {
        return financialManagementApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 理财管理应用（SseEmitter 格式）
     */
    @GetMapping(value = "/financial/chat/sse_emitter")
    public SseEmitter doChatSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L);
        financialManagementApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    /**
     * AI 理财报告功能（结构化输出）
     */
    @GetMapping("/financial/report")
    public FinancialManagementApp.FinancialReport getReport(String message, String chatId) {
        return financialManagementApp.doChatWithReport(message, chatId);
    }

    /**
     * 流式调用超级智能体（ReAct 模式，整合记忆、RAG、工具调用、MCP）
     */
    @GetMapping("/financial/agent")
    public SseEmitter doChatWithAgent(String message, String chatId) {
        ToolCallAgent agent = financialManagementApp.createAgentInstance(chatId);
        return agent.runStream(message);
    }
}
