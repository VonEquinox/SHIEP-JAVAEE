<script setup>
// 平铺日期选择：点击后月历在文档流内向下展开，把下方内容推走（无浮层）。
// mode="range"：选入住/离店（默认，只允许今天及以后，双月历）；
// mode="single"：选单个日期（如出生日期，允许过去，单月历）。
import { computed, ref } from 'vue';

const props = defineProps({
  modelValue: { type: [Array, String], default: null }, // range: [in, out] | single: 'YYYY-MM-DD'
  mode: { type: String, default: 'range' },
  placeholder: { type: String, default: '选择入住与离店日期' },
});
const emit = defineEmits(['update:modelValue', 'change']);

const open = ref(false);
const viewMonth = ref(startOfMonth(new Date()));

const fmt = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

function startOfMonth(d) {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

const today = new Date();
today.setHours(0, 0, 0, 0);

const isRange = computed(() => props.mode === 'range');

const label = computed(() => {
  if (isRange.value) {
    return props.modelValue ? `${props.modelValue[0]} 入住 → ${props.modelValue[1]} 离店` : '';
  }
  return props.modelValue || '';
});

/** 单个月的日历格：前导空位 + 当月每天 */
function monthCells(base) {
  const first = startOfMonth(base);
  const cells = Array.from({ length: first.getDay() }, () => null);
  const days = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
  for (let i = 1; i <= days; i++) {
    cells.push(new Date(first.getFullYear(), first.getMonth(), i));
  }
  return cells;
}

const months = computed(() => {
  const list = [viewMonth.value];
  if (isRange.value) {
    list.push(new Date(viewMonth.value.getFullYear(), viewMonth.value.getMonth() + 1, 1));
  }
  return list.map((m) => ({
    title: `${m.getFullYear()}年${m.getMonth() + 1}月`,
    cells: monthCells(m),
  }));
});

const picking = ref(null); // range 模式：已点入住日、待点离店日

function stateOf(d) {
  if (!d) return {};
  const v = fmt(d);
  if (!isRange.value) {
    return { disabled: false, start: v === props.modelValue };
  }
  const [inD, outD] = props.modelValue || [];
  return {
    disabled: d < today,
    start: v === (picking.value ?? inD),
    end: !picking.value && v === outD,
    inRange: !picking.value && inD && outD && v > inD && v < outD,
  };
}

function pick(d) {
  if (!d) return;
  const v = fmt(d);
  if (!isRange.value) {
    emit('update:modelValue', v);
    emit('change', v);
    open.value = false;
    return;
  }
  if (d < today) return;
  if (!picking.value) {
    picking.value = v;
    emit('update:modelValue', null);
  } else if (v > picking.value) {
    const range = [picking.value, v];
    picking.value = null;
    emit('update:modelValue', range);
    emit('change', range);
    open.value = false;
  } else {
    picking.value = v; // 点了更早的日期：重新作为入住日
  }
}

function shiftMonth(n) {
  viewMonth.value = new Date(viewMonth.value.getFullYear(), viewMonth.value.getMonth() + n, 1);
}

function shiftYear(n) {
  viewMonth.value = new Date(viewMonth.value.getFullYear() + n, viewMonth.value.getMonth(), 1);
}
</script>

<template>
  <div class="flat-daterange" :class="{ open }">
    <button type="button" class="fd-field" @click="open = !open">
      <span :class="{ 'fd-placeholder': !label }">
        {{ label || (picking ? `${picking} 入住 → 请选离店日期` : placeholder) }}
      </span>
      <span class="fd-arrow" :class="{ up: open }">▾</span>
    </button>
    <el-collapse-transition>
      <div v-show="open" class="fd-panel">
        <div class="fd-nav">
          <div>
            <button v-if="!isRange" type="button" class="fd-nav-btn" @click="shiftYear(-1)">«</button>
            <button type="button" class="fd-nav-btn" @click="shiftMonth(-1)">←</button>
          </div>
          <div>
            <button type="button" class="fd-nav-btn" @click="shiftMonth(1)">→</button>
            <button v-if="!isRange" type="button" class="fd-nav-btn" @click="shiftYear(1)">»</button>
          </div>
        </div>
        <div class="fd-months" :class="{ single: !isRange }">
          <div v-for="m in months" :key="m.title" class="fd-month">
            <div class="fd-title">{{ m.title }}</div>
            <div class="fd-week">
              <span v-for="w in ['日', '一', '二', '三', '四', '五', '六']" :key="w">{{ w }}</span>
            </div>
            <div class="fd-grid">
              <button
                v-for="(d, i) in m.cells"
                :key="i"
                type="button"
                class="fd-day"
                :class="stateOf(d)"
                :disabled="!d || stateOf(d).disabled"
                @click="pick(d)"
              >
                {{ d ? d.getDate() : '' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<style scoped>
.flat-daterange {
  display: flex;
  flex-direction: column;
}

.fd-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  height: 2.4rem;
  padding: 0 0.9rem;
  border-radius: 999px;
  border: var(--flat-border);
  background: transparent;
  color: var(--md-sys-color-on-surface);
  font-size: 0.92rem;
  cursor: pointer;
  transition: border-radius var(--motion-duration) var(--motion-easing);
}

.open .fd-field {
  border-radius: 16px 16px 6px 6px;
}

.fd-placeholder {
  color: var(--md-sys-color-on-surface-variant);
}

.fd-arrow {
  font-size: 0.7rem;
  color: var(--md-sys-color-on-surface-variant);
  transition: transform var(--motion-duration) var(--motion-easing);
}

.fd-arrow.up {
  transform: rotate(180deg);
}

.fd-panel {
  margin-top: 0.35rem;
  padding: 0.8rem;
  border-radius: 6px 6px 16px 16px;
  border: var(--flat-border);
}

.fd-nav {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.3rem;
}

.fd-nav-btn {
  border: var(--flat-border);
  background: transparent;
  color: var(--md-sys-color-on-surface);
  border-radius: 999px;
  width: 2rem;
  height: 2rem;
  cursor: pointer;
  margin: 0 0.15rem;
}

.fd-months {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.2rem;
}

.fd-months.single {
  grid-template-columns: 1fr;
}

@media (max-width: 700px) {
  .fd-months {
    grid-template-columns: 1fr;
  }
}

.fd-title {
  text-align: center;
  font-weight: 600;
  font-size: 0.92rem;
  margin-bottom: 0.5rem;
}

.fd-week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 0.75rem;
  color: var(--md-sys-color-on-surface-variant);
  margin-bottom: 0.2rem;
}

.fd-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.fd-day {
  aspect-ratio: 1;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--md-sys-color-on-surface);
  font-size: 0.85rem;
  cursor: pointer;
  transition: background var(--motion-duration) var(--motion-easing);
}

.fd-day:hover:not(:disabled) {
  background: color-mix(in srgb, var(--md-sys-color-surface-variant) 55%, transparent);
}

.fd-day:disabled {
  color: color-mix(in srgb, var(--md-sys-color-on-surface-variant) 35%, transparent);
  cursor: default;
}

.fd-day.start,
.fd-day.end {
  background: var(--md-sys-color-primary);
  color: var(--md-sys-color-on-primary);
  font-weight: 600;
}

.fd-day.inRange {
  background: var(--md-sys-color-primary-container);
  color: var(--md-sys-color-on-primary-container);
  border-radius: 6px;
}
</style>
