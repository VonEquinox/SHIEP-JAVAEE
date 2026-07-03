<script setup>
// 平铺下拉：点击后面板在文档流内向下展开，把下方内容推走（不浮在页面上）。
// 全站替代 el-select 的浮层行为 —— 需求文档 7.2/7.3 平铺原则。
import { computed, ref } from 'vue';

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] }, // 字符串数组或 {label, value}
  placeholder: String,
  disabled: Boolean,
});
const emit = defineEmits(['update:modelValue', 'change']);

const open = ref(false);
const items = computed(() =>
  props.options.map((o) => (typeof o === 'object' ? o : { label: String(o), value: o })),
);
const selectedLabel = computed(
  () => items.value.find((o) => o.value === props.modelValue)?.label ?? '',
);

function pick(value) {
  emit('update:modelValue', value);
  emit('change', value);
  open.value = false;
}
</script>

<template>
  <div class="flat-select" :class="{ open, disabled }">
    <button type="button" class="fs-field" :disabled="disabled" @click="open = !open">
      <span :class="{ 'fs-placeholder': !selectedLabel }">{{ selectedLabel || placeholder }}</span>
      <span class="fs-arrow" :class="{ up: open }">▾</span>
    </button>
    <el-collapse-transition>
      <div v-show="open" class="fs-panel">
        <button v-if="selectedLabel" type="button" class="fs-option fs-clear" @click="pick('')">
          清除选择
        </button>
        <button
          v-for="o in items"
          :key="o.value"
          type="button"
          class="fs-option"
          :class="{ active: o.value === modelValue }"
          @click="pick(o.value)"
        >
          {{ o.label }}
        </button>
      </div>
    </el-collapse-transition>
  </div>
</template>

<style scoped>
.flat-select {
  display: flex;
  flex-direction: column;
}

.fs-field {
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

.disabled .fs-field {
  opacity: 0.45;
  cursor: not-allowed;
}

.open .fs-field {
  border-radius: 16px 16px 6px 6px;
}

.fs-placeholder {
  color: var(--md-sys-color-on-surface-variant);
}

.fs-arrow {
  font-size: 0.7rem;
  color: var(--md-sys-color-on-surface-variant);
  transition: transform var(--motion-duration) var(--motion-easing);
}

.fs-arrow.up {
  transform: rotate(180deg);
}

.fs-panel {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 0.35rem;
  padding: 0.35rem;
  border-radius: 6px 6px 16px 16px;
  border: var(--flat-border);
  max-height: 15rem;
  overflow-y: auto;
}

.fs-option {
  text-align: left;
  padding: 0.45rem 0.7rem;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--md-sys-color-on-surface);
  font-size: 0.9rem;
  cursor: pointer;
  transition: background var(--motion-duration) var(--motion-easing);
}

.fs-option:hover {
  background: color-mix(in srgb, var(--md-sys-color-surface-variant) 45%, transparent);
}

.fs-option.active {
  background: var(--md-sys-color-primary-container);
  color: var(--md-sys-color-on-primary-container);
  font-weight: 600;
}

.fs-clear {
  color: var(--md-sys-color-on-surface-variant);
  font-size: 0.82rem;
}
</style>
