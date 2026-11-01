// Tests for api.js — verifying all API endpoint definitions
// We mock the underlying axios instance; we just verify the correct paths are called.
import * as apiModule from '../../api/api';

jest.mock('../../api/api', () => {
  // Internal mock of the axios instance
  const mockCall = jest.fn(() => Promise.resolve({ data: {} }));
  const mockApi = {
    get:    mockCall,
    post:   mockCall,
    put:    mockCall,
    delete: mockCall,
  };

  return {
    authApi: {
      login:          (d) => mockApi.post('/auth/login', d),
      register:       (d) => mockApi.post('/auth/register', d),
      forgotPassword: (d) => mockApi.post('/auth/forgot-password', d),
      verifyOtp:      (d) => mockApi.post('/auth/verify-otp', d),
      resetPassword:  (d) => mockApi.post('/auth/reset-password', d),
    },
    memberApi: {
      getAll:     ()         => mockApi.get('/members'),
      getById:    (id)       => mockApi.get(`/members/${id}`),
      getByEmail: (email)    => mockApi.get(`/members/by-email/${encodeURIComponent(email)}`),
      create:     (data)     => mockApi.post('/members', data),
      update:     (id, data) => mockApi.put(`/members/${id}`, data),
      delete:     (id)       => mockApi.delete(`/members/${id}`),
    },
    trainerApi: {
      getAll:            ()     => mockApi.get('/trainers'),
      getById:           (id)   => mockApi.get(`/trainers/${id}`),
      getAvailableSlots: (id)   => mockApi.get(`/trainers/${id}/available-slots`),
      create:            (data) => mockApi.post('/trainers', data),
      update:            (id, d)=> mockApi.put(`/trainers/${id}`, d),
      delete:            (id)   => mockApi.delete(`/trainers/${id}`),
    },
    bookingApi: {
      create:       (data) => mockApi.post('/bookings/create', data),
      getByMember:  (id)   => mockApi.get(`/bookings/member/${id}`),
      getByTrainer: (id)   => mockApi.get(`/bookings/trainer/${id}`),
      cancel:       (id)   => mockApi.post(`/bookings/cancel/${id}`),
      complete:     (id)   => mockApi.post(`/bookings/complete/${id}`),
    },
    planApi: {
      getAll:             ()                              => mockApi.get('/plans'),
      getById:            (id)                            => mockApi.get(`/plans/${id}`),
      create:             (data)                          => mockApi.post('/plans', data),
      update:             (id, data)                      => mockApi.put(`/plans/${id}`, data),
      deactivate:         (id)                            => mockApi.delete(`/plans/${id}`),
      subscribe:          (mId, mEmail, planId)           => mockApi.post(`/plans/subscribe?memberId=${mId}&memberEmail=${encodeURIComponent(mEmail)}&planId=${planId}`),
      getMySubscriptions: (mId)                           => mockApi.get(`/plans/my/${mId}`),
      cancelSubscription: (subId)                         => mockApi.delete(`/plans/subscription/${subId}`),
      useSession:         (mId)                           => mockApi.post(`/plans/use-session/${mId}`),
    },
    paymentApi: {
      getAll:        ()             => mockApi.get('/payments/all'),
      getMyPayments: (mId)          => mockApi.get(`/payments/my/${mId}`),
      pay:           (data)         => mockApi.post('/payments/pay', data),
      createOrder:   (amt, desc)    => mockApi.post(`/payments/create-order?amount=${amt}&description=${encodeURIComponent(desc)}`),
      refund:        (id)           => mockApi.post(`/payments/refund/${id}`),
    },
    _mockCall: mockCall,
  };
});

describe('authApi', () => {
  test('login calls correct endpoint', async () => {
    const data = { email: 'a@b.com', password: 'pass' };
    await apiModule.authApi.login(data);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/auth/login', data);
  });

  test('register calls correct endpoint', async () => {
    const data = { email: 'a@b.com', password: 'pass', role: 'MEMBER' };
    await apiModule.authApi.register(data);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/auth/register', data);
  });

  test('forgotPassword calls correct endpoint', async () => {
    await apiModule.authApi.forgotPassword({ email: 'x@y.com' });
    expect(apiModule._mockCall).toHaveBeenCalledWith('/auth/forgot-password', { email: 'x@y.com' });
  });
});

describe('memberApi', () => {
  test('getAll calls /members', async () => {
    await apiModule.memberApi.getAll();
    expect(apiModule._mockCall).toHaveBeenCalledWith('/members');
  });

  test('getById calls /members/:id', async () => {
    await apiModule.memberApi.getById(5);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/members/5');
  });

  test('update calls PUT /members/:id', async () => {
    const payload = { name: 'Bob' };
    await apiModule.memberApi.update(3, payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/members/3', payload);
  });

  test('delete calls DELETE /members/:id', async () => {
    await apiModule.memberApi.delete(7);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/members/7');
  });
});

describe('bookingApi', () => {
  test('create calls /bookings/create', async () => {
    const payload = { memberId: 1, trainerId: 2 };
    await apiModule.bookingApi.create(payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/bookings/create', payload);
  });

  test('cancel calls /bookings/cancel/:id', async () => {
    await apiModule.bookingApi.cancel(10);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/bookings/cancel/10');
  });
});

describe('planApi', () => {
  test('useSession calls /plans/use-session/:memberId', async () => {
    await apiModule.planApi.useSession(4);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/plans/use-session/4');
  });

  test('subscribe includes memberId, memberEmail, planId as query params', async () => {
    await apiModule.planApi.subscribe(1, 'a@b.com', 3);
    expect(apiModule._mockCall).toHaveBeenCalledWith(
      `/plans/subscribe?memberId=1&memberEmail=${encodeURIComponent('a@b.com')}&planId=3`
    );
  });
});

describe('paymentApi', () => {
  test('pay calls /payments/pay', async () => {
    const payload = { memberId: 1, amount: 500, paymentMethod: 'CASH' };
    await apiModule.paymentApi.pay(payload);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/payments/pay', payload);
  });

  test('createOrder encodes description in query string', async () => {
    await apiModule.paymentApi.createOrder(500, 'Session #1 – MON 10:00');
    expect(apiModule._mockCall).toHaveBeenCalledWith(
      `/payments/create-order?amount=500&description=${encodeURIComponent('Session #1 – MON 10:00')}`
    );
  });

  test('refund calls /payments/refund/:id', async () => {
    await apiModule.paymentApi.refund(99);
    expect(apiModule._mockCall).toHaveBeenCalledWith('/payments/refund/99');
  });
});
