package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mock_interview_session")
public class MockInterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String category;

    private String status;

    private String interviewerStyle;

    private Integer questionCount;

    private Integer answeredMainCount;

    private Long currentKnowledgeCardId;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
