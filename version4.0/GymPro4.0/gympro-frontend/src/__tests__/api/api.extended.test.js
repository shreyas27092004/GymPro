// Extended API tests — additional endpoint coverage for trainerApi, memberApi,
// planApi, and paymentApi not fully covered in api.test.js

import * as apiModule from '../../api/api';

jest.mock('../../api/api', () => {
  const mockCall = jest.fn(() => Promise.resolve({ data: {} }));
  const mockApi = {
    get:    mockCall,
    post:   mockCall,
    put:    mockCall,
    delete: mockCall,
  };

  return {
    authApi: {
      login:          (d)       => mockApi.post('/auth/login', d),
      register:       (d)       => mockApi.post('/auth/register', d),
      forgotPassword: (d)       => mockApi.post('/auth/forgot-password', d),
      verifyOtp:      (d)       => mockApi.post('/auth/verify-otp', d),
      resetPassword:  (d)       => mockApi.post('/auth/reset-password', d),
    },
    memberApi: {
      getAll:     ()            => mockApi.get('/members'),
      getById:    (id)          => mockApi.get(`/members/${id}`),
      getByEmail: (email)       => mockApi.get(`/members/by-email/${encodeURIComponent(email)}`),
      create:     (data)        => mockApi.post('/members', data),
      update:     (id, data)    => mockApi.put(`/members/${id}`, data),
      delete:     (id)          => mockApi.delete(`/members/${id}`),
    },
    trainerApi: {
      getAll:            ()      => mockApi.get('/trainers'),
      getById:           (id)    => mockApi.get(`/trainers/${id}`),
      getAvailableSlots: (id)    => mockApi.get(`/trainers/${id}/available-slots`),
      create:            (data)  => mockApi.post('/trainers', data),
      update:            (id, d) => mockApi.put(`/trainers/${id}`, d),
      delete:            (id)    => mockApi.delete(`/trainers/${id}`),
    },
    bookingApi: {
      create:       (data) => mockApi.post('/bookings/create', data),
      getByMember:  (id)   => mockApi.get(`/bookings/member/${id}`),
      getByTrainer: (id)   => mockApi.get(`/bookings/trainer/${id}`),
      cancel:       (id)   => mockApi.post(`/bookings/cancel/${id}`),
      complete:     (id)   => mockApi.post(`/bookings/complete/${id}`),
    },
    planApi: {
      getAll:             ()                    => mockApi.get('/plans'),
      getById:            (id)                  => mockApi.get(`/plans/${id}`),
      create:             (data)                => mockApi.post('/plans', data),
      update:             (id, data)            => mockApi.put(`/plans/${id}`, data),
      deactivate:         (id)                  => mockApi.delete(`/plans/${id}`),
      subscribe:          (mId, mEmail, planId) => mockApi.post(`/plans/subscribe?memberId=${mId}&memberEmail=${encodeURIComponent(mEmail)}&planId=${planId}`),
      getMySubscriptions: (mId)                 => mockApi.get(`/plans/my/${mId}`),
      cancelSubscription: (subId)               => mockApi.delete(`/plans/subscription/${subId}`),
      useSession:         (mId)                 => mockApi.post(`/plans/use-session/${mId}`),
    },
    paymentApi: {
      getAll:        ()          => mockApi.get('/payments/all'),
      getMyPayments: (mId)       => mockApi.get(`/payments/my/${mId}`),
      pay:           (data)      => mockApi.post('/payments/pay', data),
      createOrder:   (amt, desc) => mockApi.post(`/payments/create-order?amount=${amt}&description=${encodeURIComponent(desc)}`),
      refund:        (id)        => mockApi.post(`/payments/refund/${id}`),
    },
    _mockCall: mockCall,
  };
});

beforeEach(() => jest.clearAllMocks());

// ── authApi — extended ─────────────────────────────────────────────────────────
describe('authApi — extended', () => {
  test('verifyOtp calls /auth/verify-otp', async () => {
    const payload = { email: 'user@gym.com', otp: '123456' };
    await apiModule.authApi.verifyOtp(payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/auth/verify-otp', payload);
  });

  test('resetPassword calls /auth/reset-password', async () => {
    const payload = { email: 'user@gym.com', otp: '123456', newPassword: 'newpass' };
    await apiModule.authApi.resetPassword(payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/auth/reset-password', payload);
  });
});

// ── memberApi — extended ───────────────────────────────────────────────────────
describe('memberApi — extended', () => {
  test('getByEmail encodes email in the URL', async () => {
    await apiModule.memberApi.getByEmail('user+test@gym.com');
    expect(apiModule._mockCall).toHaveBeenCalledWith(
      `/members/by-email/${encodeURIComponent('user+test@gym.com')}`
    );
  });

  test('create calls POST /members with payload', async () => {
    const payload = { name: 'New Guy', email: 'new@gym.com', role: 'MEMBER' };
    await apiModule.memberApi.create(payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/members', payload);
  });

  test('getById with numeric id builds correct path', async () => {
    await apiModule.memberApi.getById(42);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/members/42');
  });
});

// ── trainerApi — full coverage ─────────────────────────────────────────────────
describe('trainerApi', () => {
  test('getAll calls GET /trainers', async () => {
    await apiModule.trainerApi.getAll();
    expect(apiModule._mockCall).toHaveBeenCalledWith('/trainers');
  });

  test('getById calls GET /trainers/:id', async () => {
    await apiModule.trainerApi.getById(7);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/trainers/7');
  });

  test('getAvailableSlots calls GET /trainers/:id/available-slots', async () => {
    await apiModule.trainerApi.getAvailableSlots(3);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/trainers/3/available-slots');
  });

  test('create calls POST /trainers', async () => {
    const payload = { name: 'New Trainer', specialization: 'Yoga' };
    await apiModule.trainerApi.create(payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/trainers', payload);
  });

  test('update calls PUT /trainers/:id', async () => {
    const payload = { name: 'Updated Trainer' };
    await apiModule.trainerApi.update(5, payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/trainers/5', payload);
  });

  test('delete calls DELETE /trainers/:id', async () => {
    await apiModule.trainerApi.delete(9);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/trainers/9');
  });
});

// ── bookingApi — extended ──────────────────────────────────────────────────────
describe('bookingApi — extended', () => {
  test('getByMember calls correct endpoint', async () => {
    await apiModule.bookingApi.getByMember(12);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/bookings/member/12');
  });

  test('getByTrainer calls correct endpoint', async () => {
    await apiModule.bookingApi.getByTrainer(8);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/bookings/trainer/8');
  });

  test('complete calls /bookings/complete/:id', async () => {
    await apiModule.bookingApi.complete(33);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/bookings/complete/33');
  });
});

// ── planApi — extended ─────────────────────────────────────────────────────────
describe('planApi — extended', () => {
  test('getAll calls GET /plans', async () => {
    await apiModule.planApi.getAll();
    expect(apiModule._mockCall).toHaveBeenCalledWith('/plans');
  });

  test('getById calls GET /plans/:id', async () => {
    await apiModule.planApi.getById(2);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/plans/2');
  });

  test('create calls POST /plans', async () => {
    const payload = { planName: 'Gold', price: 3000, durationDays: 90 };
    await apiModule.planApi.create(payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/plans', payload);
  });

  test('update calls PUT /plans/:id', async () => {
    const payload = { price: 3500 };
    await apiModule.planApi.update(4, payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/plans/4', payload);
  });

  test('deactivate calls DELETE /plans/:id', async () => {
    await apiModule.planApi.deactivate(6);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/plans/6');
  });

  test('getMySubscriptions calls GET /plans/my/:memberId', async () => {
    await apiModule.planApi.getMySubscriptions(3);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/plans/my/3');
  });

  test('cancelSubscription calls DELETE /plans/subscription/:subId', async () => {
    await apiModule.planApi.cancelSubscription(77);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/plans/subscription/77');
  });

  test('subscribe encodes email with special characters', async () => {
    await apiModule.planApi.subscribe(1, 'user+tag@gym.com', 5);
    expect(apiModule._mockCall).toHaveBeenCalledWith(
      `/plans/subscribe?memberId=1&memberEmail=${encodeURIComponent('user+tag@gym.com')}&planId=5`
    );
  });
});

// ── paymentApi — extended ──────────────────────────────────────────────────────
describe('paymentApi — extended', () => {
  test('getAll calls GET /payments/all', async () => {
    await apiModule.paymentApi.getAll();
    expect(apiModule._mockCall).toHaveBeenCalledWith('/payments/all');
  });

  test('getMyPayments calls GET /payments/my/:memberId', async () => {
    await apiModule.paymentApi.getMyPayments(5);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/payments/my/5');
  });

  test('pay calls POST /payments/pay with full payload', async () => {
    const payload = {
      memberId: 1, memberEmail: 'user@gym.com',
      amount: 999, paymentMethod: 'UPI',
      description: 'Basic Plan Subscription',
    };
    await apiModule.paymentApi.pay(payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/payments/pay', payload);
  });

  test('createOrder encodes description with unicode characters', async () => {
    await apiModule.paymentApi.createOrder(500, 'Session – Mon 10:00');
    expect(apiModule._mockCall).toHaveBeenCalledWith(
      `/payments/create-order?amount=500&description=${encodeURIComponent('Session – Mon 10:00')}`
    );
  });

  test('refund calls POST /payments/refund/:id', async () => {
    await apiModule.paymentApi.refund(22);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/payments/refund/22');
  });
});
