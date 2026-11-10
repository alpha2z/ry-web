<template>
  <div class="p-4">
    <el-card shadow="hover">
      <template #header>玩家实时查询</template>
      <el-form :model="query" inline @submit.prevent="search">
        <el-form-item label="区服">
          <el-select v-model="query.serverCode" class="w-52" placeholder="选择授权区服">
            <el-option v-for="item in servers" :key="item.serverCode" :label="`${item.serverName} (${item.serverCode})`" :value="item.serverCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="玩家 ID"><el-input v-model="query.playerId" clearable placeholder="输入游戏内玩家 ID" /></el-form-item>
        <el-button type="primary" :loading="loading" @click="search">查询</el-button>
      </el-form>
    </el-card>
    <el-card v-if="player" class="mt-4" shadow="never">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="玩家 ID">{{ player.playerId }}</el-descriptions-item>
        <el-descriptions-item label="玩家名">{{ player.playerName }}</el-descriptions-item>
        <el-descriptions-item label="等级">{{ player.level }}</el-descriptions-item>
        <el-descriptions-item label="状态"
          ><el-tag type="success">{{ player.status }}</el-tag></el-descriptions-item
        >
        <el-descriptions-item label="资产摘要" :span="2">
          <pre>{{ JSON.stringify(player.assets, null, 2) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="GamePlayer">
import { getPlayer, listServers } from '@/api/game';
import type { GameServer, PlayerView } from '@/api/game/types';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const servers = ref<GameServer[]>([]);
const player = ref<PlayerView>();
const loading = ref(false);
const query = reactive({ serverCode: '', playerId: '' });
const search = async () => {
  if (!query.serverCode || !query.playerId) return proxy?.$modal.msgWarning('请选择区服并输入玩家 ID');
  loading.value = true;
  try {
    player.value = (await getPlayer(query.serverCode, query.playerId)).data;
  } finally {
    loading.value = false;
  }
};
onMounted(async () => {
  servers.value = (await listServers()).data;
  query.serverCode = servers.value[0]?.serverCode || '';
});
</script>
