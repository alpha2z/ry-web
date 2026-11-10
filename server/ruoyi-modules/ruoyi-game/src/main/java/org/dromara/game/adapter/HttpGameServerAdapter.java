package org.dromara.game.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.game.domain.GameServer;
import org.dromara.game.dto.AdapterModels.*;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HttpGameServerAdapter implements GameServerAdapter {
    private final ObjectMapper objectMapper;

    @Override
    public PlayerView findPlayer(GameServer server, String secret, String playerId) {
        String path = "/ops-api/v1/players/" + URLEncoder.encode(playerId, StandardCharsets.UTF_8);
        JsonNode node = invoke(server, secret, "GET", path, null);
        return new PlayerView(server.getServerCode(), playerId, node.path("playerName").asText(),
            node.path("level").asInt(), node.path("status").asText(),
            objectMapper.convertValue(node.path("assets"), Map.class));
    }

    @Override
    public DeliveryResult grantReward(GameServer server, String secret, RewardGrant grant) {
        try {
            JsonNode node = invoke(server, secret, "POST", "/ops-api/v1/reward-grants", grant);
            boolean success = "SUCCESS".equalsIgnoreCase(node.path("status").asText());
            return new DeliveryResult(success, false, node.path("externalTxnId").asText(null),
                node.path("code").asText(null), node.path("message").asText(null));
        } catch (RemoteCallException e) {
            return new DeliveryResult(false, e.retryable, null, "REMOTE_ERROR", e.getMessage());
        }
    }

    @Override
    public PublishResult publishActivity(GameServer server, String secret, ActivityPayload payload) {
        String path = "/ops-api/v1/activities/" + payload.activityCode() + "/versions/" + payload.version();
        try {
            JsonNode node = invoke(server, secret, "PUT", path, payload);
            return new PublishResult(true, node.path("externalVersion").asText(String.valueOf(payload.version())), null);
        } catch (RemoteCallException e) {
            return new PublishResult(false, null, e.getMessage());
        }
    }

    @Override
    public PublishResult offlineActivity(GameServer server, String secret, String requestId, String activityCode) {
        try {
            invoke(server, secret, "POST", "/ops-api/v1/activities/" + activityCode + "/offline",
                Map.of("requestId", requestId));
            return new PublishResult(true, null, null);
        } catch (RemoteCallException e) {
            return new PublishResult(false, null, e.getMessage());
        }
    }

    @Override
    public HealthResult health(GameServer server, String secret) {
        try {
            invoke(server, secret, "GET", "/ops-api/v1/health", null);
            return new HealthResult(true, "UP");
        } catch (Exception e) {
            return new HealthResult(false, e.getMessage());
        }
    }

    private JsonNode invoke(GameServer server, String secret, String method, String path, Object payload) {
        try {
            String body = payload == null ? "" : objectMapper.writeValueAsString(payload);
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String nonce = UUID.randomUUID().toString();
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(server.getBaseUrl() + path))
                .timeout(Duration.ofMillis(server.getTimeoutMs()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-App-Id", server.getAppId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", HmacSigner.sign(secret, timestamp, nonce, method, path, body));
            builder.method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(server.getTimeoutMs())).build()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RemoteCallException("游戏服返回 HTTP " + response.statusCode(), response.statusCode() >= 500);
            }
            return response.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
        } catch (RemoteCallException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException | java.net.ConnectException e) {
            throw new RemoteCallException("游戏服连接超时或不可用", true);
        } catch (Exception e) {
            throw new RemoteCallException("游戏服调用失败: " + e.getMessage(), false);
        }
    }

    private static final class RemoteCallException extends RuntimeException {
        private final boolean retryable;
        private RemoteCallException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }
    }
}
