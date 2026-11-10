package org.dromara.game.adapter;

import org.dromara.game.domain.GameServer;
import org.dromara.game.dto.AdapterModels.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class MockGameServerAdapter implements GameServerAdapter {
    @Override
    public PlayerView findPlayer(GameServer server, String secret, String playerId) {
        return new PlayerView(server.getServerCode(), playerId, "MockPlayer-" + playerId, 42, "ONLINE",
            Map.of("gold", 1000, "diamond", 88));
    }
    @Override
    public DeliveryResult grantReward(GameServer server, String secret, RewardGrant grant) {
        return new DeliveryResult(true, false, "mock-" + UUID.randomUUID(), null, "accepted");
    }
    @Override
    public PublishResult publishActivity(GameServer server, String secret, ActivityPayload payload) {
        return new PublishResult(true, "mock-v" + payload.version(), null);
    }
    @Override
    public PublishResult offlineActivity(GameServer server, String secret, String requestId, String activityCode) {
        return new PublishResult(true, null, null);
    }
    @Override
    public HealthResult health(GameServer server, String secret) {
        return new HealthResult(true, "MOCK_UP");
    }
}
