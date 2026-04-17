<template>
  <span class="status-badge" :class="`status-${tone}`">
    {{ label }}
  </span>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    default: '未设置'
  },
  tone: {
    type: String,
    default: 'neutral'
  }
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 82px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  border: 1px solid transparent;
}

.status-primary {
  color: #a52a37;
  background: rgba(233, 107, 118, 0.14);
  border-color: rgba(233, 107, 118, 0.22);
}

.status-success {
  color: #2d8d61;
  background: rgba(71, 171, 120, 0.12);
  border-color: rgba(71, 171, 120, 0.2);
}

.status-warning {
  color: #b3771e;
  background: rgba(245, 182, 81, 0.14);
  border-color: rgba(245, 182, 81, 0.22);
}

.status-danger {
  color: #b63a4b;
  background: rgba(225, 87, 106, 0.13);
  border-color: rgba(225, 87, 106, 0.22);
}

.status-neutral {
  color: #7a6870;
  background: rgba(140, 117, 125, 0.12);
  border-color: rgba(140, 117, 125, 0.18);
}
</style>
