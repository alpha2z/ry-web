package org.dromara.game.config;

import lombok.RequiredArgsConstructor;
import org.dromara.game.quartz.GameSchedulerService;
import org.dromara.game.quartz.JobLogRetentionJob;
import org.dromara.game.quartz.RetryFailedRewardsJob;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GameModuleConfiguration implements ApplicationRunner {
    private final GameSchedulerService scheduler;

    @Override
    public void run(ApplicationArguments args) {
        scheduler.ensureCron("retry-failed-rewards", RetryFailedRewardsJob.class, "0 */5 * * * ?", "UTC");
        scheduler.ensureCron("purge-job-logs", JobLogRetentionJob.class, "0 15 3 * * ?", "UTC");
    }
}
