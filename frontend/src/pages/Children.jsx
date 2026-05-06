import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const Children = ({ currentUser }) => {
  const [childrenList, setChildrenList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [progressFilter, setProgressFilter] = useState('All');
  const [showAddModal, setShowAddModal] = useState(false);
  const [addError, setAddError] = useState('');
  const [newChild, setNewChild] = useState({ Name: '', Age: '', BaselineSkillLevel: 'Beginner', ParentEmail: '', ParentPassword: '', ChildUsername: '', ChildPassword: '' });

  const fetchChildren = () => {
    setLoading(true);
    let url = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/children/details`;
    if (currentUser && currentUser.role !== 'Admin') {
      url += `?role=${currentUser.role}&refId=${currentUser.refId}`;
    }
    fetch(url)
      .then(res => res.json())
      .then(data => {
        setChildrenList(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching children:', err);
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchChildren();
  }, [currentUser]);

  const handleAddChild = (e) => {
    e.preventDefault();
    setAddError('');
    
    const payload = { ...newChild };
    if (currentUser?.role === 'Parent') {
       payload.ParentID = currentUser.refId;
    }
    
    fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/children`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
      .then(async res => {
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to add child');
        return data;
      })
      .then(() => {
        setShowAddModal(false);
        setNewChild({ Name: '', Age: '', BaselineSkillLevel: 'Beginner', ParentEmail: '', ParentPassword: '', ChildUsername: '', ChildPassword: '' });
        fetchChildren();
      })
      .catch(err => setAddError(err.message));
  };

  const filteredChildren = childrenList.filter(child => {
    // Search filter
    if (searchTerm && !child.ChildName.toLowerCase().includes(searchTerm.toLowerCase())) {
      return false;
    }
    // Progress filter
    const acc = Number(child.AvgAccuracy) || 0;
    if (progressFilter === '<50' && acc >= 50) return false;
    if (progressFilter === '50-80' && (acc < 50 || acc > 80)) return false;
    if (progressFilter === '>80' && acc <= 80) return false;
    return true;
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1 className="page-title" style={{ marginBottom: 0 }}>Registered Children</h1>
        <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>+ Add Child</button>
      </div>

      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
        <input 
          type="text" 
          placeholder="Search by name..." 
          value={searchTerm} 
          onChange={e => setSearchTerm(e.target.value)}
          style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid #ccc', flex: 1 }}
        />
        <select 
          value={progressFilter} 
          onChange={e => setProgressFilter(e.target.value)}
          style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid #ccc' }}
        >
          <option value="All">All Progress</option>
          <option value="<50">&lt; 50%</option>
          <option value="50-80">50% - 80%</option>
          <option value=">80">&gt; 80%</option>
        </select>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
        {loading ? (
          <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '2rem' }}>Loading...</div>
        ) : filteredChildren.length === 0 ? (
          <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '2rem' }}>No children found.</div>
        ) : (
          filteredChildren.map((child, index) => {
            const acc = Number(child.AvgAccuracy) || 0;
            return (
              <div key={index} className="card" style={{ display: 'flex', flexDirection: 'column', padding: '1.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
                  <div style={{ 
                    width: '60px', height: '60px', borderRadius: '12px', 
                    backgroundColor: `hsl(${index * 40 + 10}, 80%, 60%)`, 
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '1.5rem', fontWeight: 'bold', color: 'white'
                  }}>
                    {child.ChildName.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <h3 style={{ margin: 0, fontSize: '1.2rem', color: 'var(--text-primary)' }}>{child.ChildName}</h3>
                    <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                      Age {child.Age} • {child.BaselineSkillLevel}
                    </div>
                  </div>
                </div>

                <div style={{ marginBottom: '1.5rem', flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginBottom: '0.5rem' }}>
                    <span>Accuracy</span>
                    <span style={{ fontWeight: 'bold', color: acc > 80 ? '#4ADE80' : acc > 50 ? 'var(--primary-color)' : '#F87171' }}>{acc.toFixed(1)}%</span>
                  </div>
                  <div style={{ width: '100%', height: '8px', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '4px', overflow: 'hidden' }}>
                    <div style={{ 
                      width: `${acc}%`, 
                      height: '100%', 
                      backgroundColor: acc > 80 ? '#4ADE80' : acc > 50 ? 'var(--primary-color)' : '#F87171',
                      borderRadius: '4px'
                    }}></div>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <Link to={`/sessions/${child.ChildID}`} className="btn btn-outline" style={{ flex: 1, padding: '0.5rem' }}>
                    Sessions
                  </Link>
                  <Link to={`/progress/${child.ChildID}`} className="btn btn-primary" style={{ flex: 1, padding: '0.5rem' }}>
                    Progress
                  </Link>
                </div>
              </div>
            );
          })
        )}
      </div>

      {showAddModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ backgroundColor: 'white', padding: '2rem', borderRadius: '8px', minWidth: '300px', color: '#1E293B', maxHeight: '90vh', overflowY: 'auto' }}>
            <h2 style={{ marginTop: 0, color: '#0F172A' }}>Add New Child</h2>
            {addError && <div style={{ backgroundColor: '#fee2e2', color: '#ef4444', padding: '0.75rem', borderRadius: '4px', marginBottom: '1rem', fontSize: '0.9rem' }}>{addError}</div>}
            <form onSubmit={handleAddChild} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              
              <fieldset style={{ border: '1px solid #e2e8f0', padding: '1rem', borderRadius: '4px' }}>
                <legend style={{ padding: '0 0.5rem', fontWeight: 'bold' }}>Child Info</legend>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  <input type="text" placeholder="Child Name *" value={newChild.Name} onChange={e => setNewChild({...newChild, Name: e.target.value})} required style={{ padding: '0.5rem', color: '#1E293B', border: '1px solid #ccc' }} />
                  <input type="number" placeholder="Age *" value={newChild.Age} onChange={e => setNewChild({...newChild, Age: e.target.value})} required style={{ padding: '0.5rem', color: '#1E293B', border: '1px solid #ccc' }} />
                  <select value={newChild.BaselineSkillLevel} onChange={e => setNewChild({...newChild, BaselineSkillLevel: e.target.value})} style={{ padding: '0.5rem', color: '#1E293B', border: '1px solid #ccc' }}>
                    <option value="Beginner">Beginner</option>
                    <option value="Intermediate">Intermediate</option>
                    <option value="Advanced">Advanced</option>
                  </select>
                </div>
              </fieldset>

              <fieldset style={{ border: '1px solid #e2e8f0', padding: '1rem', borderRadius: '4px' }}>
                <legend style={{ padding: '0 0.5rem', fontWeight: 'bold' }}>Child Login (Optional)</legend>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  <input type="text" placeholder="Child Username (auto-generated if empty)" value={newChild.ChildUsername} onChange={e => setNewChild({...newChild, ChildUsername: e.target.value})} style={{ padding: '0.5rem', color: '#1E293B', border: '1px solid #ccc' }} />
                  <input type="text" placeholder="Child Password (defaults to child123)" value={newChild.ChildPassword} onChange={e => setNewChild({...newChild, ChildPassword: e.target.value})} style={{ padding: '0.5rem', color: '#1E293B', border: '1px solid #ccc' }} />
                </div>
              </fieldset>

              {currentUser?.role !== 'Parent' && (
                <fieldset style={{ border: '1px solid #e2e8f0', padding: '1rem', borderRadius: '4px' }}>
                  <legend style={{ padding: '0 0.5rem', fontWeight: 'bold' }}>Parent Assignment</legend>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    <input type="email" placeholder="Parent's Email Address *" value={newChild.ParentEmail} onChange={e => setNewChild({...newChild, ParentEmail: e.target.value})} required style={{ padding: '0.5rem', color: '#1E293B', border: '1px solid #ccc' }} />
                    <input type="text" placeholder="Parent Account Password (if new)" value={newChild.ParentPassword} onChange={e => setNewChild({...newChild, ParentPassword: e.target.value})} style={{ padding: '0.5rem', color: '#1E293B', border: '1px solid #ccc' }} />
                  </div>
                </fieldset>
              )}
              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-outline" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Children;
