import axios from 'axios';
import { logout, getCurrentUser } from './authService';

const API_URL = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_URL,
});

api.interceptors.request.use((config) => {
  const user = getCurrentUser();
  if (user && user.token) {
    config.headers.Authorization = `Bearer ${user.token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      logout();
      if (!window.location.pathname.startsWith('/acceso')) {
        window.location.href = '/acceso';
      }
    }
    return Promise.reject(error);
  }
);

export default api;