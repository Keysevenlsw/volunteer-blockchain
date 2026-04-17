<template>
  <section class="workspace-top surface-card">
    <div class="workspace-main">
      <div class="workspace-copy">
        <span class="workspace-eyebrow">{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <div class="workspace-actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="metric-grid">
      <article v-for="item in stats" :key="item.label" class="metric-card soft-card">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
        <div class="metric-hint">{{ item.hint }}</div>
      </article>
    </div>
  </section>
</template>

<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: '工作台'
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  stats: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.workspace-top {
  padding: clamp(24px, 3vw, 34px);
  margin-bottom: 22px;
  background:
    radial-gradient(circle at top right, rgba(247, 201, 207, 0.3), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 247, 247, 0.92));
}

.workspace-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
  flex-wrap: wrap;
}

.workspace-copy {
  max-width: 760px;
}

.workspace-eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  color: var(--brand-500);
  background: rgba(233, 107, 118, 0.12);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.workspace-copy h1 {
  margin: 16px 0 0;
  font-size: clamp(30px, 3vw, 42px);
  color: var(--ink-900);
}

.workspace-copy p {
  margin: 12px 0 0;
  line-height: 1.85;
  color: var(--ink-500);
}

.workspace-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .workspace-top {
    padding: 20px;
  }
}
</style>
