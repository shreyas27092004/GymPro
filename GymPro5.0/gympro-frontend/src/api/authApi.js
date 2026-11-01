// src/api/authApi.js
// Calls GymPro auth-service via API Gateway

import api from './axiosInstance';

/**
 * Register a new user
 * POST /auth/register
 * Body: { name, email, password, role }
 * role: "ADMIN" | "TRAINER" | "MEMBER"
 */
export const registerUser = (data) => api.post('/auth/register', data);

/**
 * Login user
 * POST /auth/login
 * Body: { email, password }
 * Response: { token, role }
 */
export const loginUser = (data) => api.post('/auth/login', data);

/**
 * Verify a pending ADMIN registration
 * POST /auth/verify-admin-registration
 * Body: { email, otp }
 * Only needed when register() returns verificationRequired: true
 * (i.e. role was ADMIN and at least one admin already existed).
 */
export const verifyAdminRegistration = (data) => api.post('/auth/verify-admin-registration', data);
