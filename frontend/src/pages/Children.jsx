import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const Children = () => {
  const [childrenList, setChildrenList] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('http://localhost:5000/api/children/details')
      .then(res => res.json())
      .then(data => {
        setChildrenList(data);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching children:', err);
        setLoading(false);
      });
  }, []);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1 className="page-title" style={{ marginBottom: 0 }}>Registered Children</h1>
        <button className="btn btn-primary">+ Add Child</button>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Child Name</th>
              <th>Age</th>
              <th>Grade</th>
              <th>Parent Name</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="6" style={{ textAlign: 'center' }}>Loading...</td></tr>
            ) : childrenList.length === 0 ? (
              <tr><td colSpan="6" style={{ textAlign: 'center' }}>No children found or database not connected.</td></tr>
            ) : (
              childrenList.map((child, index) => (
                <tr key={index}>
                  <td>{child.ChildID}</td>
                  <td style={{ fontWeight: 500 }}>{child.ChildName}</td>
                  <td>{child.Age} yrs</td>
                  <td>{child.BaselineSkillLevel}</td>
                  <td>
                    {child.ParentName ? child.ParentName : 'No Parent Assigned'}
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{child.Email}</div>
                  </td>
                  <td>
                    <Link to={`/sessions/${child.ChildID}`} className="btn btn-outline" style={{ marginRight: '0.5rem' }}>
                      Sessions
                    </Link>
                    <Link to={`/progress/${child.ChildID}`} className="btn btn-outline">
                      Progress
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Children;
