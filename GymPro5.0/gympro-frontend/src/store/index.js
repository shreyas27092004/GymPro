// src/store/index.js
import { configureStore } from '@reduxjs/toolkit';
import bookingReducer      from './slices/bookingSlice';
import paymentReducer      from './slices/paymentSlice';
import planReducer         from './slices/planSlice';
import adminReducer        from './slices/adminSlice';
import notificationReducer from './slices/notificationSlice';   // ← NEW

export const store = configureStore({
  reducer: {
    bookings:      bookingReducer,
    payments:      paymentReducer,
    plans:         planReducer,
    admin:         adminReducer,
    notifications: notificationReducer,    // ← NEW
  },
});

export default store;