<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login as apiLogin } from '../api/user';
import { login as saveAuth } from '../store/auth';

const router = useRouter();
const form = ref({ username: '', password: '' });
const loading = ref(false);

async function submit() {
  if (!form.value.username || !form.value.password) return ElMessage.warning('请输入用户名和密码');
  loading.value = true;
  try {
    const { token, user } = await apiLogin(form.value);
    saveAuth(token, user);
    ElMessage.success(`欢迎回来，${user.username}`);
    router.push(user.role === '管理员' ? '/admin' : '/');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <header class="page-hero">
      <h1>欢迎来到<span>易宿</span></h1>
      <p>登录后即可预订全国各地的酒店客房。</p>
    </header>

    <div class="flat-card auth-card">
      <el-input v-model="form.username" placeholder="用户名" size="large" @keyup.enter="submit" />
      <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                show-password @keyup.enter="submit" />
      <el-button type="primary" size="large" round :loading="loading" @click="submit">登 录</el-button>
      <div class="muted hint">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  flex-direction: column;
  gap: 2.3rem;
  max-width: 26rem;
  margin: 3rem auto 0;
}

.auth-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 2rem;
}

.hint {
  text-align: center;
  font-size: 0.9rem;
}

.hint a {
  color: var(--md-sys-color-primary);
}
</style>
