package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_activity_version")
public class ActivityVersion extends BaseEntity {
    @TableId
    private Long id;
    private Long activityId;
    private Integer versionNo;
    private String snapshotJson;
}
