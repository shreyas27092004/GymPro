// src/pages/TrainerDashboard.jsx

import { useState } from 'react';
import Sidebar from '../components/Sidebar';
import TrainerOverview from './trainer/TrainerOverview';
import TrainerMembers  from './trainer/TrainerMembers';
import TrainerSessions from './trainer/TrainerSessions';
import TrainerSchedule from './trainer/TrainerSchedule';
import TrainerProfile  from './trainer/TrainerProfile';

export default function TrainerDashboard() {
  const [active, setActive] = useState('overview');
  const views = {
    overview: <TrainerOverview onNav={setActive} />,
    members:  <TrainerMembers />,
    sessions: <TrainerSessions />,
    schedule: <TrainerSchedule />,
    profile:  <TrainerProfile />,
  };
  return (
    <div className="page">
      <Sidebar active={active} onNav={setActive} />
      <main className="main-area">{views[active] || views.overview}</main>
    </div>
  );
}
