// src/store/slices/bookingSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { bookingApi } from '../../api/api';

// ── Async Thunks ────────────────────────────────────────────────────────────

export const fetchMemberBookings = createAsyncThunk(
  'bookings/fetchMemberBookings',
  async (memberId, { rejectWithValue }) => {
    try {
      const res = await bookingApi.getByMember(memberId);
      return [...res.data].sort((a, b) => b.id - a.id);
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to load bookings');
    }
  }
);

/**
 * createBooking — the backend (booking-service) now handles ALL free-session logic.
 *
 * The BookingResponse shape is:
 * {
 *   success:               boolean,
 *   paymentRequired:       boolean,   // false = free session, true = must pay
 *   freeSessionUsed:       boolean,   // true = this booking used a free session
 *   remainingFreeSessions: number,    // sessions left after this booking (-1 = unlimited)
 *   amount:                number,    // trainer fee (only set when paymentRequired=true)
 *   booking:               Booking    // the saved booking record
 * }
 *
 * No secondary plan-service call is made here — all decisions are already
 * baked into the BookingResponse by the backend. This prevents any possibility
 * of the frontend bypassing payment by making a separate free-session call.
 */
export const createBooking = createAsyncThunk(
  'bookings/createBooking',
  async (data, { rejectWithValue }) => {
    try {
      const res = await bookingApi.create(data);
      // BookingResponse is returned directly from booking-service
      const bookingResponse = res.data;
      return {
        booking:               bookingResponse.booking,
        paymentRequired:       bookingResponse.paymentRequired,
        freeSessionUsed:       bookingResponse.freeSessionUsed,
        remainingFreeSessions: bookingResponse.remainingFreeSessions,
        amount:                bookingResponse.amount,
      };
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Booking failed');
    }
  }
);

export const cancelBooking = createAsyncThunk(
  'bookings/cancelBooking',
  async (id, { rejectWithValue }) => {
    try {
      await bookingApi.cancel(id);
      return id;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Cancel failed');
    }
  }
);

// ── Slice ───────────────────────────────────────────────────────────────────

const bookingSlice = createSlice({
  name: 'bookings',
  initialState: {
    bookings:          [],
    loading:           false,
    error:             null,
    lastBookingResult: null, // { booking, paymentRequired, freeSessionUsed, remainingFreeSessions, amount }
  },
  reducers: {
    clearBookingError(state) {
      state.error = null;
    },
    clearLastBookingResult(state) {
      state.lastBookingResult = null;
    },
  },
  extraReducers: (builder) => {
    // fetchMemberBookings
    builder
      .addCase(fetchMemberBookings.pending,   (state)         => { state.loading = true; state.error = null; })
      .addCase(fetchMemberBookings.fulfilled, (state, action) => { state.loading = false; state.bookings = action.payload; })
      .addCase(fetchMemberBookings.rejected,  (state, action) => { state.loading = false; state.error = action.payload; });

    // createBooking
    builder
      .addCase(createBooking.pending,   (state)         => { state.loading = true; state.error = null; state.lastBookingResult = null; })
      .addCase(createBooking.fulfilled, (state, action) => { state.loading = false; state.lastBookingResult = action.payload; })
      .addCase(createBooking.rejected,  (state, action) => { state.loading = false; state.error = action.payload; });

    // cancelBooking — update status in-place
    builder
      .addCase(cancelBooking.pending,   (state)         => { state.error = null; })
      .addCase(cancelBooking.fulfilled, (state, action) => {
        const booking = state.bookings.find(b => b.id === action.payload);
        if (booking) booking.status = 'CANCELLED';
      })
      .addCase(cancelBooking.rejected,  (state, action) => { state.error = action.payload; });
  },
});

export const { clearBookingError, clearLastBookingResult } = bookingSlice.actions;
export default bookingSlice.reducer;
