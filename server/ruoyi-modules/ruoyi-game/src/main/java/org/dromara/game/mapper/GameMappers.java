package org.dromara.game.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.game.domain.*;

public final class GameMappers {
    private GameMappers() {}

    public interface ServerMapper extends BaseMapperPlus<GameServer, GameServer> {}
    public interface ScopeMapper extends BaseMapperPlus<UserServerScope, UserServerScope> {}
    public interface RewardOrderMapper extends BaseMapperPlus<RewardOrder, RewardOrder> {}
    public interface RewardTargetMapper extends BaseMapperPlus<RewardTarget, RewardTarget> {}
    public interface RewardItemMapper extends BaseMapperPlus<RewardItem, RewardItem> {}
    public interface ActivityMapper extends BaseMapperPlus<GameActivity, GameActivity> {}
    public interface ActivityVersionMapper extends BaseMapperPlus<ActivityVersion, ActivityVersion> {}
    public interface ActivityPublishMapper extends BaseMapperPlus<ActivityPublish, ActivityPublish> {}
    public interface JobLogMapper extends BaseMapperPlus<GameJobLog, GameJobLog> {}
}
