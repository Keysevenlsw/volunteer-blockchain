<template>
  <div class="empty-state">
    <div class="empty-mark">{{ mark }}</div>
    <h4>{{ title }}</h4>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  mark: {
    type: String,
    default: '空'
  },
  title: {
    type: String,
    default: '暂无内容'
  },
  description: {
    type: String,
    default: '当前还没有可展示的数据。'
  }
})
</script>

<style scoped>
.empty-state {
  padding: 28px 18px;
  text-align: center;
  color: var(--ink-500);
}

.empty-mark {
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: var(--brand-500);
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(180deg, rgba(233, 107, 118, 0.14), rgba(247, 201, 207, 0.34));
}

.empty-state h4 {
  margin: 0;
  color: var(--ink-900);
  font-size: 16px;
}

.empty-state p {
  margin: 10px auto 0;
  max-width: 360px;
  line-height: 1.7;
}
</style>
