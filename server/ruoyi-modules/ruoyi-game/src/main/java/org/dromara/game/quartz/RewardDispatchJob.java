package org.dromara.game.quartz;

import org.dromara.game.domain.GameJobLog;
import org.dromara.game.service.JobAuditService;
import org.dromara.game.service.RewardService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

@DisallowConcurrentExecution
public class RewardDispatchJob implements Job {
    @Autowired
    private RewardService rewards;
    @Autowired
    private JobAuditService audit;
    @Override public void execute(JobExecutionContext context) throws JobExecutionException {
        GameJobLog log = audit.start(context); Throwable error = null;
        try { rewards.execute(context.getMergedJobDataMap().getLongFromString("orderId")); }
        catch (Throwable e) { error = e; throw new JobExecutionException(e); }
        finally { audit.finish(log, error); }
    }
}
