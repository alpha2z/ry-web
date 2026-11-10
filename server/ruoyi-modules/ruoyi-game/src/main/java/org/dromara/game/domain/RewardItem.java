package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("game_reward_item")
public class RewardItem {
    @TableId
    private Long id;
    private Long rewardOrderId;
    private String itemCode;
    private Long quantity;
}
