import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FaUserShield, FaUserPlus, FaTrashAlt, FaCheckCircle, FaTimesCircle, FaShieldAlt, FaToggleOn, FaToggleOff } from 'react-icons/fa';
import { fetchWithCsrf } from './api';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';
import AddUserModal from './AddUserModal';

const UserManagement = () => {
  const { t } = useTranslation();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  const fetchUsers = async () => {
    try {
      const response = await fetch('/api/admin/users');
      if (response.ok) {
        const data = await response.json();
        setUsers(data);
      }
    } catch (err) {
      console.error('Failed to fetch users', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleToggleAdmin = async (user) => {
    const isCurrentlyAdmin = user.roles.includes('ADMIN') || user.roles.includes('ROLE_ADMIN');
    const newRoles = isCurrentlyAdmin 
      ? user.roles.filter(r => r !== 'ADMIN' && r !== 'ROLE_ADMIN')
      : [...user.roles, 'ADMIN'];

    try {
      const response = await fetchWithCsrf(`/api/admin/users/${user.id}/roles`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newRoles),
      });

      if (response.ok) {
        fetchUsers();
      } else {
        setNotification({ type: 'error', message: 'Failed to update roles.' });
      }
    } catch (err) {
      setNotification({ type: 'error', message: 'Failed to update roles.' });
    }
  };

  const handleDeleteUser = async () => {
    if (!deleteConfirm) return;
    try {
      const response = await fetchWithCsrf(`/api/admin/users/${deleteConfirm.id}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        setNotification({ type: 'success', message: t('admin.users.deleteSuccess') });
        fetchUsers();
      } else {
        setNotification({ type: 'error', message: t('admin.users.deleteFailure') });
      }
    } catch (err) {
      setNotification({ type: 'error', message: t('admin.users.deleteFailure') });
    } finally {
      setDeleteConfirm(null);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-100 border-t-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {notification && (
        <Notification
          type={notification.type}
          message={notification.message}
          onClose={() => setNotification(null)}
        />
      )}

      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-12">
        <div className="animate-slide-in-left">
          <div className="flex items-center gap-3 mb-2 text-indigo-600">
            <FaUserShield className="text-xl" />
            <span className="text-xs font-black uppercase tracking-[0.3em]">{t('admin.dashboard')}</span>
          </div>
          <h1 className="text-4xl font-black text-gray-800 tracking-tighter">
            User <span className="text-indigo-600">Security</span>
          </h1>
        </div>

        <button
          onClick={() => setIsAddModalOpen(true)}
          className="flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white font-black rounded-2xl shadow-lg shadow-indigo-100 hover:bg-indigo-700 transition-all transform active:scale-95 animate-fade-in"
        >
          <FaUserPlus />
          {t('admin.users.addUser')}
        </button>
      </div>

      <div className="bg-white rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 overflow-hidden animate-fade-in">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50/50">
                <th className="px-8 py-5 text-[10px] font-black text-gray-400 uppercase tracking-widest">{t('admin.users.username')}</th>
                <th className="px-8 py-5 text-[10px] font-black text-gray-400 uppercase tracking-widest">{t('admin.users.roles')}</th>
                <th className="px-8 py-5 text-[10px] font-black text-gray-400 uppercase tracking-widest text-center">{t('admin.users.status')}</th>
                <th className="px-8 py-5 text-[10px] font-black text-gray-400 uppercase tracking-widest text-right">{t('admin.users.actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {users.length === 0 ? (
                <tr>
                  <td colSpan="4" className="px-8 py-12 text-center text-gray-400 font-bold italic">
                    {t('admin.users.noUsersFound')}
                  </td>
                </tr>
              ) : (
                users.map((user) => {
                  const isAdmin = user.roles.includes('ADMIN') || user.roles.includes('ROLE_ADMIN');
                  return (
                    <tr key={user.id} className="group hover:bg-indigo-50/30 transition-colors">
                      <td className="px-8 py-5">
                        <div className="flex items-center gap-3">
                          <div className={`w-10 h-10 rounded-full flex items-center justify-center font-black text-sm uppercase ${isAdmin ? 'bg-rose-100 text-rose-600' : 'bg-indigo-100 text-indigo-600'}`}>
                            {user.username.substring(0, 2)}
                          </div>
                          <span className="font-bold text-gray-700">{user.username}</span>
                        </div>
                      </td>
                      <td className="px-8 py-5">
                        <div className="flex items-center gap-4">
                          <div className="flex flex-wrap gap-2">
                            {user.roles.map((role) => (
                              <span
                                key={role}
                                className={`px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${
                                  role === 'ADMIN' || role === 'ROLE_ADMIN'
                                    ? 'bg-rose-100 text-rose-600'
                                    : 'bg-indigo-100 text-indigo-600'
                                }`}
                              >
                                {role.replace('ROLE_', '')}
                              </span>
                            ))}
                          </div>
                          <button 
                            onClick={() => handleToggleAdmin(user)}
                            className={`flex items-center gap-1.5 px-3 py-1 rounded-lg border text-[10px] font-black uppercase tracking-tight transition-all ${
                              isAdmin 
                                ? 'bg-rose-50 border-rose-100 text-rose-600 hover:bg-rose-100' 
                                : 'bg-indigo-50 border-indigo-100 text-indigo-600 hover:bg-indigo-100'
                            }`}
                          >
                            {isAdmin ? <FaToggleOn className="text-sm" /> : <FaToggleOff className="text-sm" />}
                            Admin
                          </button>
                        </div>
                      </td>
                      <td className="px-8 py-5">
                        <div className="flex justify-center">
                          {user.enabled ? (
                            <div className="flex items-center gap-1.5 text-green-500 font-black text-[10px] uppercase tracking-widest">
                              <FaCheckCircle /> {t('admin.users.active')}
                            </div>
                          ) : (
                            <div className="flex items-center gap-1.5 text-gray-400 font-black text-[10px] uppercase tracking-widest">
                              <FaTimesCircle /> {t('admin.users.disabled')}
                            </div>
                          )}
                        </div>
                      </td>
                      <td className="px-8 py-5 text-right">
                        <button
                          onClick={() => setDeleteConfirm(user)}
                          className="p-3 text-gray-300 hover:text-rose-500 hover:bg-rose-50 rounded-xl transition-all"
                          title={t('common.delete')}
                        >
                          <FaTrashAlt />
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      <AddUserModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSuccess={fetchUsers}
      />

      {deleteConfirm && (
        <ConfirmationDialog
          onCancel={() => setDeleteConfirm(null)}
          onConfirm={handleDeleteUser}
          title={t('admin.users.deleteConfirmTitle')}
          message={t('admin.users.deleteConfirmMessage', { username: deleteConfirm?.username })}
          confirmButtonText={t('common.delete')}
          confirmColor="bg-rose-600"
        />
      )}
    </div>
  );
};

export default UserManagement;
