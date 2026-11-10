package org.dromara.game.dto;

import java.util.List;
import java.util.Map;

public final class AdapterModels {
    private AdapterModels() {}

    public record PlayerView(String serverCode, String playerId, String playerName, Integer level,
                             String status, Map<String, Object> assets) {}
    public record RewardGrant(String requestId, String serverCode, String playerId, String mailTitle,
                              String mailContent, List<RewardLine> items) {}
    public record RewardLine(String itemCode, Long quantity) {}
    public record DeliveryResult(boolean success, boolean retryable, String externalTxnId,
                                 String code, String message) {}
    public record ActivityPayload(String requestId, String activityCode, Integer version,
                                  Map<String, Object> snapshot) {}
    public record PublishResult(boolean success, String externalVersion, String message) {}
    public record HealthResult(boolean healthy, String message) {}
}
