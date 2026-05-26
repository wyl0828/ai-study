package com.interview.coach.dto;

import lombok.Data;

@Data
public class RagVectorHit {

    private Long chunkId;

    private Long documentId;

    private Long userId;

    private Float similarity;
}
