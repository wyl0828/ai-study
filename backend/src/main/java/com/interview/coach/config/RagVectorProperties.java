package com.interview.coach.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "coach.rag.vector")
public class RagVectorProperties {

    private boolean enabled = false;

    private String collectionName = "ai_study_rag_chunks";

    private String host = "localhost";

    private int port = 6334;

    private boolean useTls = false;

    private int vectorSize = 1536;

    private BigDecimal hybridVectorWeight = new BigDecimal("0.60");

    private BigDecimal hybridRuleWeight = new BigDecimal("0.40");

    private int vectorCandidateLimit = 30;
}
