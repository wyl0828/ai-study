package com.interview.coach.enums;

public enum AgentState {
    PLANNING,
    CODE_EXECUTION,
    OBSERVATION,
    RAG_RETRIEVAL,
    ERROR_CLASSIFICATION,
    CODE_REVIEW,
    /**
     * Legacy state kept for schema/API compatibility. Current Agent workflow does not run AI hint generation.
     */
    HINT_GENERATION,
    MEMORY_UPDATE,
    TRAINING_PLAN,
    COMPLETED,
    FAILED
}
