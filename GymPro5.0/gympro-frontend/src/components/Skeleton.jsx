// src/components/Skeleton.jsx
// Animated shimmer skeleton placeholders using --bg3 CSS variable.
// Usage:
//   <SkeletonCard />          — card-sized block
//   <SkeletonRow />           — single table row placeholder
//   <SkeletonStat />          — stat/metric widget
//   <SkeletonTable rows={5} /> — full table placeholder
//   <SkeletonGrid cols={3} />  — grid of cards

/* ─── Base shimmer bar ──────────────────────────────────────────────── */
export function SkeletonBar({ width = '100%', height = '14px', radius = '6px', style = {} }) {
  return (
    <div
      className="skeleton-shimmer"
      style={{
        width,
        height,
        borderRadius: radius,
        ...style,
      }}
    />
  );
}

/* ─── Stat card (metric widgets in Overview) ────────────────────────── */
export function SkeletonStat() {
  return (
    <div style={styles.statCard}>
      <SkeletonBar width="40px" height="40px" radius="10px" />
      <div style={{ flex: 1 }}>
        <SkeletonBar width="60%" height="12px" style={{ marginBottom: '10px' }} />
        <SkeletonBar width="40%" height="22px" />
      </div>
    </div>
  );
}

/* ─── Generic card block ────────────────────────────────────────────── */
export function SkeletonCard({ lines = 3 }) {
  return (
    <div style={styles.card}>
      <SkeletonBar width="50%" height="16px" style={{ marginBottom: '16px' }} />
      {Array.from({ length: lines }).map((_, i) => (
        <SkeletonBar
          key={i}
          width={i === lines - 1 ? '70%' : '100%'}
          height="12px"
          style={{ marginBottom: '10px' }}
        />
      ))}
    </div>
  );
}

/* ─── Table row placeholder ─────────────────────────────────────────── */
export function SkeletonRow({ cols = 4 }) {
  return (
    <div style={styles.tableRow}>
      {Array.from({ length: cols }).map((_, i) => (
        <SkeletonBar key={i} width={i === 0 ? '30%' : '20%'} height="12px" />
      ))}
    </div>
  );
}

/* ─── Full table skeleton ───────────────────────────────────────────── */
export function SkeletonTable({ rows = 5, cols = 4 }) {
  return (
    <div style={styles.table}>
      {/* Header */}
      <div style={{ ...styles.tableRow, marginBottom: '4px' }}>
        {Array.from({ length: cols }).map((_, i) => (
          <SkeletonBar key={i} width={i === 0 ? '25%' : '18%'} height="10px" />
        ))}
      </div>
      <div style={styles.divider} />
      {Array.from({ length: rows }).map((_, i) => (
        <SkeletonRow key={i} cols={cols} />
      ))}
    </div>
  );
}

/* ─── Grid of cards ─────────────────────────────────────────────────── */
export function SkeletonGrid({ cols = 3, cards = 6 }) {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: `repeat(${cols}, 1fr)`,
        gap: '16px',
      }}
    >
      {Array.from({ length: cards }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
  );
}

/* ─── Admin Overview skeleton ───────────────────────────────────────── */
export function SkeletonAdminOverview() {
  return (
    <div style={styles.overviewWrap}>
      {/* Stats row */}
      <div style={styles.statsRow}>
        {Array.from({ length: 4 }).map((_, i) => (
          <SkeletonStat key={i} />
        ))}
      </div>
      {/* Two column area */}
      <div style={styles.twoCol}>
        <SkeletonCard lines={6} />
        <SkeletonCard lines={6} />
      </div>
      {/* Full-width table */}
      <SkeletonTable rows={6} cols={5} />
    </div>
  );
}

/* ─── Member Dashboard skeleton ─────────────────────────────────────── */
export function SkeletonMemberDashboard() {
  return (
    <div style={styles.overviewWrap}>
      <div style={styles.statsRow}>
        {Array.from({ length: 3 }).map((_, i) => (
          <SkeletonStat key={i} />
        ))}
      </div>
      <SkeletonTable rows={5} cols={4} />
      <SkeletonGrid cols={2} cards={4} />
    </div>
  );
}

/* ─── Styles ─────────────────────────────────────────────────────────── */
const styles = {
  statCard: {
    background: 'var(--bg2, #0F1318)',
    border: '1px solid var(--border, #1E2A3A)',
    borderRadius: '12px',
    padding: '18px',
    display: 'flex',
    gap: '14px',
    alignItems: 'center',
  },
  card: {
    background: 'var(--bg2, #0F1318)',
    border: '1px solid var(--border, #1E2A3A)',
    borderRadius: '12px',
    padding: '20px',
  },
  tableRow: {
    display: 'flex',
    gap: '16px',
    alignItems: 'center',
    padding: '12px 0',
    borderBottom: '1px solid var(--border, #1E2A3A)',
  },
  table: {
    background: 'var(--bg2, #0F1318)',
    border: '1px solid var(--border, #1E2A3A)',
    borderRadius: '12px',
    padding: '16px 20px',
  },
  divider: {
    height: '1px',
    background: 'var(--border, #1E2A3A)',
    margin: '8px 0',
  },
  statsRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
    gap: '16px',
    marginBottom: '20px',
  },
  twoCol: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '16px',
    marginBottom: '20px',
  },
  overviewWrap: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
    padding: '24px',
  },
};

/* ─── Global shimmer CSS — inject once ──────────────────────────────── */
if (typeof document !== 'undefined') {
  const styleId = 'gympro-skeleton-styles';
  if (!document.getElementById(styleId)) {
    const el = document.createElement('style');
    el.id = styleId;
    el.textContent = `
      .skeleton-shimmer {
        background: linear-gradient(
          90deg,
          var(--bg3, #161B24) 25%,
          var(--bg4, #1C2230) 50%,
          var(--bg3, #161B24) 75%
        );
        background-size: 200% 100%;
        animation: shimmer 1.6s infinite ease-in-out;
        display: block;
        flex-shrink: 0;
      }
      @keyframes shimmer {
        0%   { background-position: 200% 0; }
        100% { background-position: -200% 0; }
      }
    `;
    document.head.appendChild(el);
  }
}