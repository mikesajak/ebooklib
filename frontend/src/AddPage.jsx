import React from 'react';
import Notification from './Notification';

const AddPage = ({ title, notification, setNotification, children }) => {
  return (
    <div className="container mx-auto p-4">
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
      <h1 className="text-2xl font-bold mb-4">{title}</h1>
      <div className="bg-white border border-gray-300 rounded p-6 shadow">
        {children}
      </div>
    </div>
  );
};

export default AddPage;
