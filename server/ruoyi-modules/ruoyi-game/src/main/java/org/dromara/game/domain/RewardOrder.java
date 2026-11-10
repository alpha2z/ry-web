package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_reward_order")
public class RewardOrder extends BaseEntity {
    @TableId
    private Long id;
    private String orderNo;
    private String title;
    private String mailTitle;
    private String mailContent;
    private String status;
    private String deliveryMode;
    private Instant scheduledAt;
    private Long approvedBy;
    private Instant approvedAt;
    private Integer targetCount;
    private Integer successCount;
    private Integer failedCount;
    private String timezone;
    private String remark;
}
