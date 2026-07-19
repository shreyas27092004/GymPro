// src/theme/ThemeContext.jsx
// Light/dark theme system. Persists the user's explicit choice in
// localStorage; falls back to the OS-level preference on first load.
// Toggling updates document.documentElement's [data-theme] attribute, which
// index.css uses to swap the entire CSS-variable palette instantly — no
// component-level re-render or re-fetch required.

import { createContext, useContext, useEffect, useState, useCallback } from 'react';

const ThemeContext = createContext(null);
const STORAGE_KEY = 'gympro-theme';

function getInitialTheme() {
  if (typeof window === 'undefined') return 'dark';
  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (stored === 'light' || stored === 'dark') return stored;
  const prefersLight = window.matchMedia?.('(prefers-color-scheme: light)').matches;
  return prefersLight ? 'light' : 'dark';
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme);

  // Apply to <html> whenever theme changes.
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    window.localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  // If the user never explicitly chose a theme, keep following the OS
  // preference live (e.g. system switches to dark at sunset).
  useEffect(() => {
    const mql = window.matchMedia?.('(prefers-color-scheme: light)');
    if (!mql) return;
    const handleChange = (e) => {
      const hasExplicitChoice = window.localStorage.getItem(STORAGE_KEY + '-explicit') === '1';
      if (!hasExplicitChoice) setTheme(e.matches ? 'light' : 'dark');
    };
    mql.addEventListener?.('change', handleChange);
    return () => mql.removeEventListener?.('change', handleChange);
  }, []);

  const toggleTheme = useCallback(() => {
    window.localStorage.setItem(STORAGE_KEY + '-explicit', '1');
    setTheme((t) => (t === 'dark' ? 'light' : 'dark'));
  }, []);

  const setExplicitTheme = useCallback((next) => {
    window.localStorage.setItem(STORAGE_KEY + '-explicit', '1');
    setTheme(next);
  }, []);

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, setTheme: setExplicitTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within a ThemeProvider');
  return ctx;
}
