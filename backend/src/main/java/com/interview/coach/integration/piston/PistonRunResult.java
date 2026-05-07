package com.interview.coach.integration.piston;

import lombok.Data;

@Data
public class PistonRunResult {

    private String stdout;

    private String stderr;

    private String output;

    private Integer code;

    private String signal;
}
