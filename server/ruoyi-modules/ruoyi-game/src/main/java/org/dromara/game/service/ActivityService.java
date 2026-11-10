package org.dromara.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.game.domain.*;
import org.dromara.game.dto.AdapterModels.ActivityPayload;
import org.dromara.game.dto.AdapterModels.PublishResult;
import org.dromara.game.dto.GameRequests.ActivityRequest;
import org.dromara.game.mapper.GameMappers.*;
import org.dromara.game.quartz.ActivityOfflineJob;
import org.dromara.game.quartz.ActivityPublishJob;
import org.dromara.game.quartz.GameSchedulerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityMapper activityMapper;
    private final ActivityVersionMapper versionMapper;
    private final ActivityPublishMapper publishMapper;
    private final ObjectMapper objectMapper;
    private final GameServerService serverService;
    private final GameServerGateway gateway;
    private final GameSchedulerService scheduler;

    public TableDataInfo<GameActivity> list(PageQuery pageQuery, String status, String type) {
        Page<GameActivity> page = activityMapper.selectPage(pageQuery.build(), Wrappers.<GameActivity>lambdaQuery()
            .eq(status != null && !status.isBlank(), GameActivity::getStatus, status)
            .eq(type != null && !type.isBlank(), GameActivity::getActivityType, type)
            .orderByDesc(GameActivity::getCreateTime));
        return TableDataInfo.build(page);
    }

    public ActivityDetail detail(Long id) {
        GameActivity activity = require(id);
        return new ActivityDetail(activity,
            versionMapper.selectList(Wrappers.<ActivityVersion>lambdaQuery().eq(ActivityVersion::getActivityId, id)
                .orderByDesc(ActivityVersion::getVersionNo)),
            publishMapper.selectList(Wrappers.<ActivityPublish>lambdaQuery().eq(ActivityPublish::getActivityId, id)
                .orderByDesc(ActivityPublish::getCreateTime)));
    }

    @Transactional
    public GameActivity create(ActivityRequest request) {
        validate(request, null);
        GameActivity activity = apply(new GameActivity(), request);
        activity.setStatus(ActivityStatus.DRAFT.name());
        activity.setCurrentVersion(0);
        activityMapper.insert(activity);
        schedulePublishIfNeeded(activity);
        return activity;
    }

    @Transactional
    public GameActivity update(Long id, ActivityRequest request) {
        GameActivity activity = require(id);
        if (!ActivityStatus.DRAFT.name().equals(activity.getStatus())) throw new ServiceException("只能修改草稿活动");
        validate(request, id);
        apply(activity, request);
        activityMapper.updateById(activity);
        schedulePublishIfNeeded(activity);
        return activity;
    }

    public void publish(Long id) {
        GameActivity activity = require(id);
        if (ActivityStatus.OFFLINE.name().equals(activity.getStatus()) || ActivityStatus.EXPIRED.name().equals(activity.getStatus())) {
            throw new ServiceException("已下线或过期活动不可直接发布");
        }
        int nextVersion = activity.getCurrentVersion() + 1;
        String snapshot = toJson(activity);
        ActivityVersion version = new ActivityVersion();
        version.setActivityId(id);
        version.setVersionNo(nextVersion);
        version.setSnapshotJson(snapshot);
        versionMapper.insert(version);
        int success = publishVersion(activity, version);
        if (success > 0) {
            activity.setCurrentVersion(nextVersion);
            activity.setStatus(ActivityStatus.PUBLISHED.name());
            activityMapper.updateById(activity);
            if (activity.getEndAt().isAfter(Instant.now())) {
                scheduler.scheduleOnce("activity-offline-" + id, ActivityOfflineJob.class, activity.getEndAt(), Map.of("activityId", id));
            }
        }
    }

    public void republish(Long id, Long versionId, String serverCode) {
        GameActivity activity = require(id);
        serverService.requireAuthorized(serverCode);
        ActivityVersion version = versionMapper.selectById(versionId);
        if (version == null || !version.getActivityId().equals(id)) throw new ServiceException("活动版本不存在");
        sendVersion(activity, version, serverCode);
    }

    public void retryLatest(Long id, String serverCode) {
        GameActivity activity = require(id);
        serverService.requireAuthorized(serverCode);
        ActivityVersion version = versionMapper.selectOne(Wrappers.<ActivityVersion>lambdaQuery()
            .eq(ActivityVersion::getActivityId, id).orderByDesc(ActivityVersion::getVersionNo).last("limit 1"));
        if (version == null) throw new ServiceException("活动尚无发布版本");
        sendVersion(activity, version, serverCode);
    }

    public void offline(Long id) {
        GameActivity activity = require(id);
        int failures = 0;
        for (String serverCode : serverCodes(activity)) {
            PublishResult result = gateway.offlineSystem(serverCode, UUID.randomUUID().toString(), activity.getActivityCode());
            if (!result.success()) failures++;
        }
        if (failures == 0) {
            activity.setStatus(activity.getEndAt().isBefore(Instant.now()) ? ActivityStatus.EXPIRED.name() : ActivityStatus.OFFLINE.name());
            activityMapper.updateById(activity);
        } else {
            throw new ServiceException("部分区服下线失败，请检查区服连接");
        }
    }

    private int publishVersion(GameActivity activity, ActivityVersion version) {
        int success = 0;
        for (String serverCode : serverCodes(activity)) if (sendVersion(activity, version, serverCode)) success++;
        return success;
    }

    private boolean sendVersion(GameActivity activity, ActivityVersion version, String serverCode) {
        String requestId = activity.getActivityCode() + ":" + version.getVersionNo() + ":" + serverCode;
        Map<String, Object> snapshot = readMap(version.getSnapshotJson());
        PublishResult result = gateway.publishSystem(serverCode,
            new ActivityPayload(requestId, activity.getActivityCode(), version.getVersionNo(), snapshot));
        ActivityPublish record = publishMapper.selectOne(Wrappers.<ActivityPublish>lambdaQuery()
            .eq(ActivityPublish::getActivityId, activity.getId()).eq(ActivityPublish::getVersionId, version.getId())
            .eq(ActivityPublish::getServerCode, serverCode));
        if (record == null) record = new ActivityPublish();
        record.setActivityId(activity.getId());
        record.setVersionId(version.getId());
        record.setServerCode(serverCode);
        record.setRequestId(requestId);
        record.setStatus(result.success() ? "SUCCESS" : "FAILED");
        record.setExternalVersion(result.externalVersion());
        record.setFailureMessage(result.message());
        if (record.getId() == null) publishMapper.insert(record); else publishMapper.updateById(record);
        return result.success();
    }

    private void validate(ActivityRequest request, Long id) {
        ZoneId.of(request.timezone());
        if (!request.endAt().isAfter(request.startAt())) throw new ServiceException("活动结束时间必须晚于开始时间");
        request.serverCodes().forEach(serverService::requireAuthorized);
        boolean exists = activityMapper.exists(Wrappers.<GameActivity>lambdaQuery()
            .eq(GameActivity::getActivityCode, request.activityCode()).ne(id != null, GameActivity::getId, id));
        if (exists) throw new ServiceException("活动编码已存在");
    }

    private GameActivity apply(GameActivity a, ActivityRequest r) {
        a.setActivityCode(r.activityCode());
        a.setActivityName(r.activityName());
        a.setActivityType(r.activityType());
        a.setTimezone(r.timezone());
        a.setDisplayStartAt(r.displayStartAt());
        a.setStartAt(r.startAt());
        a.setEndAt(r.endAt());
        a.setPublishAt(r.publishAt());
        a.setServerCodesJson(toJson(r.serverCodes()));
        a.setRuleJson(toJson(r.rule()));
        a.setRewardJson(toJson(r.rewards()));
        a.setDescription(r.description());
        return a;
    }

    private void schedulePublishIfNeeded(GameActivity a) {
        if (a.getPublishAt() != null) {
            scheduler.scheduleOnce("activity-publish-" + a.getId(), ActivityPublishJob.class, a.getPublishAt(), Map.of("activityId", a.getId()));
        }
    }
    private List<String> serverCodes(GameActivity a) {
        try { return objectMapper.readValue(a.getServerCodesJson(), new TypeReference<>() {}); }
        catch (Exception e) { throw new ServiceException("活动区服配置损坏"); }
    }
    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new ServiceException("JSON 序列化失败"); }
    }
    private Map<String, Object> readMap(String value) {
        try { return objectMapper.readValue(value, new TypeReference<>() {}); }
        catch (Exception e) { throw new ServiceException("活动版本数据损坏"); }
    }
    private GameActivity require(Long id) {
        GameActivity activity = activityMapper.selectById(id);
        if (activity == null) throw new ServiceException("活动不存在");
        return activity;
    }
    public record ActivityDetail(GameActivity activity, List<ActivityVersion> versions, List<ActivityPublish> publishes) {}
}
