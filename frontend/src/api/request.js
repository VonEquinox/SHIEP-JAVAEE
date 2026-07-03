import axios from 'axios';
import { ElMessage } from 'element-plus';
import { auth, logout } from '../store/auth';

/** 统一 axios 实例：带 token、统一解包 Result、统一报错 */
const request = axios.create({ baseURL: '/api', timeout: 10000 });

request.interceptors.request.use((config) => {
  if (auth.token) {
    config.headers.Authorization = auth.token;
  }
  return config;
});

request.interceptors.response.use(
  (resp) => {
    const { code, msg, data } = resp.data;
    if (code !== 0) {
      ElMessage.error(msg);
      return Promise.reject(new Error(msg));
    }
    return data;
  },
  (error) => {
    if (error.response?.status === 401) {
      logout();
      ElMessage.warning('请先登录');
      window.location.hash = '#/login';
    } else {
      ElMessage.error('网络异常，请稍后重试');
    }
    return Promise.reject(error);
  },
);

export default request;
