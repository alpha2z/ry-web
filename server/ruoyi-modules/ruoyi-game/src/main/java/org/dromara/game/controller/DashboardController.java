package org.dromara.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.game.domain.*;
import org.dromara.game.mapper.GameMappers.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/dashboard")
public class DashboardController {
    private final ServerMapper servers; private final RewardOrderMapper rewards;
    private final ActivityMapper activities; private final JobLogMapper jobs;

    @SaCheckPermission("game:dashboard:view")
    @GetMapping
    public R<DashboardStats> stats() {
        long totalRewards = rewards.selectCount(null);
        long successfulRewards = rewards.selectCount(Wrappers.<RewardOrder>lambdaQuery().eq(RewardOrder::getStatus, RewardStatus.SUCCESS.name()));
        return R.ok(new DashboardStats(
            servers.selectCount(Wrappers.<GameServer>lambdaQuery().eq(GameServer::getEnabled, true)),
            activities.selectCount(Wrappers.<GameActivity>lambdaQuery().eq(GameActivity::getStatus, ActivityStatus.PUBLISHED.name())),
            rewards.selectCount(Wrappers.<RewardOrder>lambdaQuery().eq(RewardOrder::getStatus, RewardStatus.PENDING_APPROVAL.name())),
            totalRewards == 0 ? 100D : successfulRewards * 100D / totalRewards,
            jobs.selectCount(Wrappers.<GameJobLog>lambdaQuery().eq(GameJobLog::getStatus, "FAILED"))));
    }
    public record DashboardStats(long enabledServers, long publishedActivities, long pendingApprovals,
                                 double rewardSuccessRate, long failedJobs) {}
}
