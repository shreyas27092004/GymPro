// src/api/api.js  — unified API layer for all GymPro services
// Uses sessionStorage for token/role (cleared on tab close).

import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token');
  const role  = sessionStorage.getItem('role');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  if (role)  config.headers['X-User-Role'] = role;
  return config;
}, (err) => Promise.reject(err));

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      sessionStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// ── AUTH ───────────────────────────────────────────────────────────────────
export const authApi = {
  login:          (data) => api.post('/auth/login', data),
  register:       (data) => api.post('/auth/register', data),
  forgotPassword: (data) => api.post('/auth/forgot-password', data),
  verifyOtp:      (data) => api.post('/auth/verify-otp', data),
  resetPassword:  (data) => api.post('/auth/reset-password', data),
};

// ── MEMBERS ───────────────────────────────────────────────────────────────
export const memberApi = {
  getAll:      ()         => api.get('/members'),
  getById:     (id)       => api.get(`/members/${id}`),
  getByEmail:  (email)    => api.get(`/members/by-email/${encodeURIComponent(email)}`),
  create:      (data)     => api.post('/members', data),
  update:      (id, data) => api.put(`/members/${id}`, data),
  delete:      (id)       => api.delete(`/members/${id}`),
};

// ── TRAINERS ───────────────────────────────────────────────────────────────
export const trainerApi = {
  getAll:            ()           => api.get('/trainers'),
  getById:           (id)         => api.get(`/trainers/${id}`),
  getByEmail:        (email)      => api.get(`/trainers/by-email/${encodeURIComponent(email)}`),
  create:            (data)       => api.post('/trainers', data),
  update:            (id, data)   => api.put(`/trainers/${id}`, data),
  delete:            (id)         => api.delete(`/trainers/${id}`),
  getSchedule:       (id)         => api.get(`/trainers/${id}/schedule`),
  getAvailableSlots: (id)         => api.get(`/trainers/${id}/available-slots`),
  addSchedule:       (data)       => api.post('/trainers/schedule', data),
  markSlotBooked:    (scheduleId) => api.put(`/trainers/schedule/${scheduleId}/book`),
  deleteSchedule:    (scheduleId) => api.delete(`/trainers/schedule/${scheduleId}`),
  updateSessionFee:  (id, fee)    => api.put(`/trainers/${id}/session-fee`, { sessionFee: fee }),
};

// ── BOOKINGS ───────────────────────────────────────────────────────────────
export const bookingApi = {
  create:       (data)      => api.post('/bookings/create', data),
  getById:      (id)        => api.get(`/bookings/${id}`),
  getByMember:  (memberId)  => api.get(`/bookings/member/${memberId}`),
  getByTrainer: (trainerId) => api.get(`/bookings/trainer/${trainerId}`),
  cancel:       (id)        => api.post(`/bookings/cancel/${id}`),
  complete:     (id)        => api.post(`/bookings/complete/${id}`),
};

// ── PLANS ──────────────────────────────────────────────────────────────────
export const planApi = {
  getAll:             ()                              => api.get('/plans'),
  getById:            (id)                            => api.get(`/plans/${id}`),
  create:             (data)                          => api.post('/plans', data),
  update:             (id, data)                      => api.put(`/plans/${id}`, data),
  deactivate:         (id)                            => api.delete(`/plans/${id}`),
  subscribe:          (memberId, memberEmail, planId) => api.post(`/plans/subscribe?memberId=${memberId}&memberEmail=${encodeURIComponent(memberEmail)}&planId=${planId}`),
  getMySubscriptions: (memberId)                      => api.get(`/plans/my/${memberId}`),
  cancelSubscription: (subId)                         => api.delete(`/plans/subscription/${subId}`),
  useSession:         (memberId)                      => api.post(`/plans/use-session/${memberId}`),
  // Returns { paymentRequired, freeSessionUsed, remainingFreeSessions, amount }
  checkFreeSession:   (memberId)                      => api.get(`/plans/free-session-check/${memberId}`),
};

// ── PAYMENTS ───────────────────────────────────────────────────────────────
export const paymentApi = {
  getAll:        ()             => api.get('/payments/all'),
  getMyPayments: (memberId)     => api.get(`/payments/my/${memberId}`),
  getById:       (id)           => api.get(`/payments/${id}`),
  getByBooking:  (bookingId)    => api.get(`/payments/booking/${bookingId}`),
  pay:           (data)         => api.post('/payments/pay', data),
  createOrder:   (amount, desc) => api.post(`/payments/create-order?amount=${amount}&description=${encodeURIComponent(desc)}`),
  refund:        (id)           => api.post(`/payments/refund/${id}`),
};

export default api;
