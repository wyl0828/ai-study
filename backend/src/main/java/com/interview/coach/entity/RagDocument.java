package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rag_document")
public class RagDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceType;

    private Long sourceId;

    private Long userId;

    private Long problemId;

    private String title;

    private String knowledgePoint;

    private String errorType;

    private String tags;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
