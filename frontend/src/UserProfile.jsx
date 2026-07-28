import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';
import { fetchWithCsrf } from './api';
import Notification from './Notification';
import { FaUserCircle, FaKey, FaShieldAlt, FaSave, FaExclamationTriangle, FaCheckCircle, FaLock } from 'react-icons/fa';

const UserProfile = () => {
  const { t } = useTranslation();
  const { user } = useAuth();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notification, setNotification] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!currentPassword) {
      setNotification({
        type: 'error',
        message: t('profile.changePassword.errors.currentRequired', 'Current password is required.')
      });
      return;
    }

    if (newPassword.length < 6) {
      setNotification({
        type: 'error',
        message: t('profile.changePassword.errors.minLength', 'New password must be at least 6 characters long.')
      });
      return;
    }

    if (newPassword !== confirmPassword) {
      setNotification({
        type: 'error',
        message: t('profile.changePassword.errors.match', 'New passwords do not match.')
      });
      return;
    }

    setIsSubmitting(true);
    setNotification(null);

    try {
      const response = await fetchWithCsrf('/api/user/change-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ currentPassword, newPassword }),
      });

      if (response.ok) {
        setNotification({
          type: 'success',
          message: t('profile.changePassword.success', 'Password updated successfully!')
        });
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
      } else {
        const data = await response.json().catch(() => ({}));
        setNotification({
          type: 'error',
          message: data.message || t('profile.changePassword.errors.failed', 'Failed to update password. Please verify current password.')
        });
      }
    } catch (err) {
      console.error('Password update error:', err);
      setNotification({
        type: 'error',
        message: t('profile.changePassword.errors.failed', 'Failed to update password. Please try again.')
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {notification && (
        <Notification
          type={notification.type}
          message={notification.message}
          onClose={() => setNotification(null)}
        />
      )}

      {/* Header Banner */}
      <div className="animate-slide-in-left mb-8">
        <div className="flex items-center gap-3 mb-2 text-indigo-600">
          <FaUserCircle className="text-2xl" />
          <span className="text-xs font-black uppercase tracking-[0.3em]">
            {t('profile.headerTag', 'Account Overview')}
          </span>
        </div>
        <h1 className="text-4xl font-black text-gray-800 tracking-tighter">
          {t('profile.titlePrefix', 'User')} <span className="text-indigo-600">{t('profile.titleSuffix', 'Profile')}</span>
        </h1>
        <p className="text-gray-400 font-bold mt-2">
          {t('profile.description', 'Manage your account security and user credentials.')}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* User Identity Info Card */}
        <div className="md:col-span-1 bg-white p-6 rounded-3xl border border-gray-100 shadow-sm flex flex-col items-center text-center">
          <div className="w-24 h-24 bg-gradient-to-tr from-indigo-500 to-violet-600 rounded-full flex items-center justify-center text-white text-4xl font-bold shadow-lg shadow-indigo-200 mb-4">
            {user?.username ? user.username.charAt(0).toUpperCase() : <FaUserCircle />}
          </div>
          <h2 className="text-xl font-black text-gray-800 mb-1">{user?.username}</h2>
          <div className="flex flex-wrap justify-center gap-1.5 mt-2">
            {user?.roles?.map((role) => (
              <span
                key={role}
                className="px-3 py-1 bg-indigo-50 text-indigo-700 font-black text-[10px] uppercase tracking-wider rounded-full border border-indigo-100 flex items-center gap-1"
              >
                <FaShieldAlt className="text-[9px]" />
                {role.replace('ROLE_', '')}
              </span>
            ))}
          </div>
        </div>

        {/* Change Password Form Card */}
        <div className="md:col-span-2 bg-white p-8 rounded-3xl border border-gray-100 shadow-sm">
          <div className="flex items-center gap-3 mb-6 pb-4 border-b border-gray-100">
            <div className="p-3 bg-indigo-50 text-indigo-600 rounded-2xl">
              <FaKey className="text-lg" />
            </div>
            <div>
              <h3 className="text-lg font-black text-gray-800">
                {t('profile.changePassword.title', 'Change Password')}
              </h3>
              <p className="text-xs font-bold text-gray-400">
                {t('profile.changePassword.subtitle', 'Ensure your account stays secure with a strong password')}
              </p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label
                htmlFor="currentPassword"
                className="block text-xs font-black text-gray-500 uppercase tracking-wider mb-2"
              >
                {t('profile.changePassword.currentPassword', 'Current Password')}
              </label>
              <div className="relative">
                <input
                  id="currentPassword"
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-2xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all text-sm font-medium pr-10"
                />
                <FaLock className="absolute right-3.5 top-3.5 text-gray-300 text-sm" />
              </div>
            </div>

            <div>
              <label
                htmlFor="newPassword"
                className="block text-xs font-black text-gray-500 uppercase tracking-wider mb-2"
              >
                {t('profile.changePassword.newPassword', 'New Password')}
              </label>
              <div className="relative">
                <input
                  id="newPassword"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-2xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all text-sm font-medium pr-10"
                />
                <FaKey className="absolute right-3.5 top-3.5 text-gray-300 text-sm" />
              </div>
            </div>

            <div>
              <label
                htmlFor="confirmPassword"
                className="block text-xs font-black text-gray-500 uppercase tracking-wider mb-2"
              >
                {t('profile.changePassword.confirmPassword', 'Confirm New Password')}
              </label>
              <div className="relative">
                <input
                  id="confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-2xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all text-sm font-medium pr-10"
                />
                <FaKey className="absolute right-3.5 top-3.5 text-gray-300 text-sm" />
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="submit"
                disabled={isSubmitting}
                className="flex items-center gap-2 px-8 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs uppercase tracking-wider rounded-2xl shadow-lg shadow-indigo-100 transition-all transform active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSubmitting ? (
                  <div className="animate-spin h-4 w-4 border-2 border-white/30 border-t-white rounded-full" />
                ) : (
                  <>
                    <FaSave />
                    {t('profile.changePassword.submit', 'Update Password')}
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default UserProfile;
