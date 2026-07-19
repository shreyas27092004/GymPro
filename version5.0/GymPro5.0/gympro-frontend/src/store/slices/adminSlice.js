// src/store/slices/adminSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { memberApi, bookingApi, paymentApi } from '../../api/api';

// ── Async Thunks ────────────────────────────────────────────────────────────

export const fetchAllMembers = createAsyncThunk(
  'admin/fetchAllMembers',
  async (_, { rejectWithValue }) => {
    try {
      const res = await memberApi.getAll();
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to load members');
    }
  }
);

/**
 * Admin has no single "get all bookings" endpoint — we fan out across
 * every member and deduplicate by booking id (same strategy as the
 * original AdminBookings component).
 */
export const fetchAllBookings = createAsyncThunk(
  'admin/fetchAllBookings',
  async (_, { rejectWithValue }) => {
    try {
      const mRes  = await memberApi.getAll();
      const allBookings = [];
      await Promise.all(
        mRes.data.map(async (m) => {
          try {
            const r = await bookingApi.getByMember(m.id);
            allBookings.push(...r.data);
          } catch {
            // skip members with no bookings / access errors
          }
        })
      );
      const unique = [...new Map(allBookings.map(b => [b.id, b])).values()];
      return unique.sort((a, b) => b.id - a.id);
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to load bookings');
    }
  }
);

export const fetchAllPayments = createAsyncThunk(
  'admin/fetchAllPayments',
  async (_, { rejectWithValue }) => {
    try {
      const res = await paymentApi.getAll();
      return [...res.data].sort((a, b) => b.id - a.id);
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to load payments');
    }
  }
);

// ── Slice ───────────────────────────────────────────────────────────────────

const adminSlice = createSlice({
  name: 'admin',
  initialState: {
    members:  [],
    bookings: [],
    payments: [],
    loading:  false,
    error:    null,
  },
  reducers: {
    clearAdminError(state) {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    const pending   = (state)          => { state.loading = true;  state.error = null; };
    const rejected  = (state, action)  => { state.loading = false; state.error = action.payload; };

    // fetchAllMembers
    builder
      .addCase(fetchAllMembers.pending,   pending)
      .addCase(fetchAllMembers.fulfilled, (state, action) => {
        state.loading = false;
        state.members = action.payload;
      })
      .addCase(fetchAllMembers.rejected,  rejected);

    // fetchAllBookings
    builder
      .addCase(fetchAllBookings.pending,   pending)
      .addCase(fetchAllBookings.fulfilled, (state, action) => {
        state.loading  = false;
        state.bookings = action.payload;
      })
      .addCase(fetchAllBookings.rejected,  rejected);

    // fetchAllPayments
    builder
      .addCase(fetchAllPayments.pending,   pending)
      .addCase(fetchAllPayments.fulfilled, (state, action) => {
        state.loading  = false;
        state.payments = action.payload;
      })
      .addCase(fetchAllPayments.rejected,  rejected);
  },
});

export const { clearAdminError } = adminSlice.actions;
export default adminSlice.reducer;