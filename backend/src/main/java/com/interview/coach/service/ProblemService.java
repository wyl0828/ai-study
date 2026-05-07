package com.interview.coach.service;

import com.interview.coach.entity.Problem;
import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
import java.util.List;

public interface ProblemService {

    List<ProblemListItemVO> listProblems();

    ProblemDetailVO getProblemDetail(Long id);

    ProblemTemplateVO getProblemTemplate(Long id);

    Problem getEnabledProblem(Long id);

    List<String> listKnowledgePointNames(Long problemId);
}
