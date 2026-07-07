// src/components/NotificationBell.jsx
// In-app notification bell with:
//   • Redux state management
//   • SSE real-time connection (via useNotificationSSE hook)
//   • Paginated load-more
//   • Mark read / delete actions
//   • Unread badge count
//   • Dropdown panel with role-based indicator
//   • Persists across page navigation (Redux store)

import { useState, useEffect, useRef, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchNotifications,
  fetchUnreadCount,
  markNotificationRead,
  markAllRead,
  deleteNotification,
  clearAllNotifications,
  selectNotifications,
  selectUnreadCount,
  selectNotifLoading,
  selectNotifHasNext,
  selectNotifPage,
  selectSseConnected,
} from '../store/slices/notificationSlice';
import { useAuth } from '../context/AuthContext';
import { useNotificationSSE } from '../hooks/useNotificationSSE';

// ── Helpers ────────────────────────────────────────────────────────────────

function timeAgo(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (isNaN(date)) return '';
  const diff = (Date.now() - date.getTime()) / 1000;
  if (diff < 60)    return 'just now';
  if (diff < 3600)  return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
}

const TYPE_META = {
  BOOKING: { icon: '📅', color: '#00d4ff' },
  PAYMENT: { icon: '💳', color: '#00e5a0' },
  PLAN:    { icon: '🎉', color: '#a78bfa' },
  SYSTEM:  { icon: '⚙️', color: '#ffd700' },
};

function getMeta(type) {
  return TYPE_META[type] || { icon: '🔔', color: '#00d4ff' };
}

// ── Component ──────────────────────────────────────────────────────────────

export default function NotificationBell({ userId: propUserId, memberId }) {
  const { userId: ctxUserId, role } = useAuth();
  const dispatch  = useDispatch();
  const panelRef  = useRef(null);
  const [open, setOpen] = useState(false);

  // Resolve effective userId — prop > memberId > context
  const effectiveId = propUserId || memberId || ctxUserId || null;

  // Redux state
  const notifications = useSelector(selectNotifications);
  const unreadCount   = useSelector(selectUnreadCount);
  const loading       = useSelector(selectNotifLoading);
  const hasNext       = useSelector(selectNotifHasNext);
  const currentPage   = useSelector(selectNotifPage);
  const sseConnected  = useSelector(selectSseConnected);

  // ── SSE real-time connection ─────────────────────────────────────────────
  useNotificationSSE(effectiveId);

  // ── Initial data load ────────────────────────────────────────────────────
  useEffect(() => {
    if (!effectiveId) return;
    dispatch(fetchNotifications({ userId: effectiveId, page: 0, size: 20 }));
    // Polling fallback for environments where SSE is unavailable
    const interval = setInterval(() => {
      dispatch(fetchUnreadCount(effectiveId));
    }, 30_000);
    return () => clearInterval(interval);
  }, [effectiveId, dispatch]);

  // ── Outside click to close ───────────────────────────────────────────────
  useEffect(() => {
    const handler = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) setOpen(false);
    };
    if (open) document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  // ── Open / close ─────────────────────────────────────────────────────────
  const handleOpen = async () => {
    const wasOpen = open;
    setOpen(o => !o);
    if (!wasOpen && unreadCount > 0 && effectiveId) {
      await dispatch(markAllRead(effectiveId));
    }
  };

  // ── Load more (pagination) ────────────────────────────────────────────────
  const handleLoadMore = useCallback(() => {
    if (!loading && hasNext && effectiveId) {
      dispatch(fetchNotifications({ userId: effectiveId, page: currentPage + 1, size: 20 }));
    }
  }, [loading, hasNext, effectiveId, currentPage, dispatch]);

  // ── Delete single notification ────────────────────────────────────────────
  const handleDelete = (e, notificationId) => {
    e.stopPropagation();
    if (effectiveId) {
      dispatch(deleteNotification({ notificationId, userId: effectiveId }));
    }
  };

  // ── Mark single as read ───────────────────────────────────────────────────
  const handleMarkRead = (e, notificationId, isRead) => {
    e.stopPropagation();
    if (!isRead) dispatch(markNotificationRead(notificationId));
  };

  // ── Render ────────────────────────────────────────────────────────────────
  const displayCount = unreadCount > 99 ? '99+' : unreadCount > 0 ? String(unreadCount) : null;

  return (
    <div ref={panelRef} style={{ position: 'relative' }}>

      {/* ── Bell button ──────────────────────────────────────────────────── */}
      <button
        onClick={handleOpen}
        title={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
        style={{
          background: open ? 'rgba(0,212,255,0.12)' : 'transparent',
          border: `1px solid ${open ? 'rgba(0,212,255,0.3)' : 'var(--border)'}`,
          borderRadius: 10,
          padding: '8px 10px',
          cursor: 'pointer',
          color: open ? 'var(--accent)' : 'var(--text3)',
          fontSize: 18,
          lineHeight: 1,
          position: 'relative',
          transition: 'all 0.2s',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '100%',
          marginBottom: 6,
        }}
      >
        🔔
        {/* Unread badge */}
        {displayCount && (
          <span style={{
            position: 'absolute',
            top: 2, right: 2,
            background: '#ff4d6d',
            color: '#fff',
            fontSize: displayCount.length > 1 ? 8 : 9,
            fontWeight: 700,
            borderRadius: 8,
            minWidth: 16, height: 16,
            padding: '0 3px',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            border: '1.5px solid var(--bg)',
            lineHeight: 1,
          }}>
            {displayCount}
          </span>
        )}
        {/* SSE connection dot */}
        <span style={{
          position: 'absolute',
          bottom: 4, right: 4,
          width: 5, height: 5,
          borderRadius: '50%',
          background: sseConnected ? '#00e5a0' : '#ffd700',
          opacity: 0.8,
        }} title={sseConnected ? 'Live updates active' : 'Polling mode'} />
      </button>

      {/* ── Dropdown panel ───────────────────────────────────────────────── */}
      {open && (
        <div style={{
          position: 'fixed',
          left: 232,
          bottom: 120,
          width: 340,
          background: 'var(--bg2)',
          border: '1px solid var(--border)',
          borderRadius: 14,
          boxShadow: '0 12px 40px rgba(0,0,0,0.55)',
          zIndex: 1000,
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}>

          {/* Header */}
          <div style={{
            padding: '14px 16px',
            borderBottom: '1px solid var(--border)',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          }}>
            <span style={{ fontWeight: 700, color: 'var(--text)', fontSize: 14 }}>
              🔔 Notifications
              {role && (
                <span style={{
                  marginLeft: 8, fontSize: 10, fontWeight: 600,
                  color: role === 'ADMIN' ? 'var(--amber)' : role === 'TRAINER' ? 'var(--green)' : 'var(--accent)',
                  background: 'rgba(255,255,255,0.07)', borderRadius: 4, padding: '2px 6px',
                }}>
                  {role}
                </span>
              )}
            </span>
            <div style={{ display: 'flex', gap: 6 }}>
              {/* Refresh */}
              <button
                onClick={() => effectiveId && dispatch(fetchNotifications({ userId: effectiveId, page: 0, size: 20 }))}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text3)', fontSize: 16 }}
                title="Refresh"
              >↻</button>
              {/* Clear all */}
              {notifications.length > 0 && (
                <button
                  onClick={() => effectiveId && dispatch(clearAllNotifications(effectiveId))}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text3)', fontSize: 11 }}
                  title="Clear all"
                >Clear all</button>
              )}
            </div>
          </div>

          {/* Notification list */}
          <div style={{ maxHeight: 380, overflowY: 'auto' }}>
            {loading && notifications.length === 0 ? (
              <div style={{ padding: 24, textAlign: 'center', color: 'var(--text3)', fontSize: 13 }}>
                Loading…
              </div>
            ) : notifications.length === 0 ? (
              <div style={{ padding: 32, textAlign: 'center', color: 'var(--text3)', fontSize: 13 }}>
                <div style={{ fontSize: 28, marginBottom: 8 }}>🔕</div>
                No notifications yet
              </div>
            ) : (
              <>
                {notifications.map((n, i) => {
                  const meta = getMeta(n.type);
                  return (
                    <div
                      key={n.id}
                      onClick={(e) => handleMarkRead(e, n.id, n.isRead)}
                      style={{
                        display: 'flex', alignItems: 'flex-start', gap: 10,
                        padding: '11px 14px',
                        borderBottom: i < notifications.length - 1 ? '1px solid var(--border)' : 'none',
                        background: n.isRead ? 'transparent' : 'rgba(0,212,255,0.04)',
                        cursor: 'pointer', transition: 'background 0.15s',
                        position: 'relative',
                      }}
                      onMouseOver={e => e.currentTarget.style.background = 'var(--bg3)'}
                      onMouseOut={e => e.currentTarget.style.background = n.isRead ? 'transparent' : 'rgba(0,212,255,0.04)'}
                    >
                      {/* Icon bubble */}
                      <div style={{
                        width: 34, height: 34, borderRadius: '50%',
                        background: `${meta.color}18`, border: `1px solid ${meta.color}40`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 14, flexShrink: 0,
                      }}>
                        {meta.icon}
                      </div>

                      {/* Text */}
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{
                          fontSize: 12.5, fontWeight: n.isRead ? 500 : 700,
                          color: n.isRead ? 'var(--text2)' : 'var(--text)',
                          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                        }}>
                          {n.title}
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 2, lineHeight: 1.4 }}>
                          {n.message}
                        </div>
                      </div>

                      {/* Time + unread dot + delete */}
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4, flexShrink: 0 }}>
                        <span style={{ fontSize: 10, color: 'var(--text3)', whiteSpace: 'nowrap' }}>
                          {timeAgo(n.createdAt)}
                        </span>
                        {!n.isRead && (
                          <span style={{
                            width: 7, height: 7, borderRadius: '50%', background: '#00d4ff',
                          }} />
                        )}
                        {/* Delete button — shows on hover */}
                        <button
                          onClick={(e) => handleDelete(e, n.id)}
                          style={{
                            background: 'none', border: 'none', cursor: 'pointer',
                            color: 'var(--text3)', fontSize: 10, padding: 0,
                            opacity: 0.5,
                          }}
                          title="Delete"
                        >✕</button>
                      </div>
                    </div>
                  );
                })}

                {/* Load more */}
                {hasNext && (
                  <div style={{ padding: '10px 16px', textAlign: 'center' }}>
                    <button
                      onClick={handleLoadMore}
                      disabled={loading}
                      style={{
                        background: 'none', border: '1px solid var(--border)',
                        color: 'var(--text3)', borderRadius: 6, padding: '6px 16px',
                        cursor: loading ? 'not-allowed' : 'pointer', fontSize: 11,
                      }}
                    >
                      {loading ? 'Loading…' : 'Load more'}
                    </button>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Footer */}
          {notifications.length > 0 && (
            <div style={{
              padding: '9px 14px', borderTop: '1px solid var(--border)',
              textAlign: 'center', fontSize: 10.5, color: 'var(--text3)',
            }}>
              {notifications.length} shown
              {unreadCount > 0 ? ` · ${unreadCount} unread` : ' · all read'}
              {sseConnected && <span style={{ color: '#00e5a0', marginLeft: 6 }}>● live</span>}
            </div>
          )}
        </div>
      )}
    </div>
  );
}