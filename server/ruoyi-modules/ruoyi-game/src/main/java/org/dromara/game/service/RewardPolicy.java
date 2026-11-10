package org.dromara.game.service;

import org.dromara.common.core.exception.ServiceException;

import java.time.Instant;

public final class RewardPolicy {
    private RewardPolicy() {}

    public static boolean requiresApproval(int targetCount, Instant scheduledAt) {
        return targetCount > 1 || scheduledAt != null;
    }

    public static void requireDifferentApprover(Long creatorId, Long approverId) {
        if (creatorId != null && creatorId.equals(approverId)) {
            throw new ServiceException("创建人不能审批自己的批量或定时奖励");
        }
    }
}
