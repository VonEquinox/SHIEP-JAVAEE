import { reactive } from 'vue';

/** 登录态：token 与用户信息存 localStorage，刷新不丢 */
export const auth = reactive({
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null'),
});

export function login(token, user) {
  auth.token = token;
  auth.user = user;
  localStorage.setItem('token', token);
  localStorage.setItem('user', JSON.stringify(user));
}

export function logout() {
  auth.token = '';
  auth.user = null;
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}

export const isAdmin = () => auth.user?.role === '管理员';
