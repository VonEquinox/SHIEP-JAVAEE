<script setup>
// 后台：订单管理与状态流转（办理入住/完成离店，完成时后端自动加积分）
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { adminOrders, updateOrderStatus } from '../../api/order';
import StatusTag from '../../components/StatusTag.vue';

const status = ref('');
const orders = ref([]);

async function load() {
  orders.value = await adminOrders(status.value || undefined);
}

async function flow(order, next) {
  await updateOrderStatus(order.orderId, next);
  ElMessage.success(`已${next === '已入住' ? '办理入住' : '完成离店（积分已发放）'}`);
  await load();
}

onMounted(load);
</script>

<template>
  <div class="page">
    <div class="head">
      <h2>订单管理</h2>
      <el-radio-group v-model="status" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button v-for="s in ['已预订', '已入住', '已完成', '已退订']" :key="s" :value="s">
          {{ s }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="orders" style="width: 100%">
      <el-table-column prop="orderNo" label="单号" width="200" />
      <el-table-column prop="username" label="用户" width="100" />
      <el-table-column prop="hotelName" label="酒店" min-width="150" />
      <el-table-column prop="checkInDate" label="入住" width="110" />
      <el-table-column prop="checkOutDate" label="离店" width="110" />
      <el-table-column prop="payPrice" label="实付" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button v-if="row.status === '已预订'" text type="primary" size="small"
                     @click="flow(row, '已入住')">办理入住</el-button>
          <el-button v-if="row.status === '已入住'" text type="primary" size="small"
                     @click="flow(row, '已完成')">完成离店</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.head h2 {
  margin: 0;
  font-weight: 560;
  letter-spacing: -0.02em;
}
</style>
