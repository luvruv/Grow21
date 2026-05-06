import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const Dashboard = ({ currentUser, activeChild }) => {
  const [stats, setStats] = useState({
    total_children: 0,
    total_sessions: 0,
    avg_accuracy: 0
  });
  const [childProgress, setChildProgress] = useState(null);
  const [childSessions, setChildSessions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);

    if (currentUser?.role === 'Parent' && activeChild) {
      // Fetch child specific data
      Promise.all([
        fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/progress/report/${activeChild.ChildID}`).then(r => r.json()),
        fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/sessions/${activeChild.ChildID}`).then(r => r.json())
      ]).then(([progressData, sessionsData]) => {
        if (Array.isArray(progressData) && progressData.length > 0) {
          setChildProgress(progressData[0]);
        } else {
          setChildProgress(progressData);
        }
        setChildSessions(sessionsData.slice(0, 5)); // Last 5 sessions
        setLoading(false);
      }).catch(err => {
        console.error('Error fetching child dashboard:', err);
        setLoading(false);
      });
    } else {
      // General dashboard for Teachers/Admins
      let url = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/dashboard`;
      if (currentUser && currentUser.role !== 'Admin') {
        url += `?role=${currentUser.role}&refId=${currentUser.refId}`;
      }
      
      fetch(url)
        .then(res => res.json())
        .then(data => {
          setStats(data);
          setLoading(false);
        })
        .catch(err => {
          console.error('Error fetching dashboard stats:', err);
          setLoading(false);
        });
    }
  }, [currentUser, activeChild]);

  if (currentUser?.role === 'Parent' && activeChild) {
    const accuracy = childProgress?.AverageAccuracy || childProgress?.avg_accuracy || childProgress?.AccuracyRate || 0;
    const progressStatus = accuracy > 80 ? 'Excellent' : accuracy > 50 ? 'On Track' : 'Needs Support';
    
    return (
      <div>
        <h1 className="page-title">{activeChild.ChildName}'s Dashboard</h1>
        <div className="card-grid" style={{ marginBottom: '2rem' }}>
          <div className="card">
            <div className="card-title">Accuracy</div>
            <div className="card-value">{loading ? '...' : `${Number(accuracy).toFixed(1)}%`}</div>
          </div>
          <div className="card">
            <div className="card-title">Total Sessions</div>
            <div className="card-value">{loading ? '...' : (childProgress?.TotalSessions || childProgress?.session_count || 0)}</div>
          </div>
          <div className="card">
            <div className="card-title">Skill Level</div>
            <div className="card-value" style={{ fontSize: '1.8rem' }}>{activeChild.BaselineSkillLevel || 'Beginner'}</div>
          </div>
          <div className="card">
            <div className="card-title">Progress Status</div>
            <div className="card-value" style={{ fontSize: '1.8rem', color: accuracy > 80 ? '#4ADE80' : accuracy > 50 ? 'var(--primary-color)' : '#F87171' }}>
              {loading ? '...' : progressStatus}
            </div>
          </div>
        </div>

        <div className="card" style={{ marginTop: '2rem' }}>
          <h2 className="section-title" style={{ fontSize: '1.25rem', marginBottom: '1rem' }}>Recent Activity</h2>
          {childSessions.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)' }}>No recent activity found.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {childSessions.map((s, i) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
                  <div>
                    <div style={{ fontWeight: 'bold' }}>{s.LessonTitle || 'Unknown Lesson'}</div>
                    <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{s.ModuleName || 'Unknown Module'}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div>{new Date(s.SessionDate).toLocaleDateString()}</div>
                    <div style={{ fontSize: '0.875rem', color: 'var(--primary-color)' }}>{s.Duration} mins</div>
                  </div>
                </div>
              ))}
              <Link to={`/sessions/${activeChild.ChildID}`} style={{ color: 'var(--primary-color)', textAlign: 'center', marginTop: '1rem', textDecoration: 'none', fontWeight: 'bold' }}>
                View All Sessions &rarr;
              </Link>
            </div>
          )}
        </div>
      </div>
    );
  }

  // Teacher / Admin View
  return (
    <div>
      <h1 className="page-title">Educator Dashboard Overview</h1>
      <div className="card-grid" style={{ marginBottom: '2rem' }}>
        <div className="card">
          <div className="card-title">Total Assigned Children</div>
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

      {currentUser?.role === 'Teacher' && stats.activityStats && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '2rem', marginBottom: '2rem' }}>
          
          <div className="card">
            <h2 className="section-title">Activity Breakdown</h2>
            {stats.activityStats.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)' }}>No activity data found.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {stats.activityStats.map((act, i) => (
                  <div key={i} style={{ padding: '1rem', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                      <strong style={{ color: 'var(--text-primary)' }}>{act.Category}</strong>
                      <span style={{ color: 'var(--primary-color)', fontWeight: 'bold' }}>{act.accuracy}% Acc</span>
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{act.count} total sessions completed</div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <div className="card">
              <h2 className="section-title">Top Performers (&gt;80%)</h2>
              {stats.topPerformers?.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)' }}>No top performers currently.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {stats.topPerformers?.map(child => (
                    <div key={child.ChildID} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem', backgroundColor: 'rgba(74, 222, 128, 0.1)', borderRadius: '8px', border: '1px solid rgba(74, 222, 128, 0.2)' }}>
                      <Link to={`/progress/${child.ChildID}`} style={{ color: 'white', textDecoration: 'none', fontWeight: 500 }}>{child.ChildName}</Link>
                      <span style={{ color: '#4ADE80', fontWeight: 'bold' }}>{child.AccuracyPercent}%</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="card">
              <h2 className="section-title">Needs Attention (&lt;50%)</h2>
              {stats.weakStudents?.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)' }}>All students are on track!</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {stats.weakStudents?.map(child => (
                    <div key={child.ChildID} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem', backgroundColor: 'rgba(248, 113, 113, 0.1)', borderRadius: '8px', border: '1px solid rgba(248, 113, 113, 0.2)' }}>
                      <Link to={`/progress/${child.ChildID}`} style={{ color: 'white', textDecoration: 'none', fontWeight: 500 }}>{child.ChildName}</Link>
                      <span style={{ color: '#F87171', fontWeight: 'bold' }}>{child.AccuracyPercent}%</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

        </div>
      )}
    </div>
  );
};

export default Dashboard;
