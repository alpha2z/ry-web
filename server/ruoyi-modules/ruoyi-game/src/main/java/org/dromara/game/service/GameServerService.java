package org.dromara.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.game.domain.GameServer;
import org.dromara.game.domain.UserServerScope;
import org.dromara.game.dto.GameRequests.ServerRequest;
import org.dromara.game.mapper.GameMappers.ScopeMapper;
import org.dromara.game.mapper.GameMappers.ServerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameServerService {
    private final ServerMapper serverMapper;
    private final ScopeMapper scopeMapper;
    private final CredentialCrypto crypto;

    public List<GameServer> listAuthorized() {
        Long userId = LoginHelper.getUserId();
        if (userId != null && userId == 1L) {
            return serverMapper.selectList(Wrappers.<GameServer>lambdaQuery().orderByAsc(GameServer::getServerCode));
        }
        Set<Long> ids = scopeMapper.selectList(Wrappers.<UserServerScope>lambdaQuery()
                .eq(UserServerScope::getUserId, userId)).stream()
            .map(UserServerScope::getServerId).collect(Collectors.toSet());
        if (ids.isEmpty()) return List.of();
        return serverMapper.selectList(Wrappers.<GameServer>lambdaQuery().in(GameServer::getId, ids)
            .orderByAsc(GameServer::getServerCode));
    }

    public GameServer requireAuthorized(String serverCode) {
        return listAuthorized().stream().filter(s -> s.getServerCode().equals(serverCode)).findFirst()
            .orElseThrow(() -> new ServiceException("区服不存在或无访问权限: " + serverCode));
    }

    public GameServer requireSystem(String serverCode) {
        GameServer server = serverMapper.selectOne(Wrappers.<GameServer>lambdaQuery()
            .eq(GameServer::getServerCode, serverCode).eq(GameServer::getEnabled, true));
        if (server == null) throw new ServiceException("区服不存在或已停用: " + serverCode);
        return server;
    }

    public String decryptedSecret(GameServer server) {
        return crypto.decrypt(server.getSecretCipher());
    }

    @Transactional
    public GameServer create(ServerRequest request) {
        if (serverMapper.exists(Wrappers.<GameServer>lambdaQuery().eq(GameServer::getServerCode, request.serverCode()))) {
            throw new ServiceException("区服编码已存在");
        }
        GameServer server = apply(new GameServer(), request, true);
        serverMapper.insert(server);
        return server;
    }

    @Transactional
    public GameServer update(Long id, ServerRequest request) {
        GameServer server = serverMapper.selectById(id);
        if (server == null) throw new ServiceException("区服不存在");
        if (!server.getServerCode().equals(request.serverCode()) &&
            serverMapper.exists(Wrappers.<GameServer>lambdaQuery().eq(GameServer::getServerCode, request.serverCode()))) {
            throw new ServiceException("区服编码已存在");
        }
        apply(server, request, false);
        serverMapper.updateById(server);
        return server;
    }

    @Transactional
    public void delete(Long id) {
        GameServer server = serverMapper.selectById(id);
        if (server == null) return;
        scopeMapper.delete(Wrappers.<UserServerScope>lambdaQuery().eq(UserServerScope::getServerId, id));
        serverMapper.deleteById(id);
    }

    private GameServer apply(GameServer server, ServerRequest r, boolean creating) {
        server.setServerCode(r.serverCode());
        server.setServerName(r.serverName());
        server.setBaseUrl(r.baseUrl().replaceAll("/+$", ""));
        server.setAppId(r.appId());
        if (creating && (r.secret() == null || r.secret().isBlank())) throw new ServiceException("新建区服必须提供密钥");
        if (r.secret() != null && !r.secret().isBlank()) server.setSecretCipher(crypto.encrypt(r.secret()));
        server.setTimeoutMs(r.timeoutMs() == null ? 3000 : r.timeoutMs());
        server.setEnabled(r.enabled() == null || r.enabled());
        server.setRemark(r.remark());
        return server;
    }

    @Transactional
    public void setUserScopes(Long userId, List<Long> serverIds) {
        scopeMapper.delete(Wrappers.<UserServerScope>lambdaQuery().eq(UserServerScope::getUserId, userId));
        serverIds.stream().distinct().forEach(serverId -> {
            if (serverMapper.selectById(serverId) == null) throw new ServiceException("区服不存在: " + serverId);
            UserServerScope scope = new UserServerScope();
            scope.setUserId(userId);
            scope.setServerId(serverId);
            scopeMapper.insert(scope);
        });
    }
}
