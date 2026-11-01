// src/api/chatbotApi.js
// Chatbot API — POST /chatbot/chat
// Persists conversationId in sessionStorage for the browser session.
// Supports AbortSignal for request timeout from the widget.

import axiosInstance from './axiosInstance';

const CONV_ID_KEY = 'chatbot_conversation_id';

export const chatbotApi = {
  /**
   * Send a message to the chatbot.
   *
   * @param {string} message  - The user's message text
   * @param {string} role     - The user's role: ADMIN | TRAINER | MEMBER
   * @param {AbortSignal} [signal] - Optional AbortSignal for timeout cancellation
   * @returns {Promise<{ reply: string, conversationId: string, timestamp: string }>}
   */
  sendMessage: async (message, role, signal) => {
    const conversationId = sessionStorage.getItem(CONV_ID_KEY) || undefined;

    const response = await axiosInstance.post(
      '/chatbot/chat',
      { message, role, conversationId },
      { signal }                          // pass signal through for timeout / abort
    );

    // Persist the conversation ID returned by the server for the next turn
    if (response.data?.conversationId) {
      sessionStorage.setItem(CONV_ID_KEY, response.data.conversationId);
    }

    return response.data;
  },

  /**
   * Tell the backend to clear the conversation history, then clear the local key.
   * Safe to call even when the backend is offline (local key is always cleared).
   */
  clearConversation: async () => {
    const conversationId = sessionStorage.getItem(CONV_ID_KEY);
    sessionStorage.removeItem(CONV_ID_KEY);

    if (conversationId) {
      try {
        await axiosInstance.delete(`/chatbot/conversation/${conversationId}`);
      } catch {
        // Silently swallow — the in-memory store will be re-initialised on next message.
      }
    }
  },

  /**
   * Lightweight health ping to check if chatbot-service is reachable.
   * Returns true if reachable, false otherwise.
   */
  ping: async () => {
    try {
      await axiosInstance.get('/chatbot/health', { timeout: 5000 });
      return true;
    } catch {
      return false;
    }
  },
};