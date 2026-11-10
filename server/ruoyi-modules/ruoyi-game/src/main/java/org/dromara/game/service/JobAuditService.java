package org.dromara.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.game.domain.GameJobLog;
import org.dromara.game.mapper.GameMappers.JobLogMapper;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JobAuditService {
    private final JobLogMapper mapper;

    public GameJobLog start(JobExecutionContext context) {
        GameJobLog log = new GameJobLog();
        log.setJobName(context.getJobDetail().getKey().getName());
        log.setJobGroup(context.getJobDetail().getKey().getGroup());
        log.setFireInstanceId(context.getFireInstanceId());
        log.setStartedAt(Instant.now());
        log.setStatus("RUNNING");
        mapper.insert(log);
        return log;
    }
    public void finish(GameJobLog log, Throwable error) {
        log.setFinishedAt(Instant.now());
        log.setDurationMs(log.getFinishedAt().toEpochMilli() - log.getStartedAt().toEpochMilli());
        log.setStatus(error == null ? "SUCCESS" : "FAILED");
        log.setErrorMessage(error == null ? null : abbreviate(error.getMessage()));
        mapper.updateById(log);
    }
    public void deleteOlderThan(Instant threshold) {
        mapper.delete(Wrappers.<GameJobLog>lambdaQuery().lt(GameJobLog::getFinishedAt, threshold));
    }
    private String abbreviate(String value) {
        if (value == null) return "Unknown error";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
