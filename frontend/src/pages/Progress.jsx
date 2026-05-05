import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const Progress = () => {
  const { childId } = useParams();
  const [progress, setProgress] = useState(null);
  const [historyData, setHistoryData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`http://localhost:5000/api/progress/report/${childId}`)
      .then(res => res.json())
      .then(data => {
        let prog = null;
        if (Array.isArray(data) && data.length > 0) {
          prog = data[0];
        } else {
          prog = data;
        }
        
        setProgress(prog);

        if (prog && prog.SessionHistoryJSON) {
          try {
            const history = JSON.parse(prog.SessionHistoryJSON);
            const chartData = history.map(h => {
              const acc = h.TotalAttempts > 0 ? (h.CorrectAttempts / h.TotalAttempts) * 100 : 0;
              return {
                date: h.SessionDate,
                accuracy: Number(acc.toFixed(1)),
                category: h.ModuleName,
                activity: h.LessonTitle
              };
            }).reverse(); // chronological order
            setHistoryData(chartData);
          } catch (e) {
            console.error('Failed to parse SessionHistoryJSON:', e);
          }
        }
        
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching progress:', err);
        setLoading(false);
      });
  }, [childId]);

  const latestSession = historyData.length > 0 ? historyData[historyData.length - 1] : null;

  return (
    <div>
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1.5rem' }}>
        <Link to="/children" className="btn btn-outline" style={{ padding: '0.25rem 0.75rem' }}>&larr; Back</Link>
        <h1 className="page-title" style={{ marginBottom: 0 }}>
          Progress Report ({progress?.ChildName || `Child #${childId}`})
        </h1>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem' }}>
        {/* Main Column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          
          <div className="card">
            {loading ? (
              <div>Loading progress data...</div>
            ) : !progress || Object.keys(progress).length === 0 ? (
               <div>No progress data found.</div>
            ) : (
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem' }}>
                  <div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Total Sessions Attended</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{progress.TotalSessions || 0}</div>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Last Active</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                      {latestSession ? latestSession.date : 'N/A'}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Average Accuracy</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 600, color: 'var(--primary-color)' }}>
                      {progress.AverageAccuracy ? `${Number(progress.AverageAccuracy).toFixed(1)}%` : '0%'}
                    </div>
                  </div>
                </div>
                
                <div style={{ marginTop: '1.5rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <span style={{ fontWeight: 500 }}>Overall Completion</span>
                    <span style={{ fontWeight: 600 }}>{progress.OverallCompletion || 0}%</span>
                  </div>
                  
                  <div style={{ width: '100%', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '9999px', height: '0.75rem', overflow: 'hidden' }}>
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
              </div>
            )}
          </div>

          <div className="card">
            <h2 className="section-title" style={{ fontSize: '1.25rem', marginBottom: '1.5rem' }}>Session Accuracy Over Time</h2>
            {historyData.length > 0 ? (
              <div style={{ width: '100%', height: 300 }}>
                <ResponsiveContainer>
                  <LineChart data={historyData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                    <Line type="monotone" dataKey="accuracy" stroke="var(--primary-color)" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                    <CartesianGrid stroke="#334155" strokeDasharray="5 5" />
                    <XAxis dataKey="date" stroke="#94A3B8" />
                    <YAxis stroke="#94A3B8" domain={[0, 100]} />
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#1E293B', borderColor: '#334155', color: '#F8FAFC' }}
                      formatter={(value, name, props) => [`${value}%`, `Accuracy (${props.payload.category})`]}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <p style={{ color: 'var(--text-secondary)' }}>Not enough data to display graph.</p>
            )}
          </div>
          
          <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            * Data provided natively from your MySQL database using the `GetChildProgressReport` Stored Procedure.
          </div>
        </div>

        {/* Sidebar */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          
          <div className="card" style={{ padding: '1.5rem' }}>
            <h2 className="section-title" style={{ fontSize: '1.1rem', marginBottom: '1rem', color: 'var(--text-primary)' }}>Insights Panel</h2>
            {historyData.length > 0 ? (() => {
              const accuracies = historyData.map(h => h.accuracy);
              const bestSession = historyData.reduce((prev, current) => (prev.accuracy > current.accuracy) ? prev : current);
              const isImproving = accuracies.length > 1 && accuracies[accuracies.length - 1] >= accuracies[accuracies.length - 2];
              
              return (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div>
                    <strong style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Trend</strong>
                    <span style={{ color: isImproving ? '#4ADE80' : '#F87171', fontWeight: 'bold' }}>
                      {isImproving ? '📈 Improving recently' : '📉 Needs attention'}
                    </span>
                  </div>
                  <div>
                    <strong style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Best Session</strong>
                    <span>{bestSession.date} ({bestSession.accuracy}%)</span>
                  </div>
                  <div>
                    <strong style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Strongest Category</strong>
                    <span>{bestSession.category}</span>
                  </div>
                </div>
              );
            })() : (
              <p style={{ color: 'var(--text-secondary)' }}>No insights available yet.</p>
            )}
          </div>

          <div className="card" style={{ padding: '1.5rem', borderLeft: '4px solid var(--primary-color)' }}>
            <h2 className="section-title" style={{ fontSize: '1.1rem', marginBottom: '1rem', color: 'var(--text-primary)' }}>Teacher Remarks</h2>
            <p style={{ fontStyle: 'italic', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
              "{(progress?.ChildName || 'The student')} is making steady progress. Let's focus more on the challenging modules next week to improve overall consistency. Great effort so far!"
            </p>
          </div>
        </div>
        
      </div>
    </div>
  );
};

export default Progress;
