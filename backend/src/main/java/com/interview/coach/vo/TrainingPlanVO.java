package com.interview.coach.vo;

import java.util.List;
import lombok.Data;

@Data
public class TrainingPlanVO {

    private String title;

    private String summary;

    private List<TrainingPlanItemVO> items;
}
