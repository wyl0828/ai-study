package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mock_interview_turn")
public class MockInterviewTurn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long knowledgeCardId;

    private Integer turnOrder;

    private String turnType;

    private Long parentTurnId;

    private String question;

    private String userAnswer;

    private Integer score;

    private String feedback;

    private String performanceLevel;

    private String strengthSummary;

    private String gapSummary;

    private String expressionFeedback;

    private String interviewerObservation;

    private String followUpReason;

    private String hitKeyPoints;

    private String missingKeyPoints;

    private String expressionIssue;

    private String aiRawJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
