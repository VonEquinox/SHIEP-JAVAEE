<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { register } from '../api/user';

const router = useRouter();
const form = ref({ username: '', password: '', phone: '', idCard: '' });

async function submit() {
  const { username, password, phone, idCard } = form.value;
  if (!username || !password) return ElMessage.warning('请填写用户名和密码');
  if (!/^\d{11}$/.test(phone)) return ElMessage.warning('手机号必须为11位数字');
  if (idCard && idCard.length !== 18) return ElMessage.warning('身份证号必须为18位');

  await register(form.value);
  ElMessage.success('注册成功，请登录');
  router.push('/login');
}
</script>

<template>
  <div class="auth-page">
    <header class="page-hero">
      <h1>注册<span>易宿</span>账号</h1>
      <p>注册后可预订客房、累计积分，积分达标自动升级 VIP 享受折扣。</p>
    </header>

    <div class="flat-card auth-card">
      <el-input v-model="form.username" placeholder="用户名" size="large" />
      <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
      <el-input v-model="form.phone" placeholder="手机号（11位）" size="large" maxlength="11" />
      <el-input v-model="form.idCard" placeholder="身份证号（18位，选填）" size="large" maxlength="18" />
      <el-button type="primary" size="large" round @click="submit">注 册</el-button>
      <div class="muted hint">
        已有账号？
        <router-link to="/login">去登录</router-link>
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
