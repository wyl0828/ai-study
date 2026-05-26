package com.interview.coach.integration.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.interview.coach.config.RagVectorProperties;
import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.entity.RagChunk;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.UpdateResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QdrantRagVectorStoreTest {

    @Test
    void upsertChunkStoresChunkIdAndMetadataPayload() {
        QdrantClient client = mock(QdrantClient.class);
        when(client.collectionExistsAsync("test_rag")).thenReturn(Futures.immediateFuture(true));
        when(client.upsertAsync(eq("test_rag"), any())).thenReturn(Futures.immediateFuture(UpdateResult.getDefaultInstance()));
        QdrantRagVectorStore store = new QdrantRagVectorStore(properties(), client);

        store.upsertChunk(chunk(), new float[] {0.1f, 0.2f, 0.3f});

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointStruct>> captor = ArgumentCaptor.forClass(List.class);
        verify(client).upsertAsync(eq("test_rag"), captor.capture());
        PointStruct point = captor.getValue().get(0);
        assertThat(point.getId().getNum()).isEqualTo(11L);
        assertThat(point.getPayloadOrThrow("chunkId").getIntegerValue()).isEqualTo(11L);
        assertThat(point.getPayloadOrThrow("documentId").getIntegerValue()).isEqualTo(22L);
        assertThat(point.getPayloadOrThrow("sourceType").getStringValue()).isEqualTo("MISTAKE_CARD");
        assertThat(point.getPayloadOrThrow("userId").getIntegerValue()).isEqualTo(7L);
        assertThat(point.getVectors().getVector().getDense().getDataList()).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void searchFilterContainsSystemOrCurrentUserScope() {
        QdrantClient client = mock(QdrantClient.class);
        when(client.collectionExistsAsync("test_rag")).thenReturn(Futures.immediateFuture(true));
        when(client.searchAsync(any(SearchPoints.class))).thenReturn(Futures.immediateFuture(List.of()));
        QdrantRagVectorStore store = new QdrantRagVectorStore(properties(), client);
        RagRetrieveQuery query = new RagRetrieveQuery();
        query.setUserId(7L);

        store.search(query, new float[] {0.1f, 0.2f, 0.3f}, 5);

        ArgumentCaptor<SearchPoints> captor = ArgumentCaptor.forClass(SearchPoints.class);
        verify(client).searchAsync(captor.capture());
        SearchPoints request = captor.getValue();
        assertThat(request.getCollectionName()).isEqualTo("test_rag");
        assertThat(request.getLimit()).isEqualTo(5);
        assertThat(request.getFilter().toString())
                .contains("key: \"userId\"")
                .contains("integer: 7")
                .contains("is_null");
    }

    private RagVectorProperties properties() {
        RagVectorProperties properties = new RagVectorProperties();
        properties.setCollectionName("test_rag");
        properties.setVectorSize(3);
        return properties;
    }

    private RagChunk chunk() {
        RagChunk chunk = new RagChunk();
        chunk.setId(11L);
        chunk.setDocumentId(22L);
        chunk.setSourceType("MISTAKE_CARD");
        chunk.setSourceId(33L);
        chunk.setUserId(7L);
        chunk.setProblemId(1L);
        chunk.setKnowledgePoint("HashMap");
        chunk.setErrorType("LOGIC_ERROR");
        chunk.setTags("HashMap,Two Sum");
        return chunk;
    }
}
