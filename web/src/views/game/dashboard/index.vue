<template>
  <div class="p-4">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :lg="4">
        <el-card shadow="hover" class="mb-4 stat-card">
          <div class="text-gray-500">{{ card.label }}</div>
          <div class="mt-2 text-3xl font-bold" :class="card.className">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card shadow="never">
      <template #header>运营提示</template>
      <el-alert v-if="stats.pendingApprovals" type="warning" :closable="false" :title="`有 ${stats.pendingApprovals} 个奖励单等待另一位审批员复核`" />
      <el-alert
        v-if="stats.failedJobs"
        class="mt-3"
        type="error"
        :closable="false"
        :title="`发现 ${stats.failedJobs} 条失败任务日志，请前往任务中心处理`"
      />
      <el-empty v-if="!stats.pendingApprovals && !stats.failedJobs" description="当前没有待处理风险项" />
    </el-card>
  </div>
</template>

<script setup lang="ts" name="GameDashboard">
import { getDashboard } from '@/api/game';
import type { DashboardStats } from '@/api/game/types';

const stats = reactive<DashboardStats>({ enabledServers: 0, publishedActivities: 0, pendingApprovals: 0, rewardSuccessRate: 100, failedJobs: 0 });
const cards = computed(() => [
  { label: '启用区服', value: stats.enabledServers, className: 'text-blue-500' },
  { label: '已发布活动', value: stats.publishedActivities, className: 'text-green-500' },
  { label: '待审批奖励', value: stats.pendingApprovals, className: 'text-orange-500' },
  { label: '发奖成功率', value: `${stats.rewardSuccessRate.toFixed(1)}%`, className: 'text-emerald-500' },
  { label: '失败任务', value: stats.failedJobs, className: 'text-red-500' }
]);
onMounted(async () => Object.assign(stats, (await getDashboard()).data));
</script>
