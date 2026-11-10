create table game_server (
    id bigint not null,
    server_code varchar(64) not null,
    server_name varchar(128) not null,
    base_url varchar(512) not null,
    app_id varchar(128) not null,
    secret_cipher varchar(1024) null,
    timeout_ms int not null default 3000,
    enabled tinyint(1) not null default 1,
    remark varchar(500) null,
    create_dept bigint null, create_by bigint null, create_time datetime(3) null,
    update_by bigint null, update_time datetime(3) null,
    primary key (id), unique key uk_game_server_code (server_code)
) engine=innodb comment='游戏区服';

create table game_user_server_scope (
    id bigint not null, user_id bigint not null, server_id bigint not null,
    primary key (id), unique key uk_user_server (user_id, server_id), key idx_scope_server (server_id)
) engine=innodb comment='用户区服数据权限';

create table game_reward_order (
    id bigint not null, order_no varchar(64) not null, title varchar(200) not null,
    mail_title varchar(200) not null, mail_content text not null,
    status varchar(32) not null, delivery_mode varchar(32) not null,
    scheduled_at datetime(3) null, approved_by bigint null, approved_at datetime(3) null,
    target_count int not null default 0, success_count int not null default 0, failed_count int not null default 0,
    timezone varchar(64) not null default 'Asia/Singapore', remark varchar(500) null,
    create_dept bigint null, create_by bigint null, create_time datetime(3) null,
    update_by bigint null, update_time datetime(3) null,
    primary key (id), unique key uk_reward_order_no (order_no), key idx_reward_status_schedule (status, scheduled_at)
) engine=innodb comment='奖励发放订单';

create table game_reward_target (
    id bigint not null, reward_order_id bigint not null, server_code varchar(64) not null,
    player_id varchar(128) not null, idempotency_key char(64) not null, status varchar(32) not null,
    attempts int not null default 0, external_txn_id varchar(128) null,
    failure_code varchar(64) null, failure_message varchar(1000) null, delivered_at datetime(3) null,
    create_dept bigint null, create_by bigint null, create_time datetime(3) null,
    update_by bigint null, update_time datetime(3) null,
    primary key (id), unique key uk_reward_idempotency (idempotency_key),
    key idx_reward_target_order (reward_order_id), key idx_reward_target_status (status)
) engine=innodb comment='奖励接收目标';

create table game_reward_item (
    id bigint not null, reward_order_id bigint not null, item_code varchar(128) not null, quantity bigint not null,
    primary key (id), key idx_reward_item_order (reward_order_id)
) engine=innodb comment='奖励内容';

create table game_activity (
    id bigint not null, activity_code varchar(64) not null, activity_name varchar(200) not null,
    activity_type varchar(32) not null, status varchar(32) not null, timezone varchar(64) not null,
    display_start_at datetime(3) null, start_at datetime(3) not null, end_at datetime(3) not null,
    publish_at datetime(3) null, server_codes_json json not null, rule_json json not null,
    reward_json json not null, description text null, current_version int not null default 0,
    create_dept bigint null, create_by bigint null, create_time datetime(3) null,
    update_by bigint null, update_time datetime(3) null,
    primary key (id), unique key uk_activity_code (activity_code), key idx_activity_status_time (status, start_at, end_at)
) engine=innodb comment='运营活动';

create table game_activity_version (
    id bigint not null, activity_id bigint not null, version_no int not null, snapshot_json json not null,
    create_dept bigint null, create_by bigint null, create_time datetime(3) null,
    update_by bigint null, update_time datetime(3) null,
    primary key (id), unique key uk_activity_version (activity_id, version_no)
) engine=innodb comment='不可变活动版本';

create table game_activity_publish (
    id bigint not null, activity_id bigint not null, version_id bigint not null, server_code varchar(64) not null,
    request_id varchar(200) not null, status varchar(32) not null, external_version varchar(128) null,
    failure_message varchar(1000) null,
    create_dept bigint null, create_by bigint null, create_time datetime(3) null,
    update_by bigint null, update_time datetime(3) null,
    primary key (id), unique key uk_activity_publish (version_id, server_code), key idx_publish_activity (activity_id)
) engine=innodb comment='活动区服发布记录';

create table game_job_log (
    id bigint not null, job_name varchar(200) not null, job_group varchar(200) not null,
    fire_instance_id varchar(200) null, started_at datetime(3) not null, finished_at datetime(3) null,
    duration_ms bigint null, status varchar(32) not null, error_message varchar(1000) null,
    primary key (id), key idx_game_job_started (started_at), key idx_game_job_status (status)
) engine=innodb comment='业务任务执行日志';

create table QRTZ_JOB_DETAILS (
 SCHED_NAME varchar(120) not null, JOB_NAME varchar(200) not null, JOB_GROUP varchar(200) not null,
 DESCRIPTION varchar(250) null, JOB_CLASS_NAME varchar(250) not null, IS_DURABLE varchar(1) not null,
 IS_NONCONCURRENT varchar(1) not null, IS_UPDATE_DATA varchar(1) not null, REQUESTS_RECOVERY varchar(1) not null,
 JOB_DATA blob null, primary key (SCHED_NAME,JOB_NAME,JOB_GROUP)
) engine=innodb;
create table QRTZ_TRIGGERS (
 SCHED_NAME varchar(120) not null, TRIGGER_NAME varchar(200) not null, TRIGGER_GROUP varchar(200) not null,
 JOB_NAME varchar(200) not null, JOB_GROUP varchar(200) not null, DESCRIPTION varchar(250) null,
 NEXT_FIRE_TIME bigint null, PREV_FIRE_TIME bigint null, PRIORITY integer null, TRIGGER_STATE varchar(16) not null,
 TRIGGER_TYPE varchar(8) not null, START_TIME bigint not null, END_TIME bigint null, CALENDAR_NAME varchar(200) null,
 MISFIRE_INSTR smallint null, JOB_DATA blob null, primary key (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
 key IDX_QRTZ_T_J (SCHED_NAME,JOB_NAME,JOB_GROUP), key IDX_QRTZ_T_STATE (SCHED_NAME,TRIGGER_STATE),
 constraint FK_QRTZ_TRIGGERS_JOB foreign key (SCHED_NAME,JOB_NAME,JOB_GROUP) references QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
) engine=innodb;
create table QRTZ_SIMPLE_TRIGGERS (
 SCHED_NAME varchar(120) not null, TRIGGER_NAME varchar(200) not null, TRIGGER_GROUP varchar(200) not null,
 REPEAT_COUNT bigint not null, REPEAT_INTERVAL bigint not null, TIMES_TRIGGERED bigint not null,
 primary key (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), constraint FK_QRTZ_SIMPLE foreign key
 (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) references QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
) engine=innodb;
create table QRTZ_CRON_TRIGGERS (
 SCHED_NAME varchar(120) not null, TRIGGER_NAME varchar(200) not null, TRIGGER_GROUP varchar(200) not null,
 CRON_EXPRESSION varchar(120) not null, TIME_ZONE_ID varchar(80), primary key (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
 constraint FK_QRTZ_CRON foreign key (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) references QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
) engine=innodb;
create table QRTZ_SIMPROP_TRIGGERS (
 SCHED_NAME varchar(120) not null, TRIGGER_NAME varchar(200) not null, TRIGGER_GROUP varchar(200) not null,
 STR_PROP_1 varchar(512) null, STR_PROP_2 varchar(512) null, STR_PROP_3 varchar(512) null,
 INT_PROP_1 int null, INT_PROP_2 int null, LONG_PROP_1 bigint null, LONG_PROP_2 bigint null,
 DEC_PROP_1 numeric(13,4) null, DEC_PROP_2 numeric(13,4) null, BOOL_PROP_1 varchar(1) null, BOOL_PROP_2 varchar(1) null,
 primary key (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), constraint FK_QRTZ_SIMPROP foreign key
 (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) references QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
) engine=innodb;
create table QRTZ_BLOB_TRIGGERS (
 SCHED_NAME varchar(120) not null, TRIGGER_NAME varchar(200) not null, TRIGGER_GROUP varchar(200) not null,
 BLOB_DATA blob null, primary key (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), constraint FK_QRTZ_BLOB foreign key
 (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) references QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
) engine=innodb;
create table QRTZ_CALENDARS (SCHED_NAME varchar(120) not null, CALENDAR_NAME varchar(200) not null, CALENDAR blob not null, primary key (SCHED_NAME,CALENDAR_NAME)) engine=innodb;
create table QRTZ_PAUSED_TRIGGER_GRPS (SCHED_NAME varchar(120) not null, TRIGGER_GROUP varchar(200) not null, primary key (SCHED_NAME,TRIGGER_GROUP)) engine=innodb;
create table QRTZ_FIRED_TRIGGERS (
 SCHED_NAME varchar(120) not null, ENTRY_ID varchar(95) not null, TRIGGER_NAME varchar(200) not null,
 TRIGGER_GROUP varchar(200) not null, INSTANCE_NAME varchar(200) not null, FIRED_TIME bigint not null,
 SCHED_TIME bigint not null, PRIORITY integer not null, STATE varchar(16) not null,
 JOB_NAME varchar(200) null, JOB_GROUP varchar(200) null, IS_NONCONCURRENT varchar(1) null,
 REQUESTS_RECOVERY varchar(1) null, primary key (SCHED_NAME,ENTRY_ID),
 key IDX_QRTZ_FT_TRIG_INST_NAME (SCHED_NAME,INSTANCE_NAME), key IDX_QRTZ_FT_J_G (SCHED_NAME,JOB_NAME,JOB_GROUP)
) engine=innodb;
create table QRTZ_SCHEDULER_STATE (SCHED_NAME varchar(120) not null, INSTANCE_NAME varchar(200) not null, LAST_CHECKIN_TIME bigint not null, CHECKIN_INTERVAL bigint not null, primary key (SCHED_NAME,INSTANCE_NAME)) engine=innodb;
create table QRTZ_LOCKS (SCHED_NAME varchar(120) not null, LOCK_NAME varchar(40) not null, primary key (SCHED_NAME,LOCK_NAME)) engine=innodb;

insert into game_server (id, server_code, server_name, base_url, app_id, timeout_ms, enabled, remark, create_by, create_time)
values (9001, 'S1', '本地演示区服', 'http://wiremock:8080', 'game-admin-local', 3000, 1, 'Docker WireMock 演示区服', 1, utc_timestamp());

update sys_menu set visible='1' where menu_id in (5,6,117,118,120,121,122);
insert into sys_menu values(2000, '游戏运营', 0, 1, 'game', null, '', 1, 0, 'M', '0', '0', '', 'game', 103, 1, sysdate(), null, null, '游戏运营后台');
insert into sys_menu values(2001, '运营仪表盘', 2000, 1, 'dashboard', 'game/dashboard/index', '', 1, 0, 'C', '0', '0', 'game:dashboard:view', 'dashboard', 103, 1, sysdate(), null, null, '运营仪表盘');
insert into sys_menu values(2002, '玩家查询', 2000, 2, 'players', 'game/player/index', '', 1, 0, 'C', '0', '0', 'game:player:query', 'user', 103, 1, sysdate(), null, null, '玩家查询');
insert into sys_menu values(2003, '区服管理', 2000, 3, 'servers', 'game/server/index', '', 1, 0, 'C', '0', '0', 'game:server:list', 'server', 103, 1, sysdate(), null, null, '区服管理');
insert into sys_menu values(2004, '奖励中心', 2000, 4, 'rewards', 'game/reward/index', '', 1, 0, 'C', '0', '0', 'game:reward:list', 'gift', 103, 1, sysdate(), null, null, '奖励发放与审批');
insert into sys_menu values(2005, '活动中心', 2000, 5, 'activities', 'game/activity/index', '', 1, 0, 'C', '0', '0', 'game:activity:list', 'calendar', 103, 1, sysdate(), null, null, '活动配置与发布');
insert into sys_menu values(2006, '任务中心', 2000, 6, 'jobs', 'game/job/index', '', 1, 0, 'C', '0', '0', 'game:job:list', 'job', 103, 1, sysdate(), null, null, 'Quartz任务中心');

insert into sys_menu values(2010, '区服查询', 2003, 1, '', '', '', 1, 0, 'F', '0', '0', 'game:server:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2011, '区服新增', 2003, 2, '', '', '', 1, 0, 'F', '0', '0', 'game:server:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2012, '区服修改', 2003, 3, '', '', '', 1, 0, 'F', '0', '0', 'game:server:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2013, '区服删除', 2003, 4, '', '', '', 1, 0, 'F', '0', '0', 'game:server:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2020, '奖励详情', 2004, 1, '', '', '', 1, 0, 'F', '0', '0', 'game:reward:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2021, '奖励创建', 2004, 2, '', '', '', 1, 0, 'F', '0', '0', 'game:reward:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2022, '奖励审批', 2004, 3, '', '', '', 1, 0, 'F', '0', '0', 'game:reward:approve', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2023, '奖励执行', 2004, 4, '', '', '', 1, 0, 'F', '0', '0', 'game:reward:execute', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2024, '奖励重试', 2004, 5, '', '', '', 1, 0, 'F', '0', '0', 'game:reward:retry', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2025, '奖励取消', 2004, 6, '', '', '', 1, 0, 'F', '0', '0', 'game:reward:cancel', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2030, '活动详情', 2005, 1, '', '', '', 1, 0, 'F', '0', '0', 'game:activity:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2031, '活动创建', 2005, 2, '', '', '', 1, 0, 'F', '0', '0', 'game:activity:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2032, '活动修改', 2005, 3, '', '', '', 1, 0, 'F', '0', '0', 'game:activity:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2033, '活动发布', 2005, 4, '', '', '', 1, 0, 'F', '0', '0', 'game:activity:publish', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2034, '活动下线', 2005, 5, '', '', '', 1, 0, 'F', '0', '0', 'game:activity:offline', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2040, '任务修改', 2006, 1, '', '', '', 1, 0, 'F', '0', '0', 'game:job:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values(2041, '任务执行', 2006, 2, '', '', '', 1, 0, 'F', '0', '0', 'game:job:run', '#', 103, 1, sysdate(), null, null, '');

insert into sys_role values(10, '000000', '游戏运营', 'game_ops', 10, 2, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '活动与奖励运营');
insert into sys_role values(11, '000000', '奖励审批员', 'game_approver', 11, 2, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '批量及预约奖励复核');
insert into sys_role values(12, '000000', '游戏管理员', 'game_admin', 12, 1, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '游戏后台管理员');
insert into sys_role values(13, '000000', '游戏审计员', 'game_auditor', 13, 5, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '只读审计角色');

insert ignore into sys_role_menu (role_id, menu_id) select 1, menu_id from sys_menu where menu_id between 2000 and 2041;
insert ignore into sys_role_menu (role_id, menu_id) select 12, menu_id from sys_menu where menu_id between 2000 and 2041;
insert ignore into sys_role_menu (role_id, menu_id) select 10, menu_id from sys_menu where menu_id in (2000,2001,2002,2004,2005,2006,2020,2021,2024,2025,2030,2031,2032,2033,2034);
insert ignore into sys_role_menu (role_id, menu_id) select 11, menu_id from sys_menu where menu_id in (2000,2001,2004,2020,2022);
insert ignore into sys_role_menu (role_id, menu_id) select 13, menu_id from sys_menu where menu_id in (2000,2001,2002,2003,2004,2005,2006,2010,2020,2030);
