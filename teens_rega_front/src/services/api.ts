import axios from 'axios';

export const API_URL = '/api';
axios.defaults.baseURL = API_URL;

const api = axios.create({
  baseURL: API_URL,
  timeout: 30000,
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('adminToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('adminToken');
      window.location.href = '/admin/login';
    }
    
    if (error.response?.status === 413) {
      error.response.data = {
        error: 'Файл слишком большой. Максимальный размер: 10MB'
      };
    }
    return Promise.reject(error);
  }
);

export default api;