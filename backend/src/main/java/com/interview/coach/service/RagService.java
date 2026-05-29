package com.interview.coach.service;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.entity.AiDiagnosis;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.Problem;
import com.interview.coach.vo.RagHealthVO;
import com.interview.coach.vo.RagSystemRebuildVO;
import com.interview.coach.vo.RagVectorRetryVO;

public interface RagService {

    RagRetrieveResult retrieveForDiagnosis(AgentContext context, int limit);

    RagRetrieveResult retrieveForChat(Long userId, String question, int limit);

    RagRetrieveResult retrieve(RagRetrieveQuery query);

    RagHealthVO checkHealth();

    RagVectorRetryVO retryFailedVectors(int limit);

    RagSystemRebuildVO rebuildSystemIndexForMaintenance();

    void indexProblem(Problem problem);

    void indexKnowledgeCard(KnowledgeCard card);

    void indexLearningMemory(AgentContext context, AiDiagnosis diagnosis, MistakeCard mistakeCard);

    void rebuildSystemIndex();
}
