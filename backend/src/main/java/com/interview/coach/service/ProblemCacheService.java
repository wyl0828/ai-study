package com.interview.coach.service;

import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
import java.util.List;
import java.util.Optional;

public interface ProblemCacheService {

    Optional<List<ProblemListItemVO>> getProblemList();

    void putProblemList(List<ProblemListItemVO> problems);

    Optional<ProblemDetailVO> getProblemDetail(Long problemId);

    void putProblemDetail(Long problemId, ProblemDetailVO detail);

    Optional<ProblemTemplateVO> getProblemTemplate(Long problemId);

    void putProblemTemplate(Long problemId, ProblemTemplateVO template);

    void evictProblem(Long problemId);

    void evictAll();
}
