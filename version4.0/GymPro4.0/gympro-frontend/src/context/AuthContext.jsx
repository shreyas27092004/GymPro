// src/context/AuthContext.jsx
// Uses sessionStorage — data is cleared when the browser tab/window is closed.
//
// FIX: userId is now decoded directly from the JWT payload (the "userId" claim
// embedded by JwtUtil.generateToken).  This avoids a separate /me round-trip
// and ensures NotificationBell has the numeric ID it needs for SSE + REST calls.

import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

// ── JWT decode helper ────────────────────────────────────────────────────────
//
// Decodes the payload section of a JWT without verifying the signature.
// Signature verification happens on the server; the frontend only needs the
// claims for display / routing purposes.
//
// Returns the parsed payload object, or null if the token is missing/malformed.
function decodeJwtPayload(token) {
  if (!token) return null;
  try {
    // A JWT is three Base64URL segments separated by "."
    // Index 1 is the payload.
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    // Base64URL → Base64 → JSON
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join('')
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

// ── Provider ─────────────────────────────────────────────────────────────────

export function AuthProvider({ children }) {
  const [token,  setToken]  = useState(() => sessionStorage.getItem('token'));
  const [role,   setRole]   = useState(() => sessionStorage.getItem('role'));
  const [email,  setEmail]  = useState(() => sessionStorage.getItem('email'));
  const [name,   setName]   = useState(() => sessionStorage.getItem('name'));

  // userId is read from sessionStorage on mount (persisted as a string).
  // We keep it as a string here to stay consistent with sessionStorage;
  // components that need a number can call Number(userId).
  const [userId, setUserId] = useState(() => sessionStorage.getItem('userId'));

  /**
   * Called by Login.jsx after a successful /auth/login response.
   *
   * The login API returns: { token, name, email, role, message }
   * It does NOT return a numeric userId in the response body, but the JWT
   * now embeds it as the "userId" claim (added in JwtUtil.generateToken).
   *
   * We decode the payload here so:
   *   1. No extra /me or /members/by-email API call is needed.
   *   2. userId is available synchronously before any component mounts.
   *   3. If the backend ever adds other claims (e.g. "gymId"), they're also
   *      available via decodeJwtPayload without touching this context.
   *
   * Signature of login() is unchanged from before — the rawUserId param is
   * still accepted for callers that already know the id (e.g. tests), but in
   * normal use Login.jsx passes null and we fall back to the JWT claim.
   */
  const login = (token, role, email, rawUserId = null, name) => {
    // Decode the JWT to extract the numeric userId claim.
    const payload = decodeJwtPayload(token);
    // payload.userId is the Long claim set by JwtUtil; fall back to rawUserId
    // (legacy path) or null if neither is present.
    const resolvedUserId =
      (payload?.userId != null ? String(payload.userId) : null) ??
      (rawUserId       != null ? String(rawUserId)       : null);

    // Persist everything
    sessionStorage.setItem('token', token);
    sessionStorage.setItem('role',  role);
    if (email)          sessionStorage.setItem('email',  email);
    if (name)           sessionStorage.setItem('name',   name);
    if (resolvedUserId) sessionStorage.setItem('userId', resolvedUserId);

    setToken(token);
    setRole(role);
    setEmail(email);
    setName(name);
    setUserId(resolvedUserId);
  };

  const logout = () => {
    sessionStorage.clear();
    setToken(null);
    setRole(null);
    setEmail(null);
    setUserId(null);
    setName(null);
  };

  return (
    <AuthContext.Provider value={{ token, role, email, userId, name, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
export { AuthContext };