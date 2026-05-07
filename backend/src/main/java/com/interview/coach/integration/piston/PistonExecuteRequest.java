package com.interview.coach.integration.piston;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class PistonExecuteRequest {

    private String language;

    private String version;

    private List<PistonFile> files;

    private String stdin;

    @JsonProperty("compile_timeout")
    private Long compileTimeout;

    @JsonProperty("run_timeout")
    private Long runTimeout;
}
