package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("self_test_record")
public class SelfTestRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long knowledgeCardId;

    private String questionSnapshot;

    private String userAnswer;

    private Integer score;

    private String feedback;

    private String missingKeyPoints;

    private LocalDateTime createdAt;
}
