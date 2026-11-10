package org.dromara.game.adapter;

import org.dromara.game.domain.GameServer;
import org.dromara.game.dto.AdapterModels.*;

public interface GameServerAdapter {
    PlayerView findPlayer(GameServer server, String secret, String playerId);
    DeliveryResult grantReward(GameServer server, String secret, RewardGrant grant);
    PublishResult publishActivity(GameServer server, String secret, ActivityPayload payload);
    PublishResult offlineActivity(GameServer server, String secret, String requestId, String activityCode);
    HealthResult health(GameServer server, String secret);
}
