<script setup>
// 订单状态胶囊：一种信息一个组件（需求文档 7.4）
defineProps({ status: String });

const colorOf = {
  已预订: 'var(--md-sys-color-primary)',
  已入住: 'var(--md-sys-color-secondary)',
  已完成: 'var(--md-sys-color-on-surface-variant)',
  已退订: 'var(--md-sys-color-outline)',
};
</script>

<template>
  <span class="pill" :style="{ color: colorOf[status] }">{{ status }}</span>
</template>
