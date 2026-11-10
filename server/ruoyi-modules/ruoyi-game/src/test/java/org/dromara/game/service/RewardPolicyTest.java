package org.dromara.game.service;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class RewardPolicyTest {
    @Test void singleImmediateRewardDoesNotRequireApproval() {
        assertFalse(RewardPolicy.requiresApproval(1, null));
    }
    @Test void batchAndScheduledRewardsRequireApproval() {
        assertTrue(RewardPolicy.requiresApproval(2, null));
        assertTrue(RewardPolicy.requiresApproval(1, Instant.now().plusSeconds(60)));
    }
    @Test void creatorCannotApproveOwnOrder() {
        assertThrows(ServiceException.class, () -> RewardPolicy.requireDifferentApprover(7L, 7L));
        assertDoesNotThrow(() -> RewardPolicy.requireDifferentApprover(7L, 8L));
    }
}
