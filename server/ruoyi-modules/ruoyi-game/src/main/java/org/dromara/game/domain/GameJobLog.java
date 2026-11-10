package org.dromara.game.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("game_job_log")
public class GameJobLog {
    @TableId
    private Long id;
    private String jobName;
    private String jobGroup;
    private String fireInstanceId;
    private Instant startedAt;
    private Instant finishedAt;
    private Long durationMs;
    private String status;
    private String errorMessage;
}
