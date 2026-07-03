<script setup>
// 我的订单：按状态分类查询、详情就地展开、退订、评价。
// 状态切换时只滑动下半部分（订单列表）：状态按钮左右有序，
// 选更右侧的状态 → 列表向左滑出、新列表自右滑入；反之相反。
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { addReview, cancelOrder, myOrders, orderDetail } from '../api/order';
import StatusTag from '../components/StatusTag.vue';

const STATUSES = ['', '已预订', '已入住', '已完成', '已退订'];

const status = ref('');
const orders = ref([]);
const slideName = ref('list-left');
const expanded = ref(null);      // 当前展开详情的 orderId
const detail = ref(null);
const reviewForm = ref({ score: 5, content: '' });
const reviewing = ref(null);     // 正在评价的 orderId

async function load() {
  orders.value = await myOrders(status.value || undefined);
}

/** 切换状态：按左右关系决定列表滑动方向，数据到位后再触发过渡 */
async function switchStatus(next) {
  if (next === status.value) return;
  slideName.value = STATUSES.indexOf(next) > STATUSES.indexOf(status.value)
    ? 'list-left'
    : 'list-right';
  status.value = next;
  expanded.value = null;
  await load();
}

async function toggleDetail(orderId) {
  if (expanded.value === orderId) {
    expanded.value = null;
    return;
  }
  detail.value = await orderDetail(orderId);
  expanded.value = orderId;
}

async function onCancel(order) {
  await ElMessageBox.confirm(`确定退订「${order.hotelName}」的订单吗？房间将立即释放。`, '退订确认');
  await cancelOrder(order.orderId);
  ElMessage.success('已退订');
  await load();
}

async function submitReview(orderId) {
  await addReview({ orderId, ...reviewForm.value });
  ElMessage.success('评价成功');
  reviewing.value = null;
  reviewForm.value = { score: 5, content: '' };
}

onMounted(load);
</script>

<template>
  <div class="orders-page">
    <header class="page-hero">
      <h1>我的<span>订单</span></h1>
      <p>可按状态分类查看；未入住的订单可以退订，已完成的订单可以评价。</p>
    </header>

    <div class="status-tabs">
      <button
        v-for="s in STATUSES"
        :key="s"
        type="button"
        class="status-tab"
        :class="{ active: status === s }"
        @click="switchStatus(s)"
      >
        {{ s || '全部' }}
      </button>
    </div>

    <!-- 下半部分：列表整体左右滑动，key 跟随状态 -->
    <div class="list-stage">
      <transition :name="slideName">
        <div :key="status" class="order-list">
          <article v-for="o in orders" :key="o.orderId" class="flat-card order-card">
            <div class="order-head" @click="toggleDetail(o.orderId)">
              <div>
                <h3>{{ o.hotelName }}</h3>
                <div class="muted">
                  {{ o.city }} · {{ o.checkInDate }} 至 {{ o.checkOutDate }} · 单号 {{ o.orderNo }}
                </div>
              </div>
              <div class="order-right">
                <div class="price">
                  ¥{{ o.payPrice }}
                  <span v-if="o.payPrice !== o.totalPrice" class="muted original">¥{{ o.totalPrice }}</span>
                </div>
                <StatusTag :status="o.status" />
              </div>
            </div>

            <!-- 详情：从卡片内展开（有迹可循），含每间房入住人 -->
            <el-collapse-transition>
              <div v-if="expanded === o.orderId && detail" class="order-detail">
                <div v-for="item in detail.items" :key="item.itemId" class="room-row">
                  <span class="pill">{{ item.typeName }} ¥{{ item.roomPrice }}/晚</span>
                  <span class="muted">
                    入住人：{{ item.guests.map((g) => `${g.name}（${g.gender}）`).join('、') }}
                  </span>
                </div>

                <div class="actions">
                  <el-button v-if="o.status === '已预订'" round @click="onCancel(o)">退 订</el-button>
                  <el-button v-if="o.status === '已完成'" round type="primary"
                             @click="reviewing = reviewing === o.orderId ? null : o.orderId">
                    写评价
                  </el-button>
                </div>

                <el-collapse-transition>
                  <div v-if="reviewing === o.orderId" class="review-form">
                    <el-rate v-model="reviewForm.score" />
                    <el-input v-model="reviewForm.content" type="textarea" :rows="3"
                              maxlength="500" show-word-limit placeholder="住得怎么样？" />
                    <el-button type="primary" round @click="submitReview(o.orderId)">提交评价</el-button>
                  </div>
                </el-collapse-transition>
              </div>
            </el-collapse-transition>
          </article>

          <el-empty v-if="!orders.length" description="暂无订单" />
        </div>
      </transition>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  display: flex;
  flex-direction: column;
  gap: 1.8rem;
  overflow-x: clip;
}

/* 状态页签：胶囊组，与整体平铺风格一致 */
.status-tabs {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.status-tab {
  border: var(--flat-border);
  background: transparent;
  color: var(--md-sys-color-on-surface-variant);
  border-radius: 999px;
  height: 2.3rem;
  padding: 0 1.1rem;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  transition:
    background var(--motion-duration) var(--motion-easing),
    color var(--motion-duration) var(--motion-easing);
}

.status-tab.active {
  background: var(--md-sys-color-primary-container);
  color: var(--md-sys-color-on-primary-container);
}

/* 转场舞台：离场列表绝对定位叠在原位，与入场列表同时滑动 */
.list-stage {
  position: relative;
  min-height: 12rem;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.list-left-enter-active,
.list-left-leave-active,
.list-right-enter-active,
.list-right-leave-active {
  transition: transform 0.45s var(--motion-easing), opacity 0.45s var(--motion-easing);
}

.list-left-leave-active,
.list-right-leave-active {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

.list-left-enter-from {
  transform: translateX(calc(100% + 4rem));
}

.list-left-leave-to {
  transform: translateX(calc(-100% - 4rem));
}

.list-right-enter-from {
  transform: translateX(calc(-100% - 4rem));
}

.list-right-leave-to {
  transform: translateX(calc(100% + 4rem));
}

.order-card {
  padding: 1.3rem 1.5rem;
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  cursor: pointer;
}

.order-head h3 {
  margin: 0 0 0.4rem;
  font-weight: 560;
}

.order-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.price {
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--md-sys-color-primary);
}

.original {
  font-size: 0.85rem;
  text-decoration: line-through;
  margin-left: 0.3rem;
}

.order-detail {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: var(--flat-border);
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.room-row {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.actions {
  display: flex;
  gap: 0.8rem;
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  align-items: flex-start;
}
</style>
