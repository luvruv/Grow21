import { useState, useEffect } from 'react';

const ProfileSelector = ({ currentUser, onSelectChild }) => {
  const [childrenList, setChildrenList] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    let url = 'http://localhost:5000/api/children/details';
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
  }, [currentUser]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 'calc(100vh - 4rem)' }}>
      <h1 style={{ fontSize: '3rem', fontWeight: 600, marginBottom: '3rem', color: '#fff' }}>Who's learning?</h1>
      
      {loading ? (
        <div>Loading profiles...</div>
      ) : (
        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', justifyContent: 'center', maxWidth: '800px' }}>
          {childrenList.map((child, index) => (
            <div 
              key={index} 
              onClick={() => onSelectChild(child)}
              style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', cursor: 'pointer', group: 'hover', transition: 'transform 0.2s' }}
              className="profile-card"
            >
              <div style={{ 
                width: '120px', height: '120px', 
                borderRadius: '16px', 
                backgroundColor: `hsl(${index * 40 + 10}, 80%, 60%)`, 
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '3rem', fontWeight: 'bold', color: 'white',
                boxShadow: '0 4px 10px rgba(0,0,0,0.3)',
                marginBottom: '1rem',
                border: '4px solid transparent',
                transition: 'all 0.2s'
              }} className="profile-avatar">
                {child.ChildName.charAt(0).toUpperCase()}
              </div>
              <span style={{ fontSize: '1.25rem', color: '#cbd5e1', fontWeight: 500 }} className="profile-name">
                {child.ChildName}
              </span>
            </div>
          ))}
          
          <div 
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', cursor: 'pointer' }}
            className="profile-card"
            onClick={() => {
              // Trigger add child modal if it was passed, or redirect
              window.location.href = "/children?add=true"; 
            }}
          >
            <div style={{ 
              width: '120px', height: '120px', 
              borderRadius: '16px', 
              backgroundColor: 'transparent', 
              border: '4px solid #334155',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '3rem', color: '#94a3b8',
              marginBottom: '1rem',
              transition: 'all 0.2s'
            }} className="profile-avatar add-avatar">
              +
            </div>
            <span style={{ fontSize: '1.25rem', color: '#94a3b8', fontWeight: 500 }} className="profile-name">
              Add Child
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProfileSelector;
