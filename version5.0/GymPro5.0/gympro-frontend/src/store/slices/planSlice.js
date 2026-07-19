// src/store/slices/planSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { planApi } from '../../api/api';

// ── Async Thunks ────────────────────────────────────────────────────────────

export const fetchPlans = createAsyncThunk(
  'plans/fetchPlans',
  async (_, { rejectWithValue }) => {
    try {
      const res = await planApi.getAll();
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to load plans');
    }
  }
);

export const fetchMySubscriptions = createAsyncThunk(
  'plans/fetchMySubscriptions',
  async (memberId, { rejectWithValue }) => {
    try {
      const res = await planApi.getMySubscriptions(memberId).catch(() => ({ data: [] }));
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to load subscriptions');
    }
  }
);

export const subscribeToPlan = createAsyncThunk(
  'plans/subscribeToPlan',
  async ({ memberId, memberEmail, planId }, { rejectWithValue }) => {
    try {
      const res = await planApi.subscribe(memberId, memberEmail, planId);
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Subscription failed');
    }
  }
);

export const cancelSubscription = createAsyncThunk(
  'plans/cancelSubscription',
  async (subId, { rejectWithValue }) => {
    try {
      const res = await planApi.cancelSubscription(subId);
      // Backend now returns a message like "Subscription cancelled ✅ — refund: ₹733.33"
      return { subId, message: res.data };
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to cancel subscription');
    }
  }
);

// ── Slice ───────────────────────────────────────────────────────────────────

const planSlice = createSlice({
  name: 'plans',
  initialState: {
    plans:           [],
    mySubscriptions: [],
    loading:         false,
    error:           null,
    cancelMessage:   null, // e.g. "Subscription cancelled ✅ — refund: ₹733.33"
  },
  reducers: {
    clearPlanError(state) {
      state.error = null;
    },
    clearCancelMessage(state) {
      state.cancelMessage = null;
    },
  },
  extraReducers: (builder) => {
    // fetchPlans
    builder
      .addCase(fetchPlans.pending, (state) => {
        state.loading = true;
        state.error   = null;
      })
      .addCase(fetchPlans.fulfilled, (state, action) => {
        state.loading = false;
        state.plans   = action.payload;
      })
      .addCase(fetchPlans.rejected, (state, action) => {
        state.loading = false;
        state.error   = action.payload;
      });

    // fetchMySubscriptions
    builder
      .addCase(fetchMySubscriptions.pending, (state) => {
        state.loading = true;
        state.error   = null;
      })
      .addCase(fetchMySubscriptions.fulfilled, (state, action) => {
        state.loading         = false;
        state.mySubscriptions = action.payload;
      })
      .addCase(fetchMySubscriptions.rejected, (state, action) => {
        state.loading = false;
        state.error   = action.payload;
      });

    // subscribeToPlan
    builder
      .addCase(subscribeToPlan.pending, (state) => {
        state.loading = true;
        state.error   = null;
      })
      .addCase(subscribeToPlan.fulfilled, (state, action) => {
        state.loading = false;
        state.mySubscriptions = [action.payload, ...state.mySubscriptions];
      })
      .addCase(subscribeToPlan.rejected, (state, action) => {
        state.loading = false;
        state.error   = action.payload;
      });

    // cancelSubscription — mark as CANCELLED in place, keep the refund message
    builder
      .addCase(cancelSubscription.fulfilled, (state, action) => {
        const { subId, message } = action.payload;
        const sub = state.mySubscriptions.find(s => s.id === subId);
        if (sub) sub.status = 'CANCELLED';
        state.cancelMessage = message;
      })
      .addCase(cancelSubscription.rejected, (state, action) => {
        state.error = action.payload;
      });
  },
});

export const { clearPlanError, clearCancelMessage } = planSlice.actions;
export default planSlice.reducer;