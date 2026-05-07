package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("test_case")
public class TestCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long problemId;

    private String inputData;

    private String expectedOutput;

    @TableField("is_sample")
    private Boolean sample;

    private Integer weight;
}
