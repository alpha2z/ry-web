package org.dromara.game.service;

import lombok.RequiredArgsConstructor;
import org.dromara.game.adapter.GameServerAdapter;
import org.dromara.game.adapter.HttpGameServerAdapter;
import org.dromara.game.adapter.MockGameServerAdapter;
import org.dromara.game.domain.GameServer;
import org.dromara.game.dto.AdapterModels.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameServerGateway {
    private final GameServerService servers;
    private final HttpGameServerAdapter httpAdapter;
    private final MockGameServerAdapter mockAdapter;

    private GameServerAdapter adapter(GameServer server) {
        return server.getBaseUrl().startsWith("mock://") ? mockAdapter : httpAdapter;
    }

    public PlayerView player(String serverCode, String playerId) {
        GameServer s = servers.requireAuthorized(serverCode);
        return adapter(s).findPlayer(s, servers.decryptedSecret(s), playerId);
    }
    public DeliveryResult reward(RewardGrant grant) {
        GameServer s = servers.requireAuthorized(grant.serverCode());
        return adapter(s).grantReward(s, servers.decryptedSecret(s), grant);
    }
    public DeliveryResult rewardSystem(RewardGrant grant) {
        GameServer s = servers.requireSystem(grant.serverCode());
        return adapter(s).grantReward(s, servers.decryptedSecret(s), grant);
    }
    public PublishResult publish(String serverCode, ActivityPayload payload) {
        GameServer s = servers.requireAuthorized(serverCode);
        return adapter(s).publishActivity(s, servers.decryptedSecret(s), payload);
    }
    public PublishResult publishSystem(String serverCode, ActivityPayload payload) {
        GameServer s = servers.requireSystem(serverCode);
        return adapter(s).publishActivity(s, servers.decryptedSecret(s), payload);
    }
    public PublishResult offline(String serverCode, String requestId, String activityCode) {
        GameServer s = servers.requireAuthorized(serverCode);
        return adapter(s).offlineActivity(s, servers.decryptedSecret(s), requestId, activityCode);
    }
    public PublishResult offlineSystem(String serverCode, String requestId, String activityCode) {
        GameServer s = servers.requireSystem(serverCode);
        return adapter(s).offlineActivity(s, servers.decryptedSecret(s), requestId, activityCode);
    }
    public HealthResult health(String serverCode) {
        GameServer s = servers.requireAuthorized(serverCode);
        return adapter(s).health(s, servers.decryptedSecret(s));
    }
}
