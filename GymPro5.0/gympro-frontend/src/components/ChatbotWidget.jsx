// src/components/ChatbotWidget.jsx
// GymPro AI Chatbot Widget — Production-ready
// Fixes: hooks-order, duplicate requests, timeouts, toast events,
//        conversation memory, retry logic, loading animation, offline resilience.

import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { chatbotApi } from '../api/chatbotApi';

/* ─── Constants ───────────────────────────────────────────────────────── */
const WELCOME_MSG = {
  id: 'welcome',
  from: 'bot',
  text: "👋 Hi! I'm **GymBot**, your GymPro assistant.\n\nAsk me about:\n• 📋 Plans & subscriptions\n• 📅 Booking sessions\n• 💳 Payments\n• 🏋️ Trainers\n• 💪 Fitness tips",
};

const MAX_HISTORY = 50; // max messages kept in UI

/* ─── Emit toast via DOM event (matches axiosInstance pattern) ─────────── */
function emitToast(message, type = 'warning') {
  window.dispatchEvent(new CustomEvent('gympro:toast', { detail: { message, type } }));
}

/* ─── Main Component ──────────────────────────────────────────────────── */
export default function ChatbotWidget() {
  const { token, role } = useAuth();

  // All hooks MUST be called unconditionally — early return is BELOW the hooks.
  const [open, setOpen]           = useState(false);
  const [messages, setMessages]   = useState([WELCOME_MSG]);
  const [input, setInput]         = useState('');
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState(null);
  const [failedMsg, setFailedMsg] = useState(null);
  const [online, setOnline]       = useState(true); // tracks backend reachability

  const messagesEndRef  = useRef(null);
  const inputRef        = useRef(null);
  const pendingRef      = useRef(false); // guard against duplicate sends
  const abortRef        = useRef(null);  // AbortController for timeout

  /* Auto-scroll */
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  /* Focus input when panel opens */
  useEffect(() => {
    if (open) {
      const id = setTimeout(() => inputRef.current?.focus(), 80);
      return () => clearTimeout(id);
    }
  }, [open]);

  /* Cleanup any in-flight request on unmount */
  useEffect(() => {
    return () => abortRef.current?.abort();
  }, []);

  /* ── Handlers ────────────────────────────────────────────────────────── */
  const handleClose = () => {
    setOpen(false);
    setError(null);
  };

  const handleOpen = () => setOpen((v) => !v);

  const doSend = useCallback(
    async (text) => {
      const trimmed = text?.trim();
      if (!trimmed) return;

      // Prevent duplicate sends (e.g. double-click, rapid Enter presses)
      if (pendingRef.current) return;
      pendingRef.current = true;

      setError(null);
      setFailedMsg(null);

      const userMsg = { id: `u-${Date.now()}`, from: 'user', text: trimmed };
      setMessages((prev) => {
        const next = [...prev, userMsg];
        // Trim UI history to avoid memory bloat
        return next.length > MAX_HISTORY ? next.slice(next.length - MAX_HISTORY) : next;
      });
      setInput('');
      setLoading(true);

      // Create an abort controller for request timeout (15 s)
      const controller = new AbortController();
      abortRef.current = controller;
      const timeoutId = setTimeout(() => controller.abort(), 15_000);

      try {
        const data = await chatbotApi.sendMessage(trimmed, role, controller.signal);
        const reply =
          data?.reply || data?.message || 'Sorry, I received an empty response. Please try again.';

        setMessages((prev) => [
          ...prev,
          { id: `b-${Date.now()}`, from: 'bot', text: reply },
        ]);
        setOnline(true);
      } catch (err) {
        if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') {
          // Request was aborted (timeout)
          const timeoutMsg = 'The request timed out. Please check your connection and try again.';
          setError(timeoutMsg);
          setFailedMsg(trimmed);
          emitToast(timeoutMsg, 'error');
          return;
        }

        const status = err.response?.status;
        const isOffline =
          !err.response ||
          err.code === 'ERR_NETWORK' ||
          status === 502 ||
          status === 503;

        const isUnauth = status === 401;

        let errorMsg;
        if (isUnauth) {
          errorMsg = 'Session expired. Please log in again.';
          emitToast(errorMsg, 'error');
        } else if (isOffline) {
          setOnline(false);
          errorMsg = 'GymBot is currently unavailable. Your message is saved — tap Retry when ready.';
          emitToast('Chatbot service is offline.', 'warning');
        } else {
          errorMsg =
            err.response?.data?.message ||
            err.response?.data?.error ||
            'Something went wrong. Please try again.';
          emitToast(errorMsg, 'error');
        }

        setError(errorMsg);
        setFailedMsg(trimmed);
      } finally {
        clearTimeout(timeoutId);
        setLoading(false);
        pendingRef.current = false;
      }
    },
    [role]
  );

  const handleSend = () => {
    if (!loading && input.trim()) doSend(input);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleRetry = () => {
    if (failedMsg && !loading) doSend(failedMsg);
  };

  const handleClear = () => {
    chatbotApi.clearConversation();
    setMessages([WELCOME_MSG]);
    setError(null);
    setFailedMsg(null);
    setOnline(true);
  };

  /* Only render widget for authenticated users.
     IMPORTANT: this check must come AFTER every hook call (useState/useEffect/
     useCallback/useRef above) so the number and order of hooks stays identical
     between the logged-out render and the logged-in render of this same
     mounted instance. Bailing out before a hook (as this used to do) made
     React throw "Rendered more hooks than during the previous render" the
     instant `token` flips from null -> set right after login, which crashed
     the whole app to a blank screen because ChatbotWidget sits outside any
     ErrorBoundary in App.jsx. */
  if (!token) return null;

  /* ── Render ──────────────────────────────────────────────────────────── */
  return (
    <>
      {/* ── Floating Action Button ───────────────────────────────────────── */}
      <button
        onClick={handleOpen}
        style={styles.fab}
        aria-label={open ? 'Close chatbot' : 'Open GymPro Assistant'}
        title="GymPro Assistant"
      >
        <span style={styles.fabIcon}>{open ? '✕' : '💬'}</span>
        {!open && <span style={styles.fabPulse} aria-hidden="true" />}
        {!open && !online && <span style={styles.offlineDot} title="Chatbot offline" />}
      </button>

      {/* ── Chat Panel ───────────────────────────────────────────────────── */}
      {open && (
        <div
          className="gympro-chat-panel"
          style={styles.panel}
          role="dialog"
          aria-label="GymPro Assistant"
          aria-modal="true"
        >
          {/* Header */}
          <div style={styles.header}>
            <div style={styles.headerLeft}>
              <div
                style={{
                  ...styles.statusDot,
                  background: online ? '#00E5A0' : '#FF5B6E',
                  boxShadow: online ? '0 0 6px #00E5A0' : '0 0 6px #FF5B6E',
                }}
                title={online ? 'Online' : 'Offline'}
              />
              <div>
                <div style={styles.headerTitle}>GymPro Assistant 🤖</div>
                <div style={styles.headerSub}>
                  {online ? 'Powered by AI · Online' : '⚠ Offline — fallback mode'}
                </div>
              </div>
            </div>
            <div style={styles.headerActions}>
              <button
                onClick={handleClear}
                style={styles.iconBtn}
                title="New conversation"
                aria-label="Start new conversation"
              >
                🔄
              </button>
              <button
                onClick={handleClose}
                style={styles.iconBtn}
                aria-label="Close chat"
              >
                ✕
              </button>
            </div>
          </div>

          {/* Message List */}
          <div style={styles.msgList} role="log" aria-live="polite" aria-label="Chat messages">
            {messages.map((m) => (
              <ChatMessage key={m.id} msg={m} />
            ))}

            {loading && <TypingIndicator />}

            {error && (
              <div style={styles.errorBubble} role="alert">
                <span>⚠ {error}</span>
                {failedMsg && (
                  <button
                    onClick={handleRetry}
                    style={styles.retryBtn}
                    disabled={loading}
                    aria-label="Retry sending message"
                  >
                    ↺ Retry
                  </button>
                )}
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Quick Prompts */}
          <QuickPrompts onSelect={(q) => doSend(q)} disabled={loading} />

          {/* Input Row */}
          <div style={styles.inputRow}>
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask me anything about GymPro…"
              rows={1}
              style={styles.textarea}
              disabled={loading}
              aria-label="Type your message"
              maxLength={500}
            />
            <button
              onClick={handleSend}
              disabled={loading || !input.trim()}
              style={{
                ...styles.sendBtn,
                opacity: loading || !input.trim() ? 0.4 : 1,
                cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
              }}
              aria-label="Send message"
            >
              {loading ? <span style={styles.spinner} /> : '➤'}
            </button>
          </div>
        </div>
      )}

      <style>{KEYFRAMES_CSS}</style>
    </>
  );
}

/* ─── Quick Prompts ────────────────────────────────────────────────────── */
const QUICK_PROMPTS = [
  { label: '📋 Plans', text: 'Tell me about the available gym membership plans.' },
  { label: '📅 Book', text: 'How do I book a training session?' },
  { label: '🏋️ Trainers', text: 'How do I find an available trainer?' },
  { label: '💳 Payments', text: 'How do I make a payment for my plan?' },
];

function QuickPrompts({ onSelect, disabled }) {
  return (
    <div style={styles.quickPrompts}>
      {QUICK_PROMPTS.map((p) => (
        <button
          key={p.label}
          onClick={() => !disabled && onSelect(p.text)}
          disabled={disabled}
          style={{
            ...styles.quickBtn,
            opacity: disabled ? 0.5 : 1,
            cursor: disabled ? 'not-allowed' : 'pointer',
          }}
          aria-label={p.text}
        >
          {p.label}
        </button>
      ))}
    </div>
  );
}

/* ─── Chat Message ─────────────────────────────────────────────────────── */
function ChatMessage({ msg }) {
  const isUser = msg.from === 'user';

  // Render markdown-style bold (**text**) without a library
  const renderText = (text) => {
    const parts = text.split(/(\*\*[^*]+\*\*)/g);
    return parts.map((part, i) => {
      if (part.startsWith('**') && part.endsWith('**')) {
        return <strong key={i}>{part.slice(2, -2)}</strong>;
      }
      // Render newlines as <br>
      return part.split('\n').map((line, j, arr) => (
        <span key={`${i}-${j}`}>
          {line}
          {j < arr.length - 1 && <br />}
        </span>
      ));
    });
  };

  return (
    <div
      style={{
        ...styles.msgRow,
        justifyContent: isUser ? 'flex-end' : 'flex-start',
      }}
    >
      {!isUser && <div style={styles.botAvatar} aria-hidden="true">🤖</div>}
      <div
        style={{
          ...styles.bubble,
          background: isUser
            ? 'linear-gradient(135deg, rgba(0,212,255,0.15), rgba(0,212,255,0.25))'
            : 'var(--bg3, #161B24)',
          border: isUser
            ? '1px solid rgba(0,212,255,0.4)'
            : '1px solid var(--border, #1E2A3A)',
          borderRadius: isUser ? '16px 4px 16px 16px' : '4px 16px 16px 16px',
          color: isUser ? '#00D4FF' : 'var(--text, #E8EDF5)',
        }}
      >
        {renderText(msg.text)}
      </div>
    </div>
  );
}

/* ─── Typing Indicator ─────────────────────────────────────────────────── */
function TypingIndicator() {
  return (
    <div style={{ ...styles.msgRow, justifyContent: 'flex-start' }} aria-label="GymBot is typing">
      <div style={styles.botAvatar} aria-hidden="true">🤖</div>
      <div
        style={{
          ...styles.bubble,
          background: 'var(--bg3, #161B24)',
          border: '1px solid var(--border, #1E2A3A)',
        }}
      >
        <div style={styles.dots} aria-hidden="true">
          {[0, 1, 2].map((i) => (
            <span
              key={i}
              style={{ ...styles.dot, animationDelay: `${i * 0.18}s` }}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

/* ─── Keyframe CSS ─────────────────────────────────────────────────────── */
const KEYFRAMES_CSS = `
  @keyframes fabPulse {
    0%   { transform: scale(1);   opacity: 0.6; }
    70%  { transform: scale(2.4); opacity: 0; }
    100% { transform: scale(2.4); opacity: 0; }
  }
  @keyframes chatSlideUp {
    from { opacity: 0; transform: translateY(24px) scale(0.97); }
    to   { opacity: 1; transform: translateY(0)    scale(1);    }
  }
  @keyframes dot-bounce {
    0%, 80%, 100% { transform: translateY(0);   opacity: 0.35; }
    40%           { transform: translateY(-7px); opacity: 1;    }
  }
  @keyframes spin {
    to { transform: rotate(360deg); }
  }
  @media (max-width: 480px) {
    .gympro-chat-panel {
      width: 100vw    !important;
      height: 100dvh  !important;
      bottom: 0       !important;
      right: 0        !important;
      border-radius: 0 !important;
    }
  }
`;

/* ─── Styles ───────────────────────────────────────────────────────────── */
const styles = {
  /* FAB */
  fab: {
    position: 'fixed',
    bottom: '28px',
    right: '28px',
    zIndex: 9999,
    width: '58px',
    height: '58px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #00D4FF, #00E5A0)',
    border: 'none',
    cursor: 'pointer',
    fontSize: '22px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: '0 4px 24px rgba(0,212,255,0.45)',
    transition: 'transform 0.2s, box-shadow 0.2s',
    color: '#000',
    outline: 'none',
  },
  fabIcon: { pointerEvents: 'none', lineHeight: 1 },
  fabPulse: {
    position: 'absolute',
    top: 0, left: 0,
    width: '100%', height: '100%',
    borderRadius: '50%',
    background: 'rgba(0,212,255,0.38)',
    animation: 'fabPulse 2.2s infinite',
    pointerEvents: 'none',
  },
  offlineDot: {
    position: 'absolute',
    top: '4px', right: '4px',
    width: '10px', height: '10px',
    borderRadius: '50%',
    background: 'var(--red, #FF5B6E)',
    border: '2px solid var(--bg, #0F1318)',
  },

  /* Panel */
  panel: {
    position: 'fixed',
    bottom: '100px',
    right: '28px',
    zIndex: 9999,
    width: '360px',
    height: '540px',
    display: 'flex',
    flexDirection: 'column',
    background: 'var(--bg2, #0F1318)',
    border: '1px solid var(--border, #1E2A3A)',
    borderTop: '2px solid #00D4FF',
    borderRadius: '18px',
    boxShadow: '0 20px 64px rgba(0,0,0,0.72)',
    overflow: 'hidden',
    animation: 'chatSlideUp 0.28s ease',
    fontFamily: 'var(--font-body, "DM Sans", sans-serif)',
    fontSize: '14px',
  },

  /* Header */
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '13px 14px',
    borderBottom: '1px solid var(--border, #1E2A3A)',
    background: 'var(--bg3, #161B24)',
    flexShrink: 0,
  },
  headerLeft: { display: 'flex', alignItems: 'center', gap: '10px' },
  statusDot: {
    width: '10px', height: '10px',
    borderRadius: '50%',
    flexShrink: 0,
    transition: 'background 0.3s, box-shadow 0.3s',
  },
  headerTitle: { color: 'var(--text, #E8EDF5)', fontWeight: 700, fontSize: '13.5px' },
  headerSub: { color: 'var(--text2, #8B9BB4)', fontSize: '11px', marginTop: '1px' },
  headerActions: { display: 'flex', gap: '4px', alignItems: 'center' },
  iconBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--text2, #8B9BB4)',
    fontSize: '15px',
    cursor: 'pointer',
    padding: '5px 6px',
    borderRadius: '6px',
    lineHeight: 1,
    width: 'auto',
    transition: 'color 0.2s, background 0.2s',
  },

  /* Messages */
  msgList: {
    flex: 1,
    overflowY: 'auto',
    padding: '14px',
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    scrollbarWidth: 'thin',
    scrollbarColor: 'var(--border2, #1E2A3A) transparent',
  },
  msgRow: { display: 'flex', alignItems: 'flex-end', gap: '8px' },
  botAvatar: { fontSize: '18px', flexShrink: 0, lineHeight: 1, marginBottom: '2px' },
  bubble: {
    maxWidth: '80%',
    padding: '10px 13px',
    fontSize: '13px',
    lineHeight: '1.6',
    wordBreak: 'break-word',
    borderRadius: '12px',
  },

  /* Error */
  errorBubble: {
    background: 'rgba(255,91,110,0.1)',
    border: '1px solid rgba(255,91,110,0.3)',
    borderRadius: '10px',
    padding: '10px 13px',
    fontSize: '12.5px',
    color: '#FF5B6E',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  retryBtn: {
    background: 'rgba(255,91,110,0.15)',
    border: '1px solid rgba(255,91,110,0.4)',
    color: '#FF5B6E',
    borderRadius: '6px',
    padding: '5px 13px',
    fontSize: '12px',
    fontWeight: 600,
    cursor: 'pointer',
    alignSelf: 'flex-start',
    fontFamily: 'inherit',
    width: 'auto',
    transition: 'background 0.2s',
  },

  /* Quick prompts */
  quickPrompts: {
    display: 'flex',
    gap: '6px',
    padding: '8px 12px',
    overflowX: 'auto',
    flexShrink: 0,
    borderTop: '1px solid var(--border, #1E2A3A)',
    scrollbarWidth: 'none',
  },
  quickBtn: {
    flexShrink: 0,
    background: 'var(--bg3, #161B24)',
    border: '1px solid var(--border, #1E2A3A)',
    borderRadius: '20px',
    color: 'var(--text2, #8B9BB4)',
    fontSize: '11.5px',
    padding: '4px 10px',
    fontFamily: 'inherit',
    whiteSpace: 'nowrap',
    transition: 'border-color 0.2s, color 0.2s',
  },

  /* Input */
  inputRow: {
    display: 'flex',
    alignItems: 'flex-end',
    gap: '8px',
    padding: '10px 12px',
    borderTop: '1px solid var(--border, #1E2A3A)',
    background: 'var(--bg3, #161B24)',
    flexShrink: 0,
  },
  textarea: {
    flex: 1,
    resize: 'none',
    background: 'var(--bg2, #0F1318)',
    border: '1px solid var(--border2, #253244)',
    borderRadius: '10px',
    padding: '9px 12px',
    color: 'var(--text, #E8EDF5)',
    fontSize: '13px',
    lineHeight: '1.45',
    maxHeight: '96px',
    overflowY: 'auto',
    outline: 'none',
    fontFamily: 'inherit',
    width: 'auto',
    transition: 'border-color 0.2s',
  },
  sendBtn: {
    background: 'linear-gradient(135deg, #00D4FF, #00E5A0)',
    border: 'none',
    borderRadius: '10px',
    width: '40px',
    height: '40px',
    fontSize: '15px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
    transition: 'opacity 0.2s, transform 0.15s',
    color: '#000',
  },
  spinner: {
    display: 'inline-block',
    width: '14px',
    height: '14px',
    border: '2px solid rgba(0,0,0,0.3)',
    borderTop: '2px solid #000',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },

  /* Typing dots */
  dots: { display: 'flex', gap: '4px', padding: '2px 4px', alignItems: 'center' },
  dot: {
    display: 'inline-block',
    width: '7px', height: '7px',
    borderRadius: '50%',
    background: '#00D4FF',
    animation: 'dot-bounce 1.3s ease infinite',
  },
};