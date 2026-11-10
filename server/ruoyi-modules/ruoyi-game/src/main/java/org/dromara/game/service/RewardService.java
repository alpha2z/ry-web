package org.dromara.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.game.domain.*;
import org.dromara.game.dto.AdapterModels.*;
import org.dromara.game.dto.GameRequests.RewardCreateRequest;
import org.dromara.game.mapper.GameMappers.*;
import org.dromara.game.quartz.GameSchedulerService;
import org.dromara.game.quartz.RewardDispatchJob;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardService {
    private final RewardOrderMapper orderMapper;
    private final RewardTargetMapper targetMapper;
    private final RewardItemMapper itemMapper;
    private final GameServerService serverService;
    private final GameServerGateway gateway;
    private final GameSchedulerService scheduler;

    public TableDataInfo<RewardOrder> list(PageQuery pageQuery, String status) {
        Page<RewardOrder> page = orderMapper.selectPage(pageQuery.build(), Wrappers.<RewardOrder>lambdaQuery()
            .eq(status != null && !status.isBlank(), RewardOrder::getStatus, status)
            .orderByDesc(RewardOrder::getCreateTime));
        return TableDataInfo.build(page);
    }

    public RewardDetail detail(Long id) {
        RewardOrder order = requireOrder(id);
        return new RewardDetail(order,
            targetMapper.selectList(Wrappers.<RewardTarget>lambdaQuery().eq(RewardTarget::getRewardOrderId, id)),
            itemMapper.selectList(Wrappers.<RewardItem>lambdaQuery().eq(RewardItem::getRewardOrderId, id)));
    }

    @Transactional
    public RewardOrder create(RewardCreateRequest request) {
        String timezone = request.timezone() == null || request.timezone().isBlank() ? "Asia/Singapore" : request.timezone();
        ZoneId.of(timezone);
        request.targets().forEach(target -> serverService.requireAuthorized(target.serverCode()));
        if (request.scheduledAt() != null && request.scheduledAt().isBefore(Instant.now())) {
            throw new ServiceException("预约时间必须在未来");
        }
        RewardOrder order = new RewardOrder();
        order.setOrderNo("RW-" + UUID.randomUUID().toString().replace("-", ""));
        order.setTitle(request.title());
        order.setMailTitle(request.mailTitle());
        order.setMailContent(request.mailContent());
        order.setScheduledAt(request.scheduledAt());
        order.setTimezone(timezone);
        order.setRemark(request.remark());
        order.setTargetCount(request.targets().size());
        order.setSuccessCount(0);
        order.setFailedCount(0);
        boolean approvalRequired = RewardPolicy.requiresApproval(request.targets().size(), request.scheduledAt());
        order.setDeliveryMode(request.scheduledAt() != null ? "SCHEDULED" : request.targets().size() > 1 ? "BATCH" : "IMMEDIATE");
        order.setStatus(approvalRequired ? RewardStatus.PENDING_APPROVAL.name() : RewardStatus.APPROVED.name());
        orderMapper.insert(order);

        request.targets().stream().distinct().forEach(target -> {
            RewardTarget entity = new RewardTarget();
            entity.setRewardOrderId(order.getId());
            entity.setServerCode(target.serverCode());
            entity.setPlayerId(target.playerId());
            entity.setIdempotencyKey(sha256(order.getOrderNo() + ":" + target.serverCode() + ":" + target.playerId()));
            entity.setStatus(DeliveryStatus.PENDING.name());
            entity.setAttempts(0);
            targetMapper.insert(entity);
        });
        request.items().forEach(item -> {
            RewardItem entity = new RewardItem();
            entity.setRewardOrderId(order.getId());
            entity.setItemCode(item.itemCode());
            entity.setQuantity(item.quantity());
            itemMapper.insert(entity);
        });
        if (!approvalRequired) execute(order.getId());
        return order;
    }

    @Transactional
    public void approve(Long id) {
        RewardOrder order = requireOrder(id);
        if (!RewardStatus.PENDING_APPROVAL.name().equals(order.getStatus())) throw new ServiceException("该奖励单不在待审批状态");
        Long approver = LoginHelper.getUserId();
        RewardPolicy.requireDifferentApprover(order.getCreateBy(), approver);
        order.setApprovedBy(approver);
        order.setApprovedAt(Instant.now());
        order.setStatus(RewardStatus.APPROVED.name());
        orderMapper.updateById(order);
        if (order.getScheduledAt() != null && order.getScheduledAt().isAfter(Instant.now())) {
            scheduler.scheduleOnce("reward-" + order.getId(), RewardDispatchJob.class, order.getScheduledAt(), java.util.Map.of("orderId", order.getId()));
        } else {
            execute(order.getId());
        }
    }

    public void execute(Long id) {
        RewardOrder order = requireOrder(id);
        int locked = orderMapper.update(null, Wrappers.<RewardOrder>lambdaUpdate()
            .eq(RewardOrder::getId, id)
            .in(RewardOrder::getStatus, RewardStatus.APPROVED.name(), RewardStatus.FAILED.name(), RewardStatus.PARTIAL_SUCCESS.name())
            .set(RewardOrder::getStatus, RewardStatus.PROCESSING.name()));
        if (locked == 0) return;
        List<RewardItem> items = itemMapper.selectList(Wrappers.<RewardItem>lambdaQuery().eq(RewardItem::getRewardOrderId, id));
        List<RewardLine> lines = items.stream().map(i -> new RewardLine(i.getItemCode(), i.getQuantity())).toList();
        List<RewardTarget> targets = targetMapper.selectList(Wrappers.<RewardTarget>lambdaQuery()
            .eq(RewardTarget::getRewardOrderId, id).in(RewardTarget::getStatus,
                DeliveryStatus.PENDING.name(), DeliveryStatus.RETRYABLE_FAILED.name()));
        for (RewardTarget target : targets) deliver(order, target, lines);
        refreshStatus(id);
    }

    private void deliver(RewardOrder order, RewardTarget target, List<RewardLine> lines) {
        target.setStatus(DeliveryStatus.PROCESSING.name());
        target.setAttempts(target.getAttempts() + 1);
        targetMapper.updateById(target);
        DeliveryResult result = gateway.rewardSystem(new RewardGrant(target.getIdempotencyKey(), target.getServerCode(),
            target.getPlayerId(), order.getMailTitle(), order.getMailContent(), lines));
        if (result.success()) {
            target.setStatus(DeliveryStatus.SUCCESS.name());
            target.setExternalTxnId(result.externalTxnId());
            target.setDeliveredAt(Instant.now());
            target.setFailureCode(null);
            target.setFailureMessage(null);
        } else {
            target.setStatus(result.retryable() ? DeliveryStatus.RETRYABLE_FAILED.name() : DeliveryStatus.REJECTED.name());
            target.setFailureCode(result.code());
            target.setFailureMessage(result.message());
        }
        targetMapper.updateById(target);
    }

    @Transactional
    public void cancel(Long id) {
        RewardOrder order = requireOrder(id);
        if (!List.of(RewardStatus.DRAFT.name(), RewardStatus.PENDING_APPROVAL.name(), RewardStatus.APPROVED.name()).contains(order.getStatus())) {
            throw new ServiceException("当前状态不可取消");
        }
        order.setStatus(RewardStatus.CANCELLED.name());
        orderMapper.updateById(order);
    }

    public void retryAllFailed() {
        orderMapper.selectList(Wrappers.<RewardOrder>lambdaQuery()
            .in(RewardOrder::getStatus, RewardStatus.FAILED.name(), RewardStatus.PARTIAL_SUCCESS.name()))
            .forEach(order -> execute(order.getId()));
    }

    private void refreshStatus(Long id) {
        List<RewardTarget> targets = targetMapper.selectList(Wrappers.<RewardTarget>lambdaQuery().eq(RewardTarget::getRewardOrderId, id));
        int success = (int) targets.stream().filter(t -> DeliveryStatus.SUCCESS.name().equals(t.getStatus())).count();
        int failed = targets.size() - success;
        RewardOrder order = requireOrder(id);
        order.setSuccessCount(success);
        order.setFailedCount(failed);
        order.setStatus(success == targets.size() ? RewardStatus.SUCCESS.name() : success > 0 ? RewardStatus.PARTIAL_SUCCESS.name() : RewardStatus.FAILED.name());
        orderMapper.updateById(order);
    }

    private RewardOrder requireOrder(Long id) {
        RewardOrder order = orderMapper.selectById(id);
        if (order == null) throw new ServiceException("奖励单不存在");
        return order;
    }
    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public record RewardDetail(RewardOrder order, List<RewardTarget> targets, List<RewardItem> items) {}
}
