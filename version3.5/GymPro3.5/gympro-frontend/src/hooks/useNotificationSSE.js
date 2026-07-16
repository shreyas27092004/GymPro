// src/hooks/useNotificationSSE.js
// Custom hook that establishes and manages an SSE connection to the notification service.
// Auto-reconnects on drop. Dispatches Redux actions on incoming events.
//
// Usage: Call once at the App or dashboard level (not per-component).
//   const sseStatus = useNotificationSSE(userId);

import { useEffect, useRef, useCallback } from 'react';
import { useDispatch } from 'react-redux';
import {
  addNotificationFromSSE,
  setUnreadCountFromSSE,
  setSseConnected,
} from '../store/slices/notificationSlice';
import { useToast } from '../components/Toast';

const SSE_BASE_URL     = '/api/notify/inapp';
const RECONNECT_DELAY  = 3000;   // ms before reconnect attempt
const MAX_RECONNECTS   = 10;     // give up after this many consecutive failures

export function useNotificationSSE(userId) {
  const dispatch        = useDispatch();
  const { showToast }   = useToast();
  const esRef           = useRef(null);
  const reconnectCount  = useRef(0);
  const reconnectTimer  = useRef(null);
  const mountedRef      = useRef(true);

  const connect = useCallback(() => {
    if (!userId || !mountedRef.current) return;

    // Append JWT token as query param because EventSource doesn't support headers
    const token = sessionStorage.getItem('token');
    const url   = `${SSE_BASE_URL}/${userId}/stream${token ? `?token=${token}` : ''}`;

    const es = new EventSource(url);
    esRef.current = es;

    es.onopen = () => {
      reconnectCount.current = 0;
      dispatch(setSseConnected(true));
    };

    // ── NEW_NOTIFICATION event ──────────────────────────────────────────
    es.addEventListener('NEW_NOTIFICATION', (e) => {
      try {
        const notification = JSON.parse(e.data);
        dispatch(addNotificationFromSSE(notification));

        // Show toast popup for new notification
        showToast(`${notification.title}: ${notification.message}`, 'info');
      } catch (err) {
        console.warn('[SSE] Failed to parse NEW_NOTIFICATION:', err);
      }
    });

    // ── UNREAD_COUNT event ──────────────────────────────────────────────
    es.addEventListener('UNREAD_COUNT', (e) => {
      try {
        const data = JSON.parse(e.data);
        dispatch(setUnreadCountFromSSE(data.count ?? 0));
      } catch (err) {
        console.warn('[SSE] Failed to parse UNREAD_COUNT:', err);
      }
    });

    es.onerror = () => {
      es.close();
      dispatch(setSseConnected(false));

      if (!mountedRef.current) return;
      if (reconnectCount.current >= MAX_RECONNECTS) {
        console.warn('[SSE] Max reconnect attempts reached — giving up.');
        return;
      }

      reconnectCount.current++;
      const delay = RECONNECT_DELAY * Math.min(reconnectCount.current, 5); // exponential cap
      reconnectTimer.current = setTimeout(connect, delay);
    };
  }, [userId, dispatch, showToast]);

  useEffect(() => {
    mountedRef.current = true;
    if (userId) connect();

    return () => {
      mountedRef.current = false;
      clearTimeout(reconnectTimer.current);
      esRef.current?.close();
      dispatch(setSseConnected(false));
    };
  }, [userId, connect, dispatch]);

  return {
    close: () => {
      esRef.current?.close();
      dispatch(setSseConnected(false));
    }
  };
}