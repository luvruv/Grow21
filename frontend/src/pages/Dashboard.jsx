import { useState, useEffect } from 'react';

const Dashboard = () => {
  const [stats, setStats] = useState({
    total_children: 0,
    total_sessions: 0,
    avg_accuracy: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Fetch stats from backend
    fetch('http://localhost:5000/api/dashboard')
      .then(res => res.json())
      .then(data => {
        setStats(data);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching dashboard stats:', err);
        setLoading(false); // To show UI even if backend isn't connected yet
      });
  }, []);

  return (
    <div>
      <h1 className="page-title">Dashboard Overview</h1>
      <div className="card-grid">
        <div className="card">
          <div className="card-title">Total Children</div>
          <div className="card-value">{loading ? '...' : stats.total_children}</div>
        </div>
        <div className="card">
          <div className="card-title">Total Sessions</div>
          <div className="card-value">{loading ? '...' : stats.total_sessions}</div>
        </div>
        <div className="card">
          <div className="card-title">Avg Accuracy</div>
          <div className="card-value">{loading ? '...' : `${Number(stats.avg_accuracy).toFixed(1)}%`}</div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
