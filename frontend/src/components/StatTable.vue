<script setup>
// 统计报表通用组件：上半部汇总表、下半部可展开的明细表（需求文档 3.6：明细+汇总同时展示）。
// 三张报表复用本组件，只是列定义与数据不同 —— 一种信息一个组件（7.4）。
import { ref } from 'vue';

defineProps({
  title: String,
  summary: Array,          // 汇总行
  summaryCols: Array,      // [{ prop, label }]
  detail: Array,           // 明细行（v_order_detail 展开）
});

const showDetail = ref(false);

const detailCols = [
  { prop: 'orderNo', label: '单号', width: 190 },
  { prop: 'hotelName', label: '酒店', width: 150 },
  { prop: 'city', label: '城市', width: 80 },
  { prop: 'typeName', label: '房型', width: 110 },
  { prop: 'checkInDate', label: '入住', width: 105 },
  { prop: 'checkOutDate', label: '离店', width: 105 },
  { prop: 'guestName', label: '入住人', width: 90 },
  { prop: 'guestAge', label: '年龄', width: 70 },
  { prop: 'occupation', label: '职业', width: 90 },
  { prop: 'payPrice', label: '实付', width: 90 },
];
</script>

<template>
  <section class="flat-card stat-block">
    <div class="stat-head">
      <h3>{{ title }} · 汇总</h3>
      <slot name="controls" />
    </div>

    <el-table :data="summary" style="width: 100%">
      <el-table-column v-for="c in summaryCols" :key="c.prop" v-bind="c" />
    </el-table>

    <button class="detail-toggle" @click="showDetail = !showDetail">
      {{ showDetail ? '收起明细 ↑' : `展开明细（${detail.length} 条）↓` }}
    </button>

    <el-collapse-transition>
      <el-table v-if="showDetail" :data="detail" style="width: 100%" max-height="420">
        <el-table-column v-for="c in detailCols" :key="c.prop" v-bind="c" />
      </el-table>
    </el-collapse-transition>
  </section>
</template>

<style scoped>
.stat-block {
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.stat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.stat-head h3 {
  margin: 0;
  font-weight: 560;
}

.detail-toggle {
  align-self: flex-start;
  border: var(--flat-border);
  background: transparent;
  color: var(--md-sys-color-on-surface-variant);
  border-radius: 999px;
  padding: 0.4rem 1rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: color var(--motion-duration) var(--motion-easing);
}

.detail-toggle:hover {
  color: var(--md-sys-color-primary);
}
</style>
