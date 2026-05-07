package com.interview.coach.integration.piston;

public class PistonExecutionException extends RuntimeException {

    public PistonExecutionException(String message) {
        super(message);
    }

    public PistonExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
