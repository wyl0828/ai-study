package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_knowledge_card_mastery")
public class UserKnowledgeCardMastery {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long knowledgeCardId;

    private BigDecimal masteryScore;

    private Integer selfTestCount;

    private Integer lastScore;

    private LocalDateTime lastPracticedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
