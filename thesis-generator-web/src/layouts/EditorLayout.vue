<template>
  <div class="editor-layout">
    <slot />
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.editor-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>
