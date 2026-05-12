package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_card")
public class KnowledgeCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    private String title;

    private String question;

    private String answer;

    private String followUp;

    private String keyPoints;

    private String difficulty;

    private String tags;

    private String sourceName;

    private String sourceUrl;

    private Boolean enabled;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
