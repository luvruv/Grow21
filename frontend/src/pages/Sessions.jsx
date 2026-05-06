import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';

const Sessions = () => {
  const { childId } = useParams();
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/sessions/${childId}`)
      .then(res => res.json())
      .then(data => {
        setSessions(data);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching sessions:', err);
        setLoading(false);
      });
  }, [childId]);

  return (
    <div>
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1.5rem' }}>
        <Link to="/children" className="btn btn-outline" style={{ padding: '0.25rem 0.75rem' }}>&larr; Back</Link>
        <h1 className="page-title" style={{ marginBottom: 0 }}>
          Sessions for {sessions.length > 0 && sessions[0].ChildName ? sessions[0].ChildName : `Child #${childId}`}
        </h1>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Activity Category</th>
              <th>Activity Name</th>
              <th>Time Spent</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
               <tr><td colSpan="5" style={{ textAlign: 'center' }}>Loading...</td></tr>
            ) : sessions.length === 0 ? (
               <tr><td colSpan="5" style={{ textAlign: 'center' }}>No activities found for this child.</td></tr>
            ) : (
              sessions.map((session, index) => {
                // Determine completion status
                let statusBadge = "status-badge ";
                let statusText = "Unknown";
                
                if (session.CompletionPercentage === 100) {
                  statusBadge += "status-success";
                  statusText = "Completed";
                } else if (session.CompletionPercentage > 0) {
                  statusBadge += "status-warning";
                  statusText = `${session.CompletionPercentage}%`;
                } else {
                  statusBadge += "status-danger";
                  statusText = "Incomplete";
                }

                return (
                  <tr key={index}>
                    <td>{new Date(session.SessionDate).toLocaleDateString()}</td>
                    <td>{session.ModuleName || 'N/A'}</td>
                    <td>{session.LessonTitle || 'N/A'}</td>
                    <td>{session.Duration} mins</td>
                    <td>
                      <span className={statusBadge} style={{ padding: '4px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 'bold' }}>
                        {statusText}
                      </span>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Sessions;
