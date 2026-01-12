package com.ai.agent.agent.model;

/**
 * Agent 执行状态枚举
 */
public enum AgentState {
    /**
     * 空闲状态，等待执行
     */
    IDLE,

    /**
     * 运行中状态
     */
    RUNNING,

    /**
     * 已完成状态
     */
    FINISHED,

    /**
     * 错误状态
     */
    ERROR
}
