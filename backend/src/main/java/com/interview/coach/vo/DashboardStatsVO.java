package com.interview.coach.vo;

import lombok.Data;

@Data
public class DashboardStatsVO {

    private Integer totalSubmissions;

    private Integer passedProblems;

    private Integer weakPointCount;

    private Integer mistakeCount;
}
