<template>
  <div class="auth-layout">
    <div class="auth-card">
      <div class="auth-brand">
        <h1>论文生成系统</h1>
        <p>让论文写作更高效</p>
      </div>
      <div class="auth-form">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409EFF 0%, #337ECC 100%);
}

.auth-card {
  width: 960px;
  min-height: 520px;
  display: flex;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.auth-brand {
  width: 40%;
  background: linear-gradient(135deg, #409EFF 0%, #2366B5 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;

  h1 { font-size: 28px; font-weight: 700; margin-bottom: 12px; }
  p { font-size: 16px; opacity: 0.85; }
}

.auth-form {
  width: 60%;
  padding: 48px 56px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .auth-brand { display: none; }
  .auth-form { width: 100%; padding: 32px 24px; }
  .auth-card { width: 100%; min-height: 100vh; border-radius: 0; }
}
</style>
