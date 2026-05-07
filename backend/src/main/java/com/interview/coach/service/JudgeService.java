package com.interview.coach.service;

import com.interview.coach.dto.JudgeCase;
import com.interview.coach.dto.JudgeResult;
import java.util.List;

public interface JudgeService {

    JudgeResult judgeJava(String code, List<JudgeCase> cases);
}
