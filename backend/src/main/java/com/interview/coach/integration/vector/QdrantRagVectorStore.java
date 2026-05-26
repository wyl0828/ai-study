package com.interview.coach.integration.vector;

import com.interview.coach.config.RagVectorProperties;
import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagVectorHit;
import com.interview.coach.entity.RagChunk;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.WithVectorsSelectorFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class QdrantRagVectorStore implements RagVectorStore {

    private final RagVectorProperties properties;

    private final boolean ownsClient;

    private volatile QdrantClient client;

    private volatile boolean collectionReady;

    @Autowired
    public QdrantRagVectorStore(RagVectorProperties properties) {
        this(properties, null, true);
    }

    QdrantRagVectorStore(RagVectorProperties properties, QdrantClient client) {
        this(properties, client, false);
    }

    private QdrantRagVectorStore(RagVectorProperties properties, QdrantClient client, boolean ownsClient) {
        this.properties = properties;
        this.client = client;
        this.ownsClient = ownsClient;
    }

    @Override
    public void upsertChunk(RagChunk chunk, float[] vector) {
        if (chunk == null || chunk.getId() == null || vector == null || vector.length == 0) {
            return;
        }
        ensureCollection();
        PointStruct point = PointStruct.newBuilder()
                .setId(PointIdFactory.id(chunk.getId()))
                .setVectors(VectorsFactory.vectors(vector))
                .putAllPayload(payload(chunk))
                .build();
        await(client().upsertAsync(properties.getCollectionName(), List.of(point)));
    }

    @Override
    public List<RagVectorHit> search(RagRetrieveQuery query, float[] vector, int limit) {
        if (vector == null || vector.length == 0 || limit <= 0) {
            return List.of();
        }
        ensureCollection();
        SearchPoints request = buildSearchRequest(query, vector, limit);
        return await(client().searchAsync(request)).stream()
                .map(this::toHit)
                .filter(hit -> hit.getChunkId() != null)
                .toList();
    }

    @Override
    public void deleteSystemChunks() {
        ensureCollection();
        await(client().deleteAsync(properties.getCollectionName(), buildUserIsolationFilter(null)));
    }

    @Override
    public void deleteDocumentChunks(Long documentId) {
        if (documentId == null) {
            return;
        }
        ensureCollection();
        Filter filter = Filter.newBuilder()
                .addMust(ConditionFactory.match("documentId", documentId))
                .build();
        await(client().deleteAsync(properties.getCollectionName(), filter));
    }

    SearchPoints buildSearchRequest(RagRetrieveQuery query, float[] vector, int limit) {
        return SearchPoints.newBuilder()
                .setCollectionName(properties.getCollectionName())
                .addAllVector(toFloatList(vector))
                .setFilter(buildUserIsolationFilter(query == null ? null : query.getUserId()))
                .setLimit(limit)
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .setWithVectors(WithVectorsSelectorFactory.enable(false))
                .build();
    }

    Filter buildUserIsolationFilter(Long userId) {
        if (userId == null) {
            return Filter.newBuilder()
                    .addMust(ConditionFactory.isNull("userId"))
                    .build();
        }
        Filter userScope = Filter.newBuilder()
                .addShould(ConditionFactory.isNull("userId"))
                .addShould(ConditionFactory.match("userId", userId))
                .build();
        return Filter.newBuilder()
                .addMust(ConditionFactory.filter(userScope))
                .build();
    }

    private Map<String, Value> payload(RagChunk chunk) {
        Map<String, Value> payload = new LinkedHashMap<>();
        put(payload, "chunkId", chunk.getId());
        put(payload, "documentId", chunk.getDocumentId());
        put(payload, "sourceType", chunk.getSourceType());
        put(payload, "sourceId", chunk.getSourceId());
        put(payload, "userId", chunk.getUserId());
        put(payload, "problemId", chunk.getProblemId());
        put(payload, "knowledgePoint", chunk.getKnowledgePoint());
        put(payload, "errorType", chunk.getErrorType());
        put(payload, "tags", chunk.getTags());
        return payload;
    }

    private void put(Map<String, Value> payload, String key, Long value) {
        payload.put(key, value == null ? ValueFactory.nullValue() : ValueFactory.value(value));
    }

    private void put(Map<String, Value> payload, String key, String value) {
        payload.put(key, StringUtils.hasText(value) ? ValueFactory.value(value) : ValueFactory.nullValue());
    }

    private RagVectorHit toHit(ScoredPoint point) {
        Map<String, Value> payload = point.getPayloadMap();
        RagVectorHit hit = new RagVectorHit();
        hit.setChunkId(valueAsLong(payload.get("chunkId"), point.getId()));
        hit.setDocumentId(valueAsLong(payload.get("documentId"), null));
        hit.setUserId(valueAsLong(payload.get("userId"), null));
        hit.setSimilarity(point.getScore());
        return hit;
    }

    private Long valueAsLong(Value value, PointId fallback) {
        if (value != null) {
            if (value.hasIntegerValue()) {
                return value.getIntegerValue();
            }
            if (value.hasDoubleValue()) {
                return Math.round(value.getDoubleValue());
            }
            if (value.hasStringValue()) {
                try {
                    return Long.parseLong(value.getStringValue());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        if (fallback != null && fallback.hasNum()) {
            return fallback.getNum();
        }
        return null;
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }

    private void ensureCollection() {
        if (collectionReady) {
            return;
        }
        synchronized (this) {
            if (collectionReady) {
                return;
            }
            boolean exists = await(client().collectionExistsAsync(properties.getCollectionName()));
            if (!exists) {
                VectorParams params = VectorParams.newBuilder()
                        .setSize(properties.getVectorSize())
                        .setDistance(Distance.Cosine)
                        .build();
                await(client().createCollectionAsync(properties.getCollectionName(), params));
            }
            collectionReady = true;
        }
    }

    private QdrantClient client() {
        QdrantClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (client == null) {
                client = new QdrantClient(QdrantGrpcClient
                        .newBuilder(properties.getHost(), properties.getPort(), properties.isUseTls())
                        .build());
            }
            return client;
        }
    }

    private <T> T await(com.google.common.util.concurrent.ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qdrant request interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Qdrant request failed", ex.getCause());
        }
    }

    @PreDestroy
    public void close() {
        if (!ownsClient || client == null) {
            return;
        }
        try {
            client.close();
        } catch (RuntimeException ex) {
            log.warn("Failed to close Qdrant client: {}", ex.getMessage());
        }
    }
}
