package com.interview.coach.integration.vector;

import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagVectorHit;
import com.interview.coach.entity.RagChunk;
import java.util.List;

public interface RagVectorStore {

    void upsertChunk(RagChunk chunk, float[] vector);

    List<RagVectorHit> search(RagRetrieveQuery query, float[] vector, int limit);

    void deleteSystemChunks();

    void deleteDocumentChunks(Long documentId);
}
