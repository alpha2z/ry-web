<template>
  <div class="p-4">
    <el-card shadow="hover">
      <template #header>
        <el-row justify="space-between">
          <el-button v-hasPermi="['game:reward:add']" type="primary" icon="Plus" @click="open">创建奖励</el-button>
          <el-select v-model="query.status" clearable placeholder="全部状态" class="w-48" @change="load">
            <el-option v-for="s in statuses" :key="s" :value="s" :label="s" />
          </el-select>
        </el-row>
      </template>
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="orderNo" label="订单号" min-width="210" />
        <el-table-column prop="title" label="说明" min-width="160" />
        <el-table-column prop="deliveryMode" label="模式" width="110" />
        <el-table-column prop="status" label="状态" width="150"
          ><template #default="s"
            ><el-tag>{{ s.row.status }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="进度" width="130"
          ><template #default="s">{{ s.row.successCount }}/{{ s.row.targetCount }} 成功</template></el-table-column
        >
        <el-table-column prop="scheduledAt" label="预约时间(UTC)" width="190" />
        <el-table-column label="操作" width="230">
          <template #default="s">
            <el-button
              v-if="s.row.status === 'PENDING_APPROVAL'"
              v-hasPermi="['game:reward:approve']"
              link
              type="success"
              @click="action(s.row, 'approve')"
              >审批</el-button
            >
            <el-button
              v-if="['FAILED', 'PARTIAL_SUCCESS'].includes(s.row.status)"
              v-hasPermi="['game:reward:retry']"
              link
              type="warning"
              @click="action(s.row, 'retry')"
              >重试</el-button
            >
            <el-button
              v-if="['PENDING_APPROVAL', 'APPROVED'].includes(s.row.status)"
              v-hasPermi="['game:reward:cancel']"
              link
              type="danger"
              @click="action(s.row, 'cancel')"
              >取消</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load" />
    </el-card>

    <el-dialog v-model="visible" title="创建奖励" width="820px" top="5vh">
      <el-form ref="formRef" :model="form" label-width="110px">
        <el-form-item label="业务说明" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="邮件标题" required><el-input v-model="form.mailTitle" /></el-form-item>
        <el-form-item label="邮件正文" required><el-input v-model="form.mailContent" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="预约时间"><el-date-picker v-model="scheduledAt" type="datetime" placeholder="留空立即执行" /></el-form-item>
        <el-divider content-position="left">接收玩家（多于 1 人或设置预约时间时必须复核）</el-divider>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".csv" :on-change="importCsv"
          ><el-button>导入 CSV（serverCode,playerId）</el-button></el-upload
        >
        <el-row v-for="(target, i) in form.targets" :key="i" :gutter="8" class="mt-2">
          <el-col :span="8"
            ><el-select v-model="target.serverCode" placeholder="区服" class="w-full"
              ><el-option v-for="s in servers" :key="s.serverCode" :label="s.serverName" :value="s.serverCode" /></el-select
          ></el-col>
          <el-col :span="12"><el-input v-model="target.playerId" placeholder="玩家 ID" /></el-col>
          <el-col :span="4"><el-button type="danger" link @click="form.targets.splice(i, 1)">删除</el-button></el-col>
        </el-row>
        <el-button class="mt-2" link type="primary" @click="form.targets.push({ serverCode: servers[0]?.serverCode || '', playerId: '' })"
          >+ 添加玩家</el-button
        >
        <el-divider content-position="left">奖励内容</el-divider>
        <el-row v-for="(item, i) in form.items" :key="i" :gutter="8" class="mt-2">
          <el-col :span="12"><el-input v-model="item.itemCode" placeholder="道具/货币编码" /></el-col>
          <el-col :span="8"><el-input-number v-model="item.quantity" :min="1" class="w-full" /></el-col>
          <el-col :span="4"><el-button type="danger" link @click="form.items.splice(i, 1)">删除</el-button></el-col>
        </el-row>
        <el-button class="mt-2" link type="primary" @click="form.items.push({ itemCode: '', quantity: 1 })">+ 添加奖励</el-button>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="GameReward">
import type { UploadFile } from 'element-plus';
import { addReward, approveReward, cancelReward, listRewards, listServers, retryReward } from '@/api/game';
import type { GameServer, RewardForm, RewardOrder } from '@/api/game/types';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const statuses = ['PENDING_APPROVAL', 'APPROVED', 'PROCESSING', 'PARTIAL_SUCCESS', 'SUCCESS', 'FAILED', 'CANCELLED'];
const rows = ref<RewardOrder[]>([]);
const servers = ref<GameServer[]>([]);
const loading = ref(false);
const total = ref(0);
const visible = ref(false);
const scheduledAt = ref<Date>();
const query = reactive({ pageNum: 1, pageSize: 10, status: '' });
const fresh = (): RewardForm => ({
  title: '',
  mailTitle: '',
  mailContent: '',
  targets: [{ serverCode: servers.value[0]?.serverCode || '', playerId: '' }],
  items: [{ itemCode: 'gold', quantity: 100 }],
  timezone: 'Asia/Singapore'
});
const form = reactive<RewardForm>(fresh());
const load = async () => {
  loading.value = true;
  try {
    const res = await listRewards(query);
    rows.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};
const open = () => {
  Object.assign(form, fresh());
  scheduledAt.value = undefined;
  visible.value = true;
};
const importCsv = async (file: UploadFile) => {
  const text = await file.raw?.text();
  if (!text) return;
  form.targets = text
    .split(/\r?\n/)
    .slice(1)
    .filter(Boolean)
    .map((line) => {
      const [serverCode, playerId] = line.split(',').map((v) => v.trim());
      return { serverCode, playerId };
    });
};
const submit = async () => {
  if (
    !form.title ||
    !form.mailTitle ||
    !form.mailContent ||
    !form.targets.length ||
    !form.items.length ||
    form.targets.some((x) => !x.serverCode || !x.playerId) ||
    form.items.some((x) => !x.itemCode || x.quantity < 1)
  )
    return proxy?.$modal.msgWarning('请完整填写奖励信息');
  await addReward({ ...form, scheduledAt: scheduledAt.value?.toISOString() });
  proxy?.$modal.msgSuccess('奖励单已创建');
  visible.value = false;
  await load();
};
const action = async (row: RewardOrder, type: string) => {
  await proxy?.$modal.confirm(`确认${type === 'approve' ? '审批' : type === 'retry' ? '重试' : '取消'}奖励单 ${row.orderNo}？`);
  if (type === 'approve') await approveReward(row.id);
  else if (type === 'retry') await retryReward(row.id);
  else await cancelReward(row.id);
  await load();
};
onMounted(async () => {
  servers.value = (await listServers()).data;
  await load();
});
</script>
