package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("knowledge_point")
public class KnowledgePoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String category;

    private String description;
}
