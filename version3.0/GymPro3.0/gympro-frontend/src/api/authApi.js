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
