<script setup>
// 后台：用户列表（含积分与等级）
import { onMounted, ref } from 'vue';
import { listUsers } from '../../api/user';

const users = ref([]);

onMounted(async () => {
  users.value = await listUsers();
});
</script>

<template>
  <div class="page">
    <h2>用户管理</h2>
    <el-table :data="users" style="width: 100%">
      <el-table-column prop="userId" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="points" label="积分" width="90" />
      <el-table-column label="等级" width="90">
        <template #default="{ row }">
          <span class="pill">{{ row.level }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="90" />
      <el-table-column prop="createdAt" label="注册时间" min-width="160" />
    </el-table>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.page h2 {
  margin: 0;
  font-weight: 560;
  letter-spacing: -0.02em;
}
</style>
