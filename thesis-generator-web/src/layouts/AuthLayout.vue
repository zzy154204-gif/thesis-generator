<template>
  <div class="auth-layout">
    <div class="auth-card">
      <div class="auth-brand">
        <div class="brand-content">
          <div class="brand-icon">📄</div>
          <h1>论文生成系统</h1>
          <p class="brand-desc">让论文写作更高效</p>
          <div class="brand-features">
            <span>✓ 模板化管理</span>
            <span>✓ 在线编辑</span>
            <span>✓ 一键导出</span>
          </div>
        </div>
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
  background:
    radial-gradient(ellipse at 20% 50%, rgba(183, 110, 60, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 50%, rgba(200, 145, 62, 0.06) 0%, transparent 60%),
    var(--el-bg-color-page);
}

.auth-card {
  width: 880px;
  min-height: 500px;
  display: flex;
  background: #fdfbf8;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 40px rgba(62, 46, 31, 0.12);
  border: 1px solid var(--el-border-color-light);
}

.auth-brand {
  width: 38%;
  background: linear-gradient(145deg, #3e2e1f 0%, #5c4530 100%);
  color: #f5efe8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.05) 0%, transparent 50%),
      radial-gradient(circle at 70% 80%, rgba(200, 145, 62, 0.08) 0%, transparent 50%);
  }
}

.brand-content {
  position: relative;
  text-align: center;
  z-index: 1;
}

.brand-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.brand-desc {
  font-size: 14px;
  opacity: 0.7;
  margin-bottom: 28px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  opacity: 0.6;
}

.auth-form {
  width: 62%;
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
