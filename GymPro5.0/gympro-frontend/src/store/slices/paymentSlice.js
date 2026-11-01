// src/store/slices/paymentSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { paymentApi } from '../../api/api';

// ── Async Thunks ────────────────────────────────────────────────────────────

export const fetchMyPayments = createAsyncThunk(
  'payments/fetchMyPayments',
  async (memberId, { rejectWithValue }) => {
    try {
      const res = await paymentApi.getMyPayments(memberId);
      return [...res.data].sort((a, b) => b.id - a.id);
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to load payment history');
    }
  }
);

export const processPayment = createAsyncThunk(
  'payments/processPayment',
  async (data, { rejectWithValue }) => {
    try {
      const res = await paymentApi.pay(data);
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Payment failed');
    }
  }
);

// ── Slice ───────────────────────────────────────────────────────────────────

const paymentSlice = createSlice({
  name: 'payments',
  initialState: {
    payments: [],
    loading:  false,
    error:    null,
  },
  reducers: {
    clearPaymentError(state) {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    // fetchMyPayments
    builder
      .addCase(fetchMyPayments.pending, (state) => {
        state.loading = true;
        state.error   = null;
      })
      .addCase(fetchMyPayments.fulfilled, (state, action) => {
        state.loading  = false;
        state.payments = action.payload;
      })
      .addCase(fetchMyPayments.rejected, (state, action) => {
        state.loading = false;
        state.error   = action.payload;
      });

    // processPayment
    builder
      .addCase(processPayment.pending, (state) => {
        state.loading = true;
        state.error   = null;
      })
      .addCase(processPayment.fulfilled, (state, action) => {
        state.loading  = false;
        state.payments = [action.payload, ...state.payments];
      })
      .addCase(processPayment.rejected, (state, action) => {
        state.loading = false;
        state.error   = action.payload;
      });
  },
});

export const { clearPaymentError } = paymentSlice.actions;
export default paymentSlice.reducer;
