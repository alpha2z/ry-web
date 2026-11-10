package org.dromara.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.game.domain.GameJobLog;
import org.dromara.game.mapper.GameMappers.JobLogMapper;
import org.dromara.game.quartz.GameSchedulerService;
import org.dromara.game.quartz.GameSchedulerService.JobView;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/jobs")
public class GameJobController {
    private final GameSchedulerService scheduler;
    private final JobLogMapper logs;

    @SaCheckPermission("game:job:list") @GetMapping("/list")
    public R<List<JobView>> list() { return R.ok(scheduler.list()); }
    @SaCheckPermission("game:job:list") @GetMapping("/logs")
    public TableDataInfo<GameJobLog> logs(PageQuery page) {
        return TableDataInfo.build(logs.selectPage(page.build(), Wrappers.<GameJobLog>lambdaQuery().orderByDesc(GameJobLog::getStartedAt)));
    }
    @SaCheckPermission("game:job:edit") @PostMapping("/{group}/{name}/pause")
    public R<Void> pause(@PathVariable String group, @PathVariable String name) { scheduler.pause(group, name); return R.ok(); }
    @SaCheckPermission("game:job:edit") @PostMapping("/{group}/{name}/resume")
    public R<Void> resume(@PathVariable String group, @PathVariable String name) { scheduler.resume(group, name); return R.ok(); }
    @SaCheckPermission("game:job:run") @PostMapping("/{group}/{name}/run")
    public R<Void> run(@PathVariable String group, @PathVariable String name) { scheduler.runNow(group, name); return R.ok(); }
    @SaCheckPermission("game:job:edit") @PutMapping("/{name}/cron")
    public R<Void> cron(@PathVariable String name, @RequestBody CronRequest request) { scheduler.updateCron(name, request.cron(), request.timezone()); return R.ok(); }
    public record CronRequest(String cron, String timezone) {}
}
