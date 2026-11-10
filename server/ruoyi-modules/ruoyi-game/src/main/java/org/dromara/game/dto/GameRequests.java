package org.dromara.game.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class GameRequests {
    private GameRequests() {}

    public record ServerRequest(
        @NotBlank String serverCode,
        @NotBlank String serverName,
        @NotBlank String baseUrl,
        @NotBlank String appId,
        String secret,
        @Min(500) @Max(30000) Integer timeoutMs,
        Boolean enabled,
        String remark
    ) {}

    public record RewardTargetRequest(@NotBlank String serverCode, @NotBlank String playerId) {}
    public record RewardItemRequest(@NotBlank String itemCode, @NotNull @Positive Long quantity) {}

    public record RewardCreateRequest(
        @NotBlank String title,
        @NotBlank String mailTitle,
        @NotBlank String mailContent,
        @NotEmpty @Size(max = 10000) List<@Valid RewardTargetRequest> targets,
        @NotEmpty @Size(max = 50) List<@Valid RewardItemRequest> items,
        Instant scheduledAt,
        String timezone,
        String remark
    ) {}

    public record ActivityRequest(
        @NotBlank String activityCode,
        @NotBlank String activityName,
        @NotBlank @Pattern(regexp = "SIGN_IN|CUMULATIVE_LOGIN|SINGLE_RECHARGE|CUMULATIVE_RECHARGE|RANKING|CUSTOM") String activityType,
        @NotBlank String timezone,
        Instant displayStartAt,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        Instant publishAt,
        @NotEmpty List<@NotBlank String> serverCodes,
        @NotNull Map<String, Object> rule,
        @NotNull List<Map<String, Object>> rewards,
        String description
    ) {}
}
