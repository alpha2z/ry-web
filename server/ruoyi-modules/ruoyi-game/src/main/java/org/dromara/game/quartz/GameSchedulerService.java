package org.dromara.game.quartz;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GameSchedulerService {
    private final Scheduler scheduler;

    public void scheduleOnce(String name, Class<? extends Job> jobClass, Instant fireAt, Map<String, ?> data) {
        try {
            JobKey key = JobKey.jobKey(name, "GAME_DYNAMIC");
            JobDetail detail = JobBuilder.newJob(jobClass).withIdentity(key).usingJobData(toJobData(data)).build();
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name + "-trigger", "GAME_DYNAMIC")
                .forJob(key).startAt(Date.from(fireAt.isBefore(Instant.now()) ? Instant.now() : fireAt))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow()).build();
            if (scheduler.checkExists(key)) scheduler.deleteJob(key);
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException e) {
            throw new ServiceException("定时任务创建失败: " + e.getMessage());
        }
    }

    public void ensureCron(String name, Class<? extends Job> jobClass, String cron, String timezone) {
        try {
            JobKey key = JobKey.jobKey(name, "GAME_MAINTENANCE");
            if (scheduler.checkExists(key)) return;
            JobDetail detail = JobBuilder.newJob(jobClass).withIdentity(key).storeDurably().build();
            CronScheduleBuilder schedule = CronScheduleBuilder.cronSchedule(cron)
                .inTimeZone(TimeZone.getTimeZone(timezone)).withMisfireHandlingInstructionDoNothing();
            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(name + "-trigger", "GAME_MAINTENANCE")
                .forJob(detail).withSchedule(schedule).build();
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException e) {
            throw new ServiceException("系统任务初始化失败: " + e.getMessage());
        }
    }

    public List<JobView> list() {
        try {
            List<JobView> result = new ArrayList<>();
            for (String group : scheduler.getJobGroupNames()) {
                if (!group.startsWith("GAME_")) continue;
                for (JobKey key : scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals(group))) {
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(key);
                    Trigger trigger = triggers.isEmpty() ? null : triggers.getFirst();
                    result.add(new JobView(key.getName(), key.getGroup(),
                        trigger == null ? null : trigger.getKey().getName(),
                        trigger == null ? null : scheduler.getTriggerState(trigger.getKey()).name(),
                        trigger == null || trigger.getNextFireTime() == null ? null : trigger.getNextFireTime().toInstant(),
                        trigger instanceof CronTrigger cron ? cron.getCronExpression() : null));
                }
            }
            return result;
        } catch (SchedulerException e) {
            throw new ServiceException("读取任务失败");
        }
    }

    public void pause(String group, String name) { withScheduler(() -> scheduler.pauseJob(JobKey.jobKey(name, group))); }
    public void resume(String group, String name) { withScheduler(() -> scheduler.resumeJob(JobKey.jobKey(name, group))); }
    public void runNow(String group, String name) { withScheduler(() -> scheduler.triggerJob(JobKey.jobKey(name, group))); }

    public void updateCron(String name, String cron, String timezone) {
        try {
            TriggerKey key = TriggerKey.triggerKey(name + "-trigger", "GAME_MAINTENANCE");
            Trigger old = scheduler.getTrigger(key);
            if (!(old instanceof CronTrigger)) throw new ServiceException("仅系统白名单 Cron 任务可修改");
            CronTrigger replacement = TriggerBuilder.newTrigger().withIdentity(key).forJob(old.getJobKey())
                .withSchedule(CronScheduleBuilder.cronSchedule(cron).inTimeZone(TimeZone.getTimeZone(timezone))
                    .withMisfireHandlingInstructionDoNothing()).build();
            scheduler.rescheduleJob(key, replacement);
        } catch (SchedulerException e) {
            throw new ServiceException("Cron 更新失败");
        }
    }

    private JobDataMap toJobData(Map<String, ?> values) {
        JobDataMap map = new JobDataMap();
        values.forEach((key, value) -> map.put(key, String.valueOf(value)));
        return map;
    }
    private void withScheduler(SchedulerAction action) {
        try { action.run(); } catch (SchedulerException e) { throw new ServiceException("任务操作失败"); }
    }
    @FunctionalInterface private interface SchedulerAction { void run() throws SchedulerException; }

    public record JobView(String name, String group, String trigger, String state, Instant nextFireAt, String cron) {}
}
