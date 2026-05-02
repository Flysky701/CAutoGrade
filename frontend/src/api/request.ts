import axios from 'axios';
import { ElMessage } from 'element-plus';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    // Only add token if it's a valid non-placeholder value
    if (token && token !== 'undefined' && token !== 'null' && token.length > 10) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => {
    const data = response.data;
    if (data && typeof data === 'object' && 'code' in data && data.code !== 200) {
      if (!response.config?._silent) {
        ElMessage.error(data.msg || '请求失败');
      }
      return Promise.reject(new Error(data.msg || '请求失败'));
    }
    return data;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      window.location.href = '/login';
      return new Promise(() => {});
    }
    if (!error.config?._silent) {
      const msg = error.response?.data?.msg || error.message || '请求失败';
      ElMessage.error(msg);
    }
    return Promise.reject(error);
  }
);

export default api;
