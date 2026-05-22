package com.interview.coach.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RagChatResponseVO {

    private String answer;

    private List<RagChatSourceVO> sources = new ArrayList<>();
}
