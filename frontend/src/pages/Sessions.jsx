import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';

const Sessions = () => {
  const { childId } = useParams();
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`http://localhost:5000/api/sessions/${childId}`)
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
        <h1 className="page-title" style={{ marginBottom: 0 }}>Sessions for Child #{childId}</h1>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Module</th>
              <th>Lesson</th>
              <th>Duration</th>
              <th>Status</th>
              <th>Score</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
               <tr><td colSpan="6" style={{ textAlign: 'center' }}>Loading...</td></tr>
            ) : sessions.length === 0 ? (
               <tr><td colSpan="6" style={{ textAlign: 'center' }}>No sessions found for this child.</td></tr>
            ) : (
              sessions.map((session, index) => (
                <tr key={index}>
                  <td>{new Date(session.SessionDate).toLocaleDateString()}</td>
                  <td>{session.ModuleName || 'N/A'}</td>
                  <td>{session.LessonTitle || 'N/A'}</td>
                  <td>{session.Duration} mins</td>
                  <td>
                    <span className="status-badge status-success">
                      Completed
                    </span>
                  </td>
                  <td style={{ fontWeight: 600 }}>N/A</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Sessions;
