import React from 'react';
import Notification from './Notification';

const AddPage = ({ title, notification, setNotification, children, color = 'indigo', icon: Icon }) => {
  const colorMap = {
    indigo: 'text-indigo-600',
    emerald: 'text-emerald-600',
    amber: 'text-amber-600',
    violet: 'text-violet-600'
  };

  const bgMap = {
    indigo: 'bg-indigo-50',
    emerald: 'bg-emerald-50',
    amber: 'bg-amber-50',
    violet: 'bg-violet-50'
  };

  const iconBgMap = {
    indigo: 'bg-indigo-600',
    emerald: 'bg-emerald-600',
    amber: 'bg-amber-600',
    violet: 'bg-violet-600'
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}

      <div className="flex items-center gap-4 mb-8 animate-slide-in-left">
        {Icon && (
          <div className={`${iconBgMap[color]} text-white p-4 rounded-[1.5rem] shadow-lg`}>
            <Icon size={24} />
          </div>
        )}
        <h1 className={`text-4xl font-black ${colorMap[color]} tracking-tighter`}>
          {title}
        </h1>
      </div>

      <div className="bg-white rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 p-8 animate-fade-in">
        {children}
      </div>
    </div>
  );
};

export default AddPage;
