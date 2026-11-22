import axios from 'axios';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300000,
  headers: { Accept: 'application/json' },
});

export function errorMessage(error) {
  if (error?.response?.data) {
    const body = error.response.data;
    if (typeof body === 'string') return body;
    if (body.message) return body.message;
    if (body.error) return body.error;
  }
  if (error?.code === 'ECONNABORTED') return 'The request took too long. Please try again.';
  if (error?.request) return `Cannot reach the ReviewLens API at ${API_BASE_URL}. Check that the backend is running.`;
  return error?.message || 'Something went wrong. Please try again.';
}

export const reviewApi = {
  getDashboard: (filters = {}) => api.get('/api/reviews/dashboard', { params: cleanFilters(filters) }),
  getReviews: (params = {}) => api.get('/api/reviews', { params: cleanFilters(params) }),
  getReview: (reviewId) => api.get(`/api/reviews/${encodeURIComponent(reviewId)}`),
  suggestReply: (reviewId) => api.post(`/api/reviews/${encodeURIComponent(reviewId)}/suggested-reply`),
  uploadCsv: (file, onUploadProgress) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/api/reviews/upload', form, { onUploadProgress });
  },
};

function cleanFilters(filters) {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '' && value != null));
}
