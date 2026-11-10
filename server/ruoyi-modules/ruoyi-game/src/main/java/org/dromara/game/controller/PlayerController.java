package org.dromara.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.game.dto.AdapterModels.PlayerView;
import org.dromara.game.service.GameServerGateway;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/players")
public class PlayerController {
    private final GameServerGateway gateway;
    @SaCheckPermission("game:player:query")
    @GetMapping("/{serverCode}/{playerId}")
    public R<PlayerView> player(@PathVariable String serverCode, @PathVariable String playerId) {
        return R.ok(gateway.player(serverCode, playerId));
    }
}
