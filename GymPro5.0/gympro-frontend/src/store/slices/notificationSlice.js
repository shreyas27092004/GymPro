// src/store/slices/notificationSlice.js
// Redux slice for in-app notification state.
// Manages the notification list, unread count, and SSE connection lifecycle.

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/axiosInstance';

// ── Async thunks ───────────────────────────────────────────────────────────

/**
 * Fetch paginated notifications from the backend.
 * GET /notify/inapp/{userId}?page=0&size=20
 */
export const fetchNotifications = createAsyncThunk(
  'notifications/fetch',
  async ({ userId, page = 0, size = 20 }, { rejectWithValue }) => {
    try {
      const res = await api.get(`/notify/inapp/${userId}?page=${page}&size=${size}`);
      return res.data; // NotificationPageResponse
    } catch (err) {
      return rejectWithValue(err.response?.data || err.message);
    }
  }
);

/**
 * Fetch the lightweight unread badge count.
 * GET /notify/inapp/{userId}/unread-count
 */
export const fetchUnreadCount = createAsyncThunk(
  'notifications/fetchUnread',
  async (userId, { rejectWithValue }) => {
    try {
      const res = await api.get(`/notify/inapp/${userId}/unread-count`);
      return res.data.count ?? 0;
    } catch {
      return rejectWithValue(0);
    }
  }
);

/**
 * Mark a single notification as read.
 * PUT /notify/inapp/{notificationId}/read
 */
export const markNotificationRead = createAsyncThunk(
  'notifications/markRead',
  async (notificationId, { rejectWithValue }) => {
    try {
      await api.put(`/notify/inapp/${notificationId}/read`);
      return notificationId;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

/**
 * Mark all notifications as read.
 * PUT /notify/inapp/{userId}/read-all
 */
export const markAllRead = createAsyncThunk(
  'notifications/markAllRead',
  async (userId, { rejectWithValue }) => {
    try {
      await api.put(`/notify/inapp/${userId}/read-all`);
      return userId;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

/**
 * Delete a single notification.
 * DELETE /notify/inapp/{notificationId}/{userId}
 */
export const deleteNotification = createAsyncThunk(
  'notifications/delete',
  async ({ notificationId, userId }, { rejectWithValue }) => {
    try {
      await api.delete(`/notify/inapp/${notificationId}/${userId}`);
      return notificationId;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

/**
 * Clear all notifications.
 * DELETE /notify/inapp/{userId}/clear
 */
export const clearAllNotifications = createAsyncThunk(
  'notifications/clearAll',
  async (userId, { rejectWithValue }) => {
    try {
      await api.delete(`/notify/inapp/${userId}/clear`);
      return userId;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

// ── Slice ──────────────────────────────────────────────────────────────────

const notificationSlice = createSlice({
  name: 'notifications',
  initialState: {
    items: [],
    unreadCount: 0,
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    hasNext: false,
    loading: false,
    error: null,
    sseConnected: false,
  },
  reducers: {
    /** Called by the SSE listener when a new notification arrives in real-time. */
    addNotificationFromSSE(state, action) {
      const incoming = action.payload;
      // Deduplicate: don't add if already exists
      const exists = state.items.some(n => n.id === incoming.id);
      if (!exists) {
        state.items = [incoming, ...state.items]; // prepend — newest first
        state.totalElements += 1;
        if (!incoming.isRead) state.unreadCount += 1;
      }
    },

    /** Update unread count from SSE UNREAD_COUNT event. */
    setUnreadCountFromSSE(state, action) {
      state.unreadCount = action.payload;
    },

    setSseConnected(state, action) {
      state.sseConnected = action.payload;
    },

    clearError(state) {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    // ── fetchNotifications ───────────────────────────────────────────────
    builder
      .addCase(fetchNotifications.pending, (state) => {
        state.loading = true; state.error = null;
      })
      .addCase(fetchNotifications.fulfilled, (state, action) => {
        const data = action.payload;
        if (action.meta.arg.page === 0) {
          // First page — replace
          state.items = data.notifications || [];
        } else {
          // Load more — append (avoid duplicates)
          const existingIds = new Set(state.items.map(n => n.id));
          const newItems = (data.notifications || []).filter(n => !existingIds.has(n.id));
          state.items = [...state.items, ...newItems];
        }
        state.totalElements  = data.totalElements  ?? state.totalElements;
        state.totalPages     = data.totalPages     ?? state.totalPages;
        state.currentPage    = data.currentPage    ?? 0;
        state.hasNext        = data.hasNext        ?? false;
        state.unreadCount    = data.unreadCount    ?? state.unreadCount;
        state.loading = false;
      })
      .addCase(fetchNotifications.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });

    // ── fetchUnreadCount ─────────────────────────────────────────────────
    builder.addCase(fetchUnreadCount.fulfilled, (state, action) => {
      state.unreadCount = action.payload;
    });

    // ── markNotificationRead ─────────────────────────────────────────────
    builder.addCase(markNotificationRead.fulfilled, (state, action) => {
      const id = action.payload;
      const item = state.items.find(n => n.id === id);
      if (item && !item.isRead) {
        item.isRead = true;
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
    });

    // ── markAllRead ──────────────────────────────────────────────────────
    builder.addCase(markAllRead.fulfilled, (state) => {
      state.items.forEach(n => { n.isRead = true; });
      state.unreadCount = 0;
    });

    // ── deleteNotification ───────────────────────────────────────────────
    builder.addCase(deleteNotification.fulfilled, (state, action) => {
      const id = action.payload;
      const item = state.items.find(n => n.id === id);
      if (item && !item.isRead) state.unreadCount = Math.max(0, state.unreadCount - 1);
      state.items = state.items.filter(n => n.id !== id);
      state.totalElements = Math.max(0, state.totalElements - 1);
    });

    // ── clearAllNotifications ────────────────────────────────────────────
    builder.addCase(clearAllNotifications.fulfilled, (state) => {
      state.items = [];
      state.unreadCount = 0;
      state.totalElements = 0;
    });
  },
});

export const {
  addNotificationFromSSE,
  setUnreadCountFromSSE,
  setSseConnected,
  clearError,
} = notificationSlice.actions;

// ── Selectors ──────────────────────────────────────────────────────────────
export const selectNotifications  = (state) => state.notifications.items;
export const selectUnreadCount    = (state) => state.notifications.unreadCount;
export const selectNotifLoading   = (state) => state.notifications.loading;
export const selectNotifHasNext   = (state) => state.notifications.hasNext;
export const selectNotifPage      = (state) => state.notifications.currentPage;
export const selectSseConnected   = (state) => state.notifications.sseConnected;

export default notificationSlice.reducer;