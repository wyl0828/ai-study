package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rag_chunk")
public class RagChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String sourceType;

    private Long sourceId;

    private Long userId;

    private Long problemId;

    private Integer chunkIndex;

    private String chunkText;

    private String knowledgePoint;

    private String errorType;

    private String tags;

    private String metadataJson;

    private String vectorPointId;

    private String embeddingModel;

    private Integer embeddingDim;

    private String vectorStatus;

    private LocalDateTime createdAt;
}
