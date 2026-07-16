// src/api/memberProfile.js
// Resolves the logged-in user's member profile by email.
// Strategy:
//   1. Return cached profile from sessionStorage (if valid for this email).
//   2. Try GET /members/by-email/{email} — works for existing members.
//   3. If not found (404), create profile via POST /members (new members).
//   4. Cache the result so subsequent calls are instant.

import { memberApi } from './api';

export async function resolveMemberProfile(email) {
  // 1. Return cached profile if still valid for this session/email
  const cached = sessionStorage.getItem('memberProfile');
  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      if (parsed?.email === email && parsed?.id) return parsed;
    } catch {}
  }

  // 2. Try to fetch existing profile by email (the reliable path)
  try {
    const res = await memberApi.getByEmail(email);
    const member = res.data;
    sessionStorage.setItem('memberProfile', JSON.stringify(member));
    return member;
  } catch (fetchErr) {
    // Only continue if it's a 404 (member doesn't exist yet)
    if (fetchErr.response?.status !== 404) {
      throw new Error(fetchErr.response?.data?.message || 'Could not load member profile.');
    }
  }

  // 3. Profile doesn't exist — create it (first login after registration)
  const storedName = sessionStorage.getItem('name');
  try {
    const res = await memberApi.create({
      name:   storedName || email.split('@')[0],
      email,
      status: 'ACTIVE',
    });
    const member = res.data;
    sessionStorage.setItem('memberProfile', JSON.stringify(member));
    return member;
  } catch (createErr) {
    // Race condition: another tab created it between our check and create — retry lookup
    try {
      const res2 = await memberApi.getByEmail(email);
      const member = res2.data;
      sessionStorage.setItem('memberProfile', JSON.stringify(member));
      return member;
    } catch {
      throw new Error(createErr.response?.data?.message || 'Could not set up member profile. Please try again.');
    }
  }
}
