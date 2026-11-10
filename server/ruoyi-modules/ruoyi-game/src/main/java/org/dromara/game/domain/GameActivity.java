package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_activity")
public class GameActivity extends BaseEntity {
    @TableId
    private Long id;
    private String activityCode;
    private String activityName;
    private String activityType;
    private String status;
    private String timezone;
    private Instant displayStartAt;
    private Instant startAt;
    private Instant endAt;
    private Instant publishAt;
    private String serverCodesJson;
    private String ruleJson;
    private String rewardJson;
    private String description;
    private Integer currentVersion;
}
