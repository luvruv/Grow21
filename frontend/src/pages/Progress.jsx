import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';

const Progress = () => {
  const { childId } = useParams();
  const [progress, setProgress] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // We try to call the SP first, else fallback to standard progress fetch
    fetch(`http://localhost:5000/api/progress/report/${childId}`)
      .then(res => res.json())
      .then(data => {
        // Handle stored procedure result formats
        if (Array.isArray(data) && data.length > 0) {
          setProgress(data[0]);
        } else {
          setProgress(data);
        }
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching progress:', err);
        setLoading(false);
      });
  }, [childId]);

  return (
    <div>
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1.5rem' }}>
        <Link to="/children" className="btn btn-outline" style={{ padding: '0.25rem 0.75rem' }}>&larr; Back</Link>
        <h1 className="page-title" style={{ marginBottom: 0 }}>Progress Report (Child #{childId})</h1>
      </div>

      <div className="card" style={{ maxWidth: '600px' }}>
        {loading ? (
          <div>Loading progress data...</div>
        ) : !progress || Object.keys(progress).length === 0 ? (
           <div>No progress data found by the Stored Procedure for this child.</div>
        ) : (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem' }}>
              <div>
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Total Sessions Attended</div>
                <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{progress.TotalSessions || progress.session_count || 0}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Average Accuracy</div>
                <div style={{ fontSize: '1.5rem', fontWeight: 600, color: 'var(--primary-color)' }}>
                  {progress.AverageAccuracy || progress.avg_accuracy ? `${Number(progress.AverageAccuracy || progress.avg_accuracy).toFixed(1)}%` : '0%'}
                </div>
              </div>
            </div>
            
            <div style={{ marginTop: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span style={{ fontWeight: 500 }}>Overall Completion</span>
                <span style={{ fontWeight: 600 }}>{progress.OverallCompletion || 0}%</span>
              </div>
              
              {/* Simple CSS Progress Bar */}
              <div style={{ width: '100%', backgroundColor: '#E2E8F0', borderRadius: '9999px', height: '0.75rem', overflow: 'hidden' }}>
                <div 
                  style={{ 
                    width: `${progress.OverallCompletion || 0}%`, 
                    backgroundColor: 'var(--primary-color)', 
                    height: '100%',
                    transition: 'width 1s ease-in-out'
                  }} 
                />
              </div>
            </div>
            
            <div style={{ marginTop: '2rem', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              * Data provided natively from your MySQL database using the `GetChildProgressReport` Stored Procedure.
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Progress;
