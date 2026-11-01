// src/api/notificationApi.js
// Notification-specific API methods.
// These are thin wrappers around axiosInstance for use in non-Redux contexts.
// Redux callers should use notificationSlice thunks instead.

import api from './axiosInstance';

export const notificationApi = {
  // ── REST ──────────────────────────────────────────────────────────────────
  /** GET paginated notifications */
  getPage: (userId, page = 0, size = 20) =>
    api.get(`/notify/inapp/${userId}?page=${page}&size=${size}`),

  /** GET all notifications (no pagination) */
  getAll: (userId) =>
    api.get(`/notify/inapp/${userId}/all`),

  /** GET unread badge count */
  getUnreadCount: (userId) =>
    api.get(`/notify/inapp/${userId}/unread-count`),

  /** PUT mark one read */
  markRead: (notificationId) =>
    api.put(`/notify/inapp/${notificationId}/read`),

  /** PUT mark all read */
  markAllRead: (userId) =>
    api.put(`/notify/inapp/${userId}/read-all`),

  /** DELETE one notification */
  delete: (notificationId, userId) =>
    api.delete(`/notify/inapp/${notificationId}/${userId}`),

  /** DELETE all notifications */
  clearAll: (userId) =>
    api.delete(`/notify/inapp/${userId}/clear`),

  // ── SSE stream URL (use with EventSource, not axios) ──────────────────────
  /** Returns the SSE stream URL for a user. */
  streamUrl: (userId) => {
    const token = sessionStorage.getItem('token');
    return `/api/notify/inapp/${userId}/stream${token ? `?token=${token}` : ''}`;
  },
};