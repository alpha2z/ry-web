package org.dromara.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.game.domain.GameServer;
import org.dromara.game.dto.AdapterModels.HealthResult;
import org.dromara.game.dto.GameRequests.ServerRequest;
import org.dromara.game.service.GameServerGateway;
import org.dromara.game.service.GameServerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/servers")
public class GameServerController {
    private final GameServerService service;
    private final GameServerGateway gateway;

    @SaCheckPermission("game:server:list")
    @GetMapping("/list")
    public R<List<GameServer>> list() { return R.ok(service.listAuthorized()); }

    @SaCheckPermission("game:server:add")
    @Log(title = "区服管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<GameServer> create(@Valid @RequestBody ServerRequest request) { return R.ok(service.create(request)); }

    @SaCheckPermission("game:server:edit")
    @Log(title = "区服管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public R<GameServer> update(@PathVariable Long id, @Valid @RequestBody ServerRequest request) { return R.ok(service.update(id, request)); }

    @SaCheckPermission("game:server:remove")
    @Log(title = "区服管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }

    @SaCheckPermission("game:server:edit")
    @PutMapping("/users/{userId}/scopes")
    public R<Void> scopes(@PathVariable Long userId, @RequestBody List<Long> serverIds) {
        service.setUserScopes(userId, serverIds); return R.ok();
    }

    @SaCheckPermission("game:server:query")
    @GetMapping("/{serverCode}/health")
    public R<HealthResult> health(@PathVariable String serverCode) { return R.ok(gateway.health(serverCode)); }
}
