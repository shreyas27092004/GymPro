// src/pages/MemberDashboard.jsx

import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import MemberOverview from './member/MemberOverview';
import MemberPlans    from './member/MemberPlans';
import MemberBookings from './member/MemberBookings';
import MemberTrainers from './member/MemberTrainers';
import MemberPayments from './member/MemberPayments';
import MemberProfile  from './member/MemberProfile';


export default function MemberDashboard() {
  const [active, setActive] = useState('overview');
  const views = {
    overview: <MemberOverview onNav={setActive} />,
    plans:    <MemberPlans />,
    bookings: <MemberBookings />,
    trainers: <MemberTrainers />,
    payments: <MemberPayments />,
    profile:  <MemberProfile />,
  };
  return (
    <div className="page">
      <Sidebar active={active} onNav={setActive} />
      <main className="main-area">{views[active] || views.overview}</main>
    </div>
  );
}
