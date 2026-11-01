// src/pages/Landing.jsx
// Public marketing landing page — entry point before Login/Register.

import { Link } from 'react-router-dom';
import ThemeToggle from '../theme/ThemeToggle';

const FEATURES = [
  {
    icon: '📅',
    title: 'Smart Bookings',
    desc: 'Members book trainer sessions in seconds, with real-time availability and instant confirmation.',
  },
  {
    icon: '💳',
    title: 'Seamless Payments',
    desc: 'Integrated Razorpay checkout for plans and sessions — cash, card, or UPI, all reconciled automatically.',
  },
  {
    icon: '🔔',
    title: 'Live Notifications',
    desc: 'Instant in-app and email alerts for bookings, payments, and plan updates, powered by RabbitMQ.',
  },
  {
    icon: '🏋️',
    title: 'Trainer Management',
    desc: 'Admins manage trainer rosters, specialties, and session fees from a single control center.',
  },
  {
    icon: '📊',
    title: 'Membership Plans',
    desc: 'Flexible plan tiers with built-in eligibility checks for free trainer sessions.',
  },
  {
    icon: '🤖',
    title: 'AI Assistant',
    desc: 'A built-in chatbot answers member questions instantly, day or night.',
  },
];

const STEPS = [
  { n: '01', title: 'Create an account', desc: 'Register as a member in under a minute.' },
  { n: '02', title: 'Pick a plan', desc: 'Choose a membership tier that fits your goals.' },
  { n: '03', title: 'Book & train', desc: 'Reserve trainer sessions and start showing up.' },
];

export default function Landing() {
  return (
    <div className="landing">
      {/* ── Nav ─────────────────────────────────────────────── */}
      <header className="landing-nav">
        <div className="landing-nav-inner">
          <div className="landing-logo">GYMPRO</div>
          <nav className="landing-nav-links">
            <a href="#features">Features</a>
            <a href="#how-it-works">How it works</a>
            <a href="#about">About</a>
          </nav>
          <div className="landing-nav-actions">
            <ThemeToggle />
            <Link to="/login" className="btn-ghost-nav">Sign In</Link>
            <Link to="/register" className="btn-primary-nav">Get Started</Link>
          </div>
        </div>
      </header>

      {/* ── Hero ────────────────────────────────────────────── */}
      <section className="landing-hero">
        <div className="landing-bg-grid" />
        <div className="landing-bg-glow" />
        <div className="landing-hero-inner">
          <span className="landing-badge">● Now with live availability booking</span>
          <h1 className="landing-hero-title">
            Run your gym.<br />
            <span className="landing-hero-accent">Grow your members.</span>
          </h1>
          <p className="landing-hero-sub">
            GymPro is an all-in-one gym management platform — bookings, payments,
            trainers, and notifications, unified for admins, trainers, and members.
          </p>
          <div className="landing-hero-cta">
            <Link to="/register" className="btn-primary-lg">Create free account</Link>
            <Link to="/login" className="btn-ghost-lg">Sign in</Link>
          </div>
          <div className="landing-hero-stats">
            <div><strong>10</strong><span>Microservices</span></div>
            <div><strong>3</strong><span>Role dashboards</span></div>
            <div><strong>24/7</strong><span>AI support</span></div>
          </div>
        </div>
      </section>

      {/* ── Features ────────────────────────────────────────── */}
      <section id="features" className="landing-section">
        <div className="landing-section-head">
          <span className="landing-eyebrow">Features</span>
          <h2>Everything your gym needs, in one place</h2>
          <p>Built for admins, trainers, and members alike.</p>
        </div>
        <div className="landing-grid">
          {FEATURES.map((f) => (
            <div className="landing-card" key={f.title}>
              <div className="landing-card-icon">{f.icon}</div>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── How it works ────────────────────────────────────── */}
      <section id="how-it-works" className="landing-section landing-section-alt">
        <div className="landing-section-head">
          <span className="landing-eyebrow">How it works</span>
          <h2>Get moving in three steps</h2>
        </div>
        <div className="landing-steps">
          {STEPS.map((s) => (
            <div className="landing-step" key={s.n}>
              <div className="landing-step-num">{s.n}</div>
              <h3>{s.title}</h3>
              <p>{s.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA banner ──────────────────────────────────────── */}
      <section id="about" className="landing-cta-banner">
        <h2>Ready to transform your gym?</h2>
        <p>Join GymPro today and take control of bookings, payments, and members.</p>
        <Link to="/register" className="btn-primary-lg">Get started for free</Link>
      </section>

      {/* ── Footer ──────────────────────────────────────────── */}
      <footer className="landing-footer">
        <div className="landing-logo">GYMPRO</div>
        <p>© {new Date().getFullYear()} GymPro. All rights reserved.</p>
      </footer>
    </div>
  );
}
