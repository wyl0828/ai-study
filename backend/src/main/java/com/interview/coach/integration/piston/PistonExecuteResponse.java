package com.interview.coach.integration.piston;

import lombok.Data;

@Data
public class PistonExecuteResponse {

    private String language;

    private String version;

    private PistonRunResult compile;

    private PistonRunResult run;
}
