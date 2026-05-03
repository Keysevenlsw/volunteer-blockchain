<template>
  <div class="app-shell">
    <router-view />
    <GlobalFooter />
  </div>
</template>

<script setup>
import GlobalFooter from './components/GlobalFooter.vue'
</script>

<style>
#app {
  min-height: 100vh;
}

.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
