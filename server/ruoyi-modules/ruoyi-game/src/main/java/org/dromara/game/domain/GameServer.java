package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_server")
public class GameServer extends BaseEntity {
    @TableId
    private Long id;
    private String serverCode;
    private String serverName;
    private String baseUrl;
    private String appId;
    @JsonIgnore
    private String secretCipher;
    private Integer timeoutMs;
    private Boolean enabled;
    private String remark;
}
