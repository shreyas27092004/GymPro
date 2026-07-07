// src/pages/AdminDashboard.jsx

import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import AdminMembers  from './admin/Members';
import AdminTrainers from './admin/Trainers';
import AdminPlans    from './admin/Plans';
import AdminBookings from './admin/Bookings';
import AdminPayments from './admin/Payments';
import AdminOverview from './admin/Overview';
import AdminProfile  from './admin/AdminProfile';


export default function AdminDashboard() {
  const [active, setActive] = useState('overview');

  const views = {
    overview: <AdminOverview onNav={setActive} />,
    members:  <AdminMembers />,
    trainers: <AdminTrainers />,
    plans:    <AdminPlans />,
    bookings: <AdminBookings />,
    payments: <AdminPayments />,
    profile:  <AdminProfile />,
  };

  return (
    <div className="page">
      <Sidebar active={active} onNav={setActive} />
      <main className="main-area">
        {views[active] || views.overview}
      </main>
    </div>
  );
}
