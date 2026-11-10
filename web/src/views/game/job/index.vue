<template>
  <div class="p-4">
    <el-card shadow="hover">
      <template #header>Quartz 集群任务</template>
      <el-table :data="jobs" border>
        <el-table-column prop="name" label="任务" min-width="220" /><el-table-column prop="group" label="分组" width="180" />
        <el-table-column prop="state" label="状态" width="120" /><el-table-column prop="cron" label="Cron" width="160" /><el-table-column
          prop="nextFireAt"
          label="下次执行(UTC)"
          min-width="190"
        />
        <el-table-column label="操作" width="220"
          ><template #default="s"
            ><el-button v-hasPermi="['game:job:run']" link type="primary" @click="run(s.row)">立即执行</el-button
            ><el-button v-if="s.row.state !== 'PAUSED'" v-hasPermi="['game:job:edit']" link type="warning" @click="pause(s.row)">暂停</el-button
            ><el-button v-else v-hasPermi="['game:job:edit']" link type="success" @click="resume(s.row)">恢复</el-button></template
          ></el-table-column
        >
      </el-table>
    </el-card>
    <el-card class="mt-4" shadow="never">
      <template #header>最近执行日志</template>
      <el-table :data="logs" border
        ><el-table-column prop="jobName" label="任务" /><el-table-column prop="status" label="结果" width="100" /><el-table-column
          prop="startedAt"
          label="开始时间"
          width="190" /><el-table-column prop="durationMs" label="耗时(ms)" width="110" /><el-table-column
          prop="errorMessage"
          label="错误"
          show-overflow-tooltip
      /></el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts" name="GameJob">
import { listJobLogs, listJobs, pauseJob, resumeJob, runJob } from '@/api/game';
import type { JobView } from '@/api/game/types';
const jobs = ref<JobView[]>([]);
const logs = ref<Record<string, unknown>[]>([]);
const load = async () => {
  jobs.value = (await listJobs()).data;
  logs.value = (await listJobLogs({ pageNum: 1, pageSize: 20 })).rows;
};
const run = async (j: JobView) => {
  await runJob(j.group, j.name);
  await load();
};
const pause = async (j: JobView) => {
  await pauseJob(j.group, j.name);
  await load();
};
const resume = async (j: JobView) => {
  await resumeJob(j.group, j.name);
  await load();
};
onMounted(load);
</script>
