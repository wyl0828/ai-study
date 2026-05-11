package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("problem")
public class Problem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private String category;

    private String inputFormat;

    private String outputFormat;

    private String codeMode;

    private String templateCode;

    private String solutionOutline;

    private String hintLevel1;

    private String hintLevel2;

    private String hintLevel3;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
