package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_activity_publish")
public class ActivityPublish extends BaseEntity {
    @TableId
    private Long id;
    private Long activityId;
    private Long versionId;
    private String serverCode;
    private String requestId;
    private String status;
    private String externalVersion;
    private String failureMessage;
}
