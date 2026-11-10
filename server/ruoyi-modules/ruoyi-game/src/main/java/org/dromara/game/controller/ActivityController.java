package org.dromara.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.game.domain.GameActivity;
import org.dromara.game.dto.GameRequests.ActivityRequest;
import org.dromara.game.service.ActivityService;
import org.dromara.game.service.ActivityService.ActivityDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/activities")
public class ActivityController {
    private final ActivityService service;

    @SaCheckPermission("game:activity:list")
    @GetMapping("/list")
    public TableDataInfo<GameActivity> list(PageQuery page, String status, String type) { return service.list(page, status, type); }
    @SaCheckPermission("game:activity:query")
    @GetMapping("/{id}") public R<ActivityDetail> detail(@PathVariable Long id) { return R.ok(service.detail(id)); }
    @SaCheckPermission("game:activity:add")
    @Log(title = "活动配置", businessType = BusinessType.INSERT)
    @PostMapping public R<GameActivity> create(@Valid @RequestBody ActivityRequest request) { return R.ok(service.create(request)); }
    @SaCheckPermission("game:activity:edit")
    @Log(title = "活动配置", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}") public R<GameActivity> update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) { return R.ok(service.update(id, request)); }
    @SaCheckPermission("game:activity:publish")
    @Log(title = "活动发布", businessType = BusinessType.OTHER)
    @PostMapping("/{id}/publish") public R<Void> publish(@PathVariable Long id) { service.publish(id); return R.ok(); }
    @SaCheckPermission("game:activity:offline")
    @Log(title = "活动下线", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/offline") public R<Void> offline(@PathVariable Long id) { service.offline(id); return R.ok(); }
    @SaCheckPermission("game:activity:publish")
    @PostMapping("/{id}/servers/{serverCode}/retry") public R<Void> retry(@PathVariable Long id, @PathVariable String serverCode) { service.retryLatest(id, serverCode); return R.ok(); }
    @SaCheckPermission("game:activity:publish")
    @PostMapping("/{id}/versions/{versionId}/servers/{serverCode}/publish")
    public R<Void> rollback(@PathVariable Long id, @PathVariable Long versionId, @PathVariable String serverCode) {
        service.republish(id, versionId, serverCode); return R.ok();
    }
}
