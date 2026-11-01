// src/components/icons.jsx
// Shared 24x24 stroke-icon set (currentColor, 1.8 stroke) — the same visual
// language as Sidebar's nav icons. Renders via <Icon name="..." /> so every
// dashboard page can swap emoji for a consistent, theme-aware icon without
// each file redefining its own SVG paths.

const PATHS = {
  users: <><circle cx="9" cy="8" r="3.2" /><path d="M2.5 20c.7-3.4 3.2-5.5 6.5-5.5s5.8 2.1 6.5 5.5" /><path d="M16 8.2a3.2 3.2 0 1 1 3 4.2" /><path d="M15.5 14.7c2.6.4 4.5 2.2 5 5.3" /></>,
  user: <><circle cx="12" cy="8.3" r="3.5" /><path d="M4.5 20.5c1-4 3.7-6.3 7.5-6.3s6.5 2.3 7.5 6.3" /></>,
  dumbbell: <><path d="M6.5 9v6" /><path d="M17.5 9v6" /><path d="M3 10.5v3" /><path d="M21 10.5v3" /><path d="M6.5 12h11" /></>,
  clipboard: <><rect x="5.5" y="4.5" width="13" height="16" rx="2" /><path d="M9 4.5V3.8A1.3 1.3 0 0 1 10.3 2.5h3.4A1.3 1.3 0 0 1 15 3.8v.7" /><path d="M8.5 11h7" /><path d="M8.5 14.5h7" /><path d="M8.5 18h4.5" /></>,
  calendar: <><rect x="3.5" y="5" width="17" height="15.5" rx="2" /><path d="M3.5 9.5h17" /><path d="M8 3v4" /><path d="M16 3v4" /></>,
  card: <><rect x="3" y="5.5" width="18" height="13" rx="2" /><path d="M3 9.5h18" /><path d="M6.5 14.5h4" /></>,
  clock: <><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>,
  logout: <><path d="M9 20.5H5.5a1.5 1.5 0 0 1-1.5-1.5v-14a1.5 1.5 0 0 1 1.5-1.5H9" /><path d="M16 16.5 21 12l-5-4.5" /><path d="M21 12H9" /></>,

  checkCircle: <><circle cx="12" cy="12" r="8.5" /><path d="M8.5 12.2l2.4 2.4 4.6-5.2" /></>,
  alertTriangle: <><path d="M12 4 21.5 20H2.5Z" strokeLinejoin="round" /><path d="M12 10v4.5" /><circle cx="12" cy="17.6" r="0.4" fill="currentColor" stroke="none" /></>,
  lock: <><rect x="5" y="10.5" width="14" height="10" rx="2" /><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" /></>,
  cash: <><rect x="2.5" y="6.5" width="19" height="11" rx="2" /><circle cx="12" cy="12" r="2.6" /><path d="M5.5 9v0M18.5 15v0" strokeLinecap="round" /></>,
  bank: <><path d="M3 9.5 12 4l9 5.5" /><path d="M4.5 9.5v9M9 9.5v9M15 9.5v9M19.5 9.5v9" /><path d="M3 20.5h18" /></>,
  smartphone: <><rect x="7" y="2.5" width="10" height="19" rx="2" /><path d="M11 18.5h2" /></>,
  qrcode: <><rect x="3.5" y="3.5" width="6.5" height="6.5" rx="1" /><rect x="14" y="3.5" width="6.5" height="6.5" rx="1" /><rect x="3.5" y="14" width="6.5" height="6.5" rx="1" /><path d="M14 14h3v3h-3zM19.5 14h1v1h-1zM14 19.5h1v1h-1zM19.5 19.5h1v1h-1z" fill="currentColor" stroke="none" /></>,
  undo: <><path d="M4 10h10a5.5 5.5 0 0 1 0 11H9" /><path d="M8 5.5 4 10l4 4.5" /></>,
  edit: <><path d="M15.2 4.3 19.7 8.8 8 20.5H3.5V16Z" strokeLinejoin="round" /></>,
  trash: <><path d="M4.5 7h15" /><path d="M9 7V5a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 5v2" /><path d="M6.5 7l1 12.5A1.5 1.5 0 0 0 9 21h6a1.5 1.5 0 0 0 1.5-1.5L17.5 7" /><path d="M10 11v6M14 11v6" /></>,
  ticket: <><path d="M3 9.5A2 2 0 0 0 5 7.5h14a2 2 0 0 0 2 2v5a2 2 0 0 0-2 2H5a2 2 0 0 0-2-2Z" strokeLinejoin="round" /><path d="M14.5 7.5v9" strokeDasharray="2 2.3" /></>,
  infinity: <><path d="M7 9.5c-2.5 0-4 1.4-4 2.9s1.5 2.9 4 2.9c3 0 4.5-5.8 7.5-5.8 2.5 0 4 1.4 4 2.9s-1.5 2.9-4 2.9c-3 0-4.5-5.8-7.5-5.8Z" strokeLinejoin="round" /></>,
  inbox: <><path d="M3.5 12h5l1.7 2.8h3.6L15.5 12h5" /><path d="M4 10.8 5.5 5a1.5 1.5 0 0 1 1.4-1h10.2a1.5 1.5 0 0 1 1.4 1L20 10.8V18a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18Z" strokeLinejoin="round" /></>,
  settings: <><circle cx="12" cy="12" r="2.8" /><path d="M12 4v2.2M12 17.8V20M4 12h2.2M17.8 12H20M6.3 6.3l1.6 1.6M16.1 16.1l1.6 1.6M17.7 6.3l-1.6 1.6M7.9 16.1l-1.6 1.6" /></>,
  bell: <><path d="M6 10a6 6 0 0 1 12 0c0 4 1.5 5.5 1.5 5.5h-15S6 14 6 10Z" strokeLinejoin="round" /><path d="M9.5 18.5a2.5 2.5 0 0 0 5 0" /></>,
  bellOff: <><path d="M6 10a6 6 0 0 1 9.8-4.6M18 12.4c.2.9.7 1.5 1.5 3.1h-13" strokeLinejoin="round" /><path d="M9.5 18.5a2.5 2.5 0 0 0 5 0" /><path d="M3.5 3.5l17 17" /></>,
  wallet: <><rect x="3" y="6.5" width="18" height="12.5" rx="2" /><path d="M3 10.5h18" /><circle cx="16.5" cy="14.5" r="1" fill="currentColor" stroke="none" /></>,
  home: <><path d="M4 11.5 12 4l8 7.5" /><path d="M6 10v9a1 1 0 0 0 1 1h3v-6h4v6h3a1 1 0 0 0 1-1v-9" /></>,
  trendingUp: <><path d="M3.5 17 10 10.5l4 4 6.5-6.5" /><path d="M15 8h5.5v5.5" /></>,
};

export default function Icon({ name, size = 16, strokeWidth = 1.8, className = '', style = {} }) {
  const path = PATHS[name];
  if (!path) return null;
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={{ flexShrink: 0, ...style }}
    >
      {path}
    </svg>
  );
}
