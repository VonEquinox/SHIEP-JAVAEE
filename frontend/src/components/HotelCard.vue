<script setup>
// 酒店卡片：列表态与全屏详情态共用一套 DOM（标题、位置、价格连续变形），
// 展开后加载房型（视图 v_hotel_room）与评价，并可直接下单 —— 需求文档 7.3/7.4。
// 平铺原则：每个房型的入住人登记行就地从该房型下方滚出，把后面的内容（含评论区）推走；
// 日期选择平铺在提交预订区内，不使用任何浮层。
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getHotelRooms } from '../api/hotel';
import { createOrder, hotelReviews } from '../api/order';
import { listGuests } from '../api/user';
import { auth } from '../store/auth';
import RoomTypeCard from './RoomTypeCard.vue';
import GuestPicker from './GuestPicker.vue';
import FlatDateRange from './FlatDateRange.vue';

const props = defineProps({
  hotel: Object,
  dates: Array,          // 搜索条选择的 [入住, 离店]，作为下单日期的默认值
  expanded: Boolean,
});
const emit = defineEmits(['open', 'close']);

const router = useRouter();
const cardEl = ref(null);
const rooms = ref([]);
const reviews = ref([]);
const guests = ref([]);
const counts = ref({});      // roomTypeId -> 预订间数
const assign = ref({});      // roomTypeId -> [{ guestIds: [] }, ...] 每间一行
const orderDates = ref(null);
const submitting = ref(false);

watch(() => props.expanded, async (val) => {
  if (!val) return;
  orderDates.value = props.dates ? [...props.dates] : null;
  counts.value = {};
  assign.value = {};
  [rooms.value, reviews.value] = await Promise.all([
    getHotelRooms(props.hotel.hotelId),
    hotelReviews(props.hotel.hotelId),
  ]);
  if (auth.token) guests.value = await listGuests();
});

/** 房型数量变化 → 该房型下方的入住人登记行同步增减（就地滚出/收回） */
function setCount(room, n) {
  counts.value[room.roomTypeId] = n;
  const rows = assign.value[room.roomTypeId] || [];
  while (rows.length < n) rows.push({ guestIds: [] });
  rows.length = n;
  assign.value[room.roomTypeId] = rows;
}

const allRows = computed(() =>
  rooms.value.flatMap((room) =>
    (assign.value[room.roomTypeId] || []).map((row) => ({ room, row })),
  ),
);
const totalRooms = computed(() => allRows.value.length);

async function refreshGuests() {
  guests.value = await listGuests();
}

async function submit() {
  if (!auth.token) {
    ElMessage.warning('请先登录');
    return router.push('/login');
  }
  if (!orderDates.value) return ElMessage.warning('请选择入住和离店日期');
  if (!totalRooms.value) return ElMessage.warning('请至少选择一间客房');
  if (allRows.value.some(({ row }) => !row.guestIds.length)) {
    return ElMessage.warning('每间房至少登记一名入住人');
  }

  submitting.value = true;
  try {
    const orderNo = await createOrder({
      hotelId: props.hotel.hotelId,
      checkInDate: orderDates.value[0],
      checkOutDate: orderDates.value[1],
      rooms: allRows.value.map(({ room, row }) => ({
        roomTypeId: room.roomTypeId,
        guestIds: row.guestIds,
      })),
    });
    ElMessage.success(`预订成功，订单号 ${orderNo}`);
    emit('close');
    router.push('/orders');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <article
    ref="cardEl"
    class="hotel-card flat-card"
    data-expand-card
    :data-expanded="expanded"
  >
    <!-- 整卡点击区（列表态） -->
    <button v-if="!expanded" class="open-btn" :aria-label="`查看${hotel.hotelName}`"
            @click="emit('open', cardEl)" />

    <div class="panel">
      <button v-if="expanded" class="back-btn" @click="emit('close')">← 返回列表</button>

      <!-- 摘要区：列表/详情复用同一 DOM，展开时字号连续放大 -->
      <header class="summary">
        <h2>{{ hotel.hotelName }}</h2>
        <div class="meta muted">
          {{ hotel.city }} · {{ hotel.district }} · {{ hotel.address }}
        </div>
        <div class="tags">
          <span class="pill">{{ '★'.repeat(hotel.star || 0) }}</span>
          <span class="pill">¥{{ hotel.minPrice }} 起</span>
          <span v-if="hotel.avgScore" class="pill">{{ hotel.avgScore }} 分</span>
        </div>
      </header>

      <!-- 详情区：仅展开时出现，自摘要下方“生长”出来 -->
      <div v-if="expanded" class="detail">
        <section>
          <h3>选择房型</h3>
          <div class="room-list">
            <!-- 每个房型一个块：登记行就地从该房型下方滚出，推走后续内容 -->
            <div v-for="r in rooms" :key="r.roomTypeId" class="room-block">
              <RoomTypeCard :room="r" selectable
                            :count="counts[r.roomTypeId] || 0"
                            @update:count="setCount(r, $event)" />
              <el-collapse-transition>
                <div v-if="(assign[r.roomTypeId] || []).length" class="assign-list">
                  <div v-for="(row, i) in assign[r.roomTypeId]" :key="i" class="assign-row">
                    <span class="pill">第 {{ i + 1 }} 间 · 登记入住人（最多 {{ r.capacity }} 人）</span>
                    <GuestPicker v-model="row.guestIds" :guests="guests" :max="r.capacity"
                                 @refresh="refreshGuests" />
                  </div>
                </div>
              </el-collapse-transition>
            </div>
          </div>
        </section>

        <!-- 提交预订：日期选择平铺在此处（点击向下展开月历，推走下方内容） -->
        <el-collapse-transition>
          <section v-if="totalRooms" class="order-section">
            <h3>提交预订</h3>
            <FlatDateRange v-model="orderDates" />
            <el-button type="primary" round size="large" :loading="submitting" @click="submit">
              提交预订（{{ totalRooms }} 间）
            </el-button>
          </section>
        </el-collapse-transition>

        <section>
          <h3>住客评价</h3>
          <el-empty v-if="!reviews.length" description="暂无评价" :image-size="60" />
          <div v-for="(r, i) in reviews" :key="i" class="review">
            <div class="review-head">
              <span>{{ r.username }}</span>
              <el-rate :model-value="r.score" disabled size="small" />
            </div>
            <p class="muted">{{ r.content }}</p>
          </div>
        </section>
      </div>
    </div>
  </article>
</template>

<style scoped>
.hotel-card {
  position: relative;
  min-height: 13rem;
  transition: border-radius var(--motion-duration) var(--motion-easing);
}

.hotel-card[data-expanded='true'] {
  border-radius: 0;
  border-left: none;
  border-right: none;
  min-height: 100svh;
  width: 100%;
}

.open-btn {
  appearance: none;
  position: absolute;
  inset: 0;
  z-index: 5;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  padding: 1.5rem;
}

.hotel-card[data-expanded='true'] .panel {
  max-width: 74rem;
  margin: 0 auto;
  padding: 1.5rem clamp(1.4rem, 5vw, 4rem) clamp(3rem, 7vw, 6rem);
}

.back-btn {
  align-self: flex-start;
  border: var(--flat-border);
  background: transparent;
  color: var(--md-sys-color-on-surface);
  border-radius: 999px;
  height: 2.4rem;
  padding: 0 1rem;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.summary h2 {
  margin: 0;
  font-size: clamp(1.15rem, 1.9vw, 1.7rem);
  line-height: 0.97;
  font-weight: 560;
  letter-spacing: -0.022em;
  transition: font-size var(--motion-duration) var(--motion-easing);
}

.hotel-card[data-expanded='true'] .summary h2 {
  font-size: clamp(1.6rem, 3.2vw, 2.6rem);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.detail {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  animation: rise var(--motion-duration) var(--motion-easing);
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail h3 {
  margin: 0 0 0.8rem;
  font-weight: 560;
}

.room-list {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  margin-top: 1rem;
}

/* 房型块：登记行从房型卡下方滚出 */
.assign-list {
  display: flex;
  flex-direction: column;
}

.assign-row {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 0.8rem 1.2rem;
  margin: 0.5rem 0 0 1.2rem;
  border-left: 2px solid var(--md-sys-color-primary-container);
}

.order-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  align-items: stretch;
  padding: 1.2rem;
  border-radius: 18px;
  border: var(--flat-border);
}

.order-section .el-button {
  align-self: flex-start;
}

.review {
  padding: 0.8rem 0;
  border-bottom: var(--flat-border);
}

.review-head {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  font-weight: 600;
}

.review p {
  margin: 0.4rem 0 0;
}
</style>
