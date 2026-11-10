package org.dromara.game.quartz;

import org.dromara.game.domain.GameJobLog;
import org.dromara.game.service.ActivityService;
import org.dromara.game.service.JobAuditService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

@DisallowConcurrentExecution
public class ActivityPublishJob implements Job {
    @Autowired
    private ActivityService activities;
    @Autowired
    private JobAuditService audit;
    @Override public void execute(JobExecutionContext context) throws JobExecutionException {
        GameJobLog log = audit.start(context); Throwable error = null;
        try { activities.publish(context.getMergedJobDataMap().getLongFromString("activityId")); }
        catch (Throwable e) { error = e; throw new JobExecutionException(e); }
        finally { audit.finish(log, error); }
    }
}
