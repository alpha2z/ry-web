package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_reward_target")
public class RewardTarget extends BaseEntity {
    @TableId
    private Long id;
    private Long rewardOrderId;
    private String serverCode;
    private String playerId;
    private String idempotencyKey;
    private String status;
    private Integer attempts;
    private String externalTxnId;
    private String failureCode;
    private String failureMessage;
    private Instant deliveredAt;
}
