<template>
  <div class="p-4">
    <el-card shadow="hover">
      <template #header>
        <el-button v-hasPermi="['game:server:add']" type="primary" icon="Plus" @click="open()">新增区服</el-button>
      </template>
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="serverCode" label="编码" width="120" />
        <el-table-column prop="serverName" label="区服名称" />
        <el-table-column prop="baseUrl" label="游戏服地址" min-width="240" show-overflow-tooltip />
        <el-table-column prop="timeoutMs" label="超时(ms)" width="110" />
        <el-table-column label="状态" width="90"
          ><template #default="s"
            ><el-tag :type="s.row.enabled ? 'success' : 'info'">{{ s.row.enabled ? '启用' : '停用' }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="操作" width="220">
          <template #default="s">
            <el-button link type="primary" @click="health(s.row)">检查</el-button>
            <el-button v-hasPermi="['game:server:edit']" link type="primary" @click="open(s.row)">编辑</el-button>
            <el-button v-hasPermi="['game:server:remove']" link type="danger" @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="editData.id ? '编辑区服' : '新增区服'" width="620px">
      <el-form ref="formRef" :model="editData" :rules="rules" label-width="110px">
        <el-form-item label="区服编码" prop="serverCode"><el-input v-model="editData.serverCode" /></el-form-item>
        <el-form-item label="区服名称" prop="serverName"><el-input v-model="editData.serverName" /></el-form-item>
        <el-form-item label="接口地址" prop="baseUrl"
          ><el-input v-model="editData.baseUrl" placeholder="https://game.example.com 或 mock://local"
        /></el-form-item>
        <el-form-item label="App ID" prop="appId"><el-input v-model="editData.appId" /></el-form-item>
        <el-form-item label="HMAC 密钥" :required="!editData.id"
          ><el-input v-model="editData.secret" type="password" show-password :placeholder="editData.id ? '留空表示不修改' : '至少使用 32 位随机密钥'"
        /></el-form-item>
        <el-form-item label="超时"><el-input-number v-model="editData.timeoutMs" :min="500" :max="30000" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="editData.enabled" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editData.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="GameServer">
import { addServer, checkServer, deleteServer, listServers, updateServer } from '@/api/game';
import type { GameServer, ServerForm } from '@/api/game/types';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const rows = ref<GameServer[]>([]);
const loading = ref(false);
const visible = ref(false);
const formRef = ref<ElFormInstance>();
const empty = (): ServerForm => ({ serverCode: '', serverName: '', baseUrl: '', appId: '', secret: '', timeoutMs: 3000, enabled: true, remark: '' });
const data = reactive<{ editData: ServerForm }>({ editData: empty() });
const { editData } = toRefs(data);
const rules = {
  serverCode: [{ required: true, message: '请输入编码' }],
  serverName: [{ required: true, message: '请输入名称' }],
  baseUrl: [{ required: true, message: '请输入接口地址' }],
  appId: [{ required: true, message: '请输入 App ID' }]
};
const load = async () => {
  loading.value = true;
  try {
    rows.value = (await listServers()).data;
  } finally {
    loading.value = false;
  }
};
const open = (row?: GameServer) => {
  editData.value = { ...empty(), ...(row || {}) };
  visible.value = true;
};
const submit = async () =>
  formRef.value?.validate(async (ok) => {
    if (!ok) return;
    editData.value.id ? await updateServer(editData.value.id, editData.value) : await addServer(editData.value);
    proxy?.$modal.msgSuccess('保存成功');
    visible.value = false;
    await load();
  });
const health = async (row: GameServer) => {
  const res = await checkServer(row.serverCode);
  res.data.healthy ? proxy?.$modal.msgSuccess(res.data.message) : proxy?.$modal.msgError(res.data.message);
};
const remove = async (row: GameServer) => {
  await proxy?.$modal.confirm(`确认删除区服 ${row.serverName}？`);
  await deleteServer(row.id);
  await load();
};
onMounted(load);
</script>
