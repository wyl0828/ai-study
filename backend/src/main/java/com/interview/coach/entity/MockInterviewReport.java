package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mock_interview_report")
public class MockInterviewReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long userId;

    private BigDecimal averageScore;

    private String summary;

    private String strengths;

    private String weaknesses;

    private String expressionAdvice;

    private String recommendedCardIds;

    private String weaknessTags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
