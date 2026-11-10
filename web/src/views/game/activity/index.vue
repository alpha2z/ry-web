<template>
  <div class="p-4">
    <el-card shadow="hover">
      <template #header><el-button v-hasPermi="['game:activity:add']" type="primary" icon="Plus" @click="open()">创建活动</el-button></template>
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="activityCode" label="活动编码" min-width="150" />
        <el-table-column prop="activityName" label="活动名称" min-width="160" />
        <el-table-column prop="activityType" label="模板" width="180" />
        <el-table-column prop="status" label="状态" width="110"
          ><template #default="s"
            ><el-tag>{{ s.row.status }}</el-tag></template
          ></el-table-column
        >
        <el-table-column prop="currentVersion" label="版本" width="80" />
        <el-table-column prop="startAt" label="开始(UTC)" width="190" />
        <el-table-column prop="endAt" label="结束(UTC)" width="190" />
        <el-table-column label="操作" width="230">
          <template #default="s">
            <el-button v-if="s.row.status === 'DRAFT'" v-hasPermi="['game:activity:edit']" link type="primary" @click="open(s.row)">编辑</el-button>
            <el-button v-if="s.row.status === 'DRAFT'" v-hasPermi="['game:activity:publish']" link type="success" @click="publish(s.row)"
              >发布</el-button
            >
            <el-button v-if="s.row.status === 'PUBLISHED'" v-hasPermi="['game:activity:offline']" link type="danger" @click="offline(s.row)"
              >下线</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load" />
    </el-card>
    <el-dialog v-model="visible" :title="editingId ? '编辑活动草稿' : '创建活动'" width="860px" top="4vh">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16"
          ><el-col :span="12"
            ><el-form-item label="活动编码" required><el-input v-model="form.activityCode" /></el-form-item></el-col
          ><el-col :span="12"
            ><el-form-item label="活动名称" required><el-input v-model="form.activityName" /></el-form-item></el-col
        ></el-row>
        <el-form-item label="活动模板" required
          ><el-select v-model="form.activityType" @change="applyTemplate"
            ><el-option v-for="t in templates" :key="t.value" :label="t.label" :value="t.value" /></el-select
        ></el-form-item>
        <el-form-item label="目标区服" required
          ><el-select v-model="form.serverCodes" multiple class="w-full"
            ><el-option v-for="s in servers" :key="s.serverCode" :label="s.serverName" :value="s.serverCode" /></el-select
        ></el-form-item>
        <el-form-item label="活动时间" required
          ><el-date-picker v-model="activityRange" type="datetimerange" range-separator="至" start-placeholder="开始" end-placeholder="结束"
        /></el-form-item>
        <el-form-item label="预约发布"><el-date-picker v-model="publishAt" type="datetime" placeholder="留空为手动发布" /></el-form-item>
        <el-form-item label="活动时区"
          ><el-select v-model="form.timezone"
            ><el-option label="Asia/Singapore" value="Asia/Singapore" /><el-option label="Asia/Shanghai" value="Asia/Shanghai" /><el-option
              label="UTC"
              value="UTC" /></el-select
        ></el-form-item>
        <el-form-item label="规则配置"><el-input v-model="ruleText" type="textarea" :rows="6" class="font-mono" /></el-form-item>
        <el-form-item label="奖励档位"><el-input v-model="rewardText" type="textarea" :rows="6" class="font-mono" /></el-form-item>
        <el-form-item label="活动说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">保存草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="GameActivity">
import { addActivity, listActivities, listServers, offlineActivity, publishActivity, updateActivity } from '@/api/game';
import type { Activity, ActivityForm, GameServer } from '@/api/game/types';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const templates = [
  { label: '签到', value: 'SIGN_IN' },
  { label: '累计登录', value: 'CUMULATIVE_LOGIN' },
  { label: '单笔充值', value: 'SINGLE_RECHARGE' },
  { label: '累计充值', value: 'CUMULATIVE_RECHARGE' },
  { label: '排行榜', value: 'RANKING' },
  { label: '自定义', value: 'CUSTOM' }
];
const defaults: Record<string, unknown> = {
  SIGN_IN: { days: 7, reset: 'DAILY' },
  CUMULATIVE_LOGIN: { milestones: [1, 3, 7] },
  SINGLE_RECHARGE: { currency: 'USD', amount: 9.99 },
  CUMULATIVE_RECHARGE: { currency: 'USD', milestones: [10, 50, 100] },
  RANKING: { metric: 'score', settleAtEnd: true },
  CUSTOM: {}
};
const rows = ref<Activity[]>([]);
const servers = ref<GameServer[]>([]);
const loading = ref(false);
const total = ref(0);
const visible = ref(false);
const editingId = ref<string | number>();
const query = reactive({ pageNum: 1, pageSize: 10 });
const activityRange = ref<Date[]>([]);
const publishAt = ref<Date>();
const ruleText = ref('{}');
const rewardText = ref('[{"tier":1,"items":[{"itemCode":"gold","quantity":100}]}]');
const fresh = (): ActivityForm => ({
  activityCode: '',
  activityName: '',
  activityType: 'SIGN_IN',
  timezone: 'Asia/Singapore',
  startAt: '',
  endAt: '',
  serverCodes: [],
  rule: defaults.SIGN_IN as Record<string, unknown>,
  rewards: [],
  description: ''
});
const form = reactive<ActivityForm>(fresh());
const load = async () => {
  loading.value = true;
  try {
    const r = await listActivities(query);
    rows.value = r.rows;
    total.value = r.total;
  } finally {
    loading.value = false;
  }
};
const applyTemplate = () => {
  ruleText.value = JSON.stringify(defaults[form.activityType] || {}, null, 2);
};
const open = (row?: Activity) => {
  editingId.value = row?.id;
  Object.assign(form, fresh());
  activityRange.value = [];
  publishAt.value = undefined;
  if (row) {
    Object.assign(form, {
      activityCode: row.activityCode,
      activityName: row.activityName,
      activityType: row.activityType,
      timezone: row.timezone,
      serverCodes: JSON.parse(row.serverCodesJson),
      description: row.description
    });
    activityRange.value = [new Date(row.startAt), new Date(row.endAt)];
    publishAt.value = row.publishAt ? new Date(row.publishAt) : undefined;
    ruleText.value = JSON.stringify(JSON.parse(row.ruleJson), null, 2);
    rewardText.value = JSON.stringify(JSON.parse(row.rewardJson), null, 2);
  } else applyTemplate();
  visible.value = true;
};
const submit = async () => {
  try {
    if (activityRange.value.length !== 2) throw new Error('请选择活动时间');
    const payload = {
      ...form,
      startAt: activityRange.value[0].toISOString(),
      endAt: activityRange.value[1].toISOString(),
      displayStartAt: activityRange.value[0].toISOString(),
      publishAt: publishAt.value?.toISOString(),
      rule: JSON.parse(ruleText.value),
      rewards: JSON.parse(rewardText.value)
    };
    editingId.value ? await updateActivity(editingId.value, payload) : await addActivity(payload);
    visible.value = false;
    proxy?.$modal.msgSuccess('活动草稿已保存');
    await load();
  } catch (e) {
    proxy?.$modal.msgError(e instanceof Error ? e.message : '配置格式错误');
  }
};
const publish = async (row: Activity) => {
  await proxy?.$modal.confirm(`发布活动 ${row.activityName}？`);
  await publishActivity(row.id);
  await load();
};
const offline = async (row: Activity) => {
  await proxy?.$modal.confirm(`下线活动 ${row.activityName}？`);
  await offlineActivity(row.id);
  await load();
};
onMounted(async () => {
  servers.value = (await listServers()).data;
  await load();
});
</script>
