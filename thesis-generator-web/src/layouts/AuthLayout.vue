<template>
  <div class="auth-layout">
    <div class="auth-card">
      <div class="auth-brand">
        <div class="brand-content">
          <div class="brand-icon">📄</div>
          <h1>论文生成系统</h1>
          <p class="brand-sub">让论文写作更高效</p>
          <div class="brand-features">
            <span>✓ 模板化结构管理</span>
            <span>✓ 在线富文本编辑</span>
            <span>✓ 一键导出 DOCX/PDF</span>
            <span>✓ 教师在线批阅</span>
          </div>
        </div>
      </div>
      <div class="auth-form">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(183, 110, 60, 0.10) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 50%, rgba(200, 145, 62, 0.08) 0%, transparent 60%),
    var(--el-bg-color-page);
}

.auth-card {
  width: 880px;
  min-height: 520px;
  display: flex;
  background: var(--el-fill-color-blank);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(62, 46, 31, 0.15);
  border: 1px solid var(--el-border-color-light);
}

.auth-brand {
  width: 38%;
  background: linear-gradient(145deg, #3e2e1f 0%, #5c4530 100%);
  color: #f5efe8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(circle at 30% 20%, rgba(255,255,255,0.06) 0%, transparent 50%),
      radial-gradient(circle at 70% 80%, rgba(200,145,62,0.10) 0%, transparent 50%);
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.brand-icon { font-size: 52px; margin-bottom: 16px; }

h1 { font-size: 24px; font-weight: 700; margin-bottom: 6px; letter-spacing: 1px; }

.brand-sub { font-size: 14px; opacity: 0.7; margin-bottom: 32px; }

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  opacity: 0.6;
  line-height: 1.8;
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
