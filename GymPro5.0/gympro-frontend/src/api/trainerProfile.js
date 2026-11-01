// src/api/trainerProfile.js
// Resolves the logged-in trainer's profile by email.
// Strategy:
//   1. Return cached profile from sessionStorage (if valid for this email).
//   2. Try GET /trainers/by-email/{email} — works for existing trainers.
//   3. If not found (404), create profile via POST /trainers (new trainers).
//   4. Cache the result so subsequent calls are instant.

import { trainerApi } from './api';

export async function resolveTrainerProfile(email) {
  // 1. Return cached profile if still valid for this session/email
  const cached = sessionStorage.getItem('trainerProfile');
  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      if (parsed?.email === email && parsed?.id) return parsed;
    } catch {}
  }

  // 2. Try to fetch existing profile by email (the reliable path)
  try {
    const res = await trainerApi.getByEmail(email);
    const trainer = res.data;
    sessionStorage.setItem('trainerProfile', JSON.stringify(trainer));
    return trainer;
  } catch (fetchErr) {
    // Only continue if it's a 404 (trainer doesn't exist yet)
    if (fetchErr.response?.status !== 404) {
      throw new Error(fetchErr.response?.data?.message || 'Could not load trainer profile.');
    }
  }

  // 3. Profile doesn't exist — create it (fallback if backend auto-create failed)
  const storedName = sessionStorage.getItem('name');
  try {
    const res = await trainerApi.create({
      name:             storedName || email.split('@')[0],
      email,
      phone:            '',
      specialization:   'General',
      experienceYears:  0,
      status:           'ACTIVE',
    });
    const trainer = res.data;
    sessionStorage.setItem('trainerProfile', JSON.stringify(trainer));
    return trainer;
  } catch (createErr) {
    // Race condition: retry lookup
    try {
      const res2 = await trainerApi.getByEmail(email);
      const trainer = res2.data;
      sessionStorage.setItem('trainerProfile', JSON.stringify(trainer));
      return trainer;
    } catch {
      throw new Error(createErr.response?.data?.message || 'Could not set up trainer profile. Please try again.');
    }
  }
}
