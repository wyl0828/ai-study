package com.interview.coach.vo;

import java.util.List;
import lombok.Data;

@Data
public class TrainingPlanVO {

    private Long id;

    private String title;

    private String summary;

    private String status;

    private String statusLabel;

    private List<TrainingPlanItemVO> items;
}
