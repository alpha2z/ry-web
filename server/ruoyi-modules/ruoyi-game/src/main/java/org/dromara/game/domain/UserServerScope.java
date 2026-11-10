package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("game_user_server_scope")
public class UserServerScope {
    @TableId
    private Long id;
    private Long userId;
    private Long serverId;
}
