package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("problem_knowledge_point")
public class ProblemKnowledgePoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long problemId;

    private Long knowledgePointId;
}
