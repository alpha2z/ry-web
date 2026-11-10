package org.dromara.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.game.domain.RewardOrder;
import org.dromara.game.dto.GameRequests.RewardCreateRequest;
import org.dromara.game.service.RewardService;
import org.dromara.game.service.RewardService.RewardDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/rewards")
public class RewardController {
    private final RewardService service;

    @SaCheckPermission("game:reward:list")
    @GetMapping("/list")
    public TableDataInfo<RewardOrder> list(PageQuery page, String status) { return service.list(page, status); }

    @SaCheckPermission("game:reward:query")
    @GetMapping("/{id}")
    public R<RewardDetail> detail(@PathVariable Long id) { return R.ok(service.detail(id)); }

    @SaCheckPermission("game:reward:add")
    @Log(title = "奖励发放", businessType = BusinessType.INSERT)
    @PostMapping
    public R<RewardOrder> create(@Valid @RequestBody RewardCreateRequest request) { return R.ok(service.create(request)); }

    @SaCheckPermission("game:reward:approve")
    @Log(title = "奖励审批", businessType = BusinessType.GRANT)
    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) { service.approve(id); return R.ok(); }

    @SaCheckPermission("game:reward:execute")
    @Log(title = "奖励执行", businessType = BusinessType.OTHER)
    @PostMapping("/{id}/execute")
    public R<Void> execute(@PathVariable Long id) { service.execute(id); return R.ok(); }

    @SaCheckPermission("game:reward:retry")
    @Log(title = "奖励重试", businessType = BusinessType.OTHER)
    @PostMapping("/{id}/retry")
    public R<Void> retry(@PathVariable Long id) { service.execute(id); return R.ok(); }

    @SaCheckPermission("game:reward:cancel")
    @Log(title = "取消奖励", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) { service.cancel(id); return R.ok(); }
}
