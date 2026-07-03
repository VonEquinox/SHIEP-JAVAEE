<script setup>
// 房型卡片：酒店详情选房用；数量选择由父组件通过 v-model:count 控制
defineProps({
  room: Object,
  count: { type: Number, default: 0 },
  selectable: { type: Boolean, default: false },
});
defineEmits(['update:count']);
</script>

<template>
  <div class="room-card">
    <div class="info">
      <h4>{{ room.typeName }}</h4>
      <div class="meta muted">
        <span class="pill">可住 {{ room.capacity }} 人</span>
        <span class="pill">剩 {{ room.availableCount }} 间</span>
      </div>
    </div>
    <div class="right">
      <div class="price">¥{{ room.price }}<span class="muted">/晚</span></div>
      <el-input-number
        v-if="selectable"
        :model-value="count"
        :min="0"
        :max="room.availableCount"
        size="small"
        @update:model-value="$emit('update:count', $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.room-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.2rem;
  border-radius: 18px;
  border: var(--flat-border);
}

.info h4 {
  margin: 0 0 0.5rem;
  font-size: 1.05rem;
  font-weight: 560;
}

.meta {
  display: flex;
  gap: 0.5rem;
}

.right {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-end;
}

.price {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--md-sys-color-primary);
}

.price span {
  font-size: 0.8rem;
  font-weight: 400;
}
</style>
