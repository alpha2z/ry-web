package org.dromara.game.quartz;

import org.dromara.game.domain.GameJobLog;
import org.dromara.game.service.JobAuditService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@DisallowConcurrentExecution
public class JobLogRetentionJob implements Job {
    @Autowired
    private JobAuditService audit;
    @Override public void execute(JobExecutionContext context) throws JobExecutionException {
        GameJobLog log = audit.start(context); Throwable error = null;
        try { audit.deleteOlderThan(Instant.now().minus(90, ChronoUnit.DAYS)); }
        catch (Throwable e) { error = e; throw new JobExecutionException(e); }
        finally { audit.finish(log, error); }
    }
}
