package com.interview.coach.integration.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.interview.coach.config.EmbeddingProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleEmbeddingClientTest {

    @Test
    void parsesOpenAiCompatibleEmbeddingResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startEmbeddingServer(requestBody);
        try {
            EmbeddingProperties properties = new EmbeddingProperties();
            properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
            properties.setApiKey("test-key");
            properties.setModel("test-embedding");
            properties.setDimensions(3);

            float[] vector = new OpenAiCompatibleEmbeddingClient(properties).embed("abc");

            assertThat(vector).containsExactly(0.1f, 0.2f, 0.3f);
            assertThat(requestBody.get())
                    .contains("\"model\":\"test-embedding\"")
                    .contains("\"input\":\"abc\"")
                    .contains("\"dimensions\":3");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer startEmbeddingServer(AtomicReference<String> requestBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }
}
