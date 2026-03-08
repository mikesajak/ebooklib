import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FaUserPlus, FaShieldAlt, FaKey, FaCopy, FaCheck, FaExclamationTriangle } from 'react-icons/fa';
import { fetchWithCsrf } from './api';

const AddUserModal = ({ isOpen, onClose, onSuccess }) => {
  const { t } = useTranslation();
  const [username, setUsername] = useState('');
  const [isAdmin, setIsAdmin] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdSecret, setCreatedSecret] = useState(null);
  const [copied, setCreatedCopied] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    const roles = ['USER'];
    if (isAdmin) roles.push('ADMIN');

    try {
      const response = await fetchWithCsrf('/api/admin/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, roles }),
      });

      if (response.ok) {
        const data = await response.json();
        setCreatedSecret(data);
      }
    } catch (err) {
      console.error('Failed to create user', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCopyPassword = () => {
    if (createdSecret?.initialPassword) {
      navigator.clipboard.writeText(createdSecret.initialPassword);
      setCreatedCopied(true);
      setTimeout(() => setCreatedCopied(false), 2000);
    }
  };

  const handleFinalClose = () => {
    setCreatedSecret(null);
    setUsername('');
    setIsAdmin(false);
    onSuccess();
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-white w-full max-w-md rounded-[2.5rem] shadow-2xl overflow-hidden border border-gray-100 transform transition-all animate-scale-in">
        {!createdSecret ? (
          <form onSubmit={handleSubmit}>
            <div className="bg-violet-600 p-8 text-center text-white relative">
              <div className="bg-white/20 w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-4 backdrop-blur-md border border-white/30 shadow-lg">
                <FaUserPlus className="text-2xl" />
              </div>
              <h2 className="text-2xl font-black tracking-tight">{t('admin.users.add.title')}</h2>
            </div>

            <div className="p-8 space-y-6">
              <div>
                <label className="block text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2" htmlFor="new-username">
                  {t('admin.users.add.username')}
                </label>
                <input
                  id="new-username"
                  type="text"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-violet-500 focus:bg-white transition-all font-medium"
                  placeholder={t('admin.users.add.usernamePlaceholder')}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  disabled={isSubmitting}
                />
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-2xl border border-gray-100">
                <div className="flex items-center gap-3">
                  <div className={`p-2 rounded-lg ${isAdmin ? 'bg-rose-100 text-rose-600' : 'bg-violet-100 text-violet-600'}`}>
                    <FaShieldAlt />
                  </div>
                  <div>
                    <p className="text-xs font-black text-gray-700 uppercase tracking-tight">{t('admin.users.add.adminLabel', 'Administrator')}</p>
                    <p className="text-[10px] text-gray-400 font-bold leading-tight">{t('admin.users.add.adminDescription', 'Grant elevated permissions')}</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setIsAdmin(!isAdmin)}
                  className={`w-12 h-6 rounded-full transition-all relative ${isAdmin ? 'bg-rose-500' : 'bg-gray-200'}`}
                >
                  <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-all ${isAdmin ? 'left-7' : 'left-1'}`}></div>
                </button>
              </div>
            </div>

            <div className="p-8 pt-0 flex gap-3">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-3 px-6 font-bold text-gray-500 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 transition-all text-sm"
                disabled={isSubmitting}
              >
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className={`flex-1 py-3 px-6 font-black text-white rounded-xl shadow-lg transition-all flex items-center justify-center gap-2 transform active:scale-95 ${
                  isSubmitting ? 'bg-violet-400 cursor-not-allowed' : 'bg-violet-600 hover:bg-violet-700 shadow-violet-100'
                }`}
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <div className="animate-spin h-5 w-5 border-2 border-white/30 border-t-white rounded-full"></div>
                ) : (
                  <>
                    <FaShieldAlt />
                    {t('admin.users.add.create')}
                  </>
                )}
              </button>
            </div>
          </form>
        ) : (
          <div className="animate-fade-in">
            <div className="bg-rose-600 p-8 text-center text-white relative">
              <div className="bg-white/20 w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-4 backdrop-blur-md border border-white/30 shadow-lg">
                <FaKey className="text-2xl" />
              </div>
              <h2 className="text-2xl font-black tracking-tight">{t('admin.users.add.successTitle')}</h2>
            </div>

            <div className="p-8 space-y-6 text-center">
              <div className="p-4 bg-amber-50 border border-amber-100 rounded-2xl flex gap-3 text-left">
                <FaExclamationTriangle className="text-amber-500 mt-1 flex-shrink-0" />
                <p className="text-xs font-bold text-amber-800 leading-relaxed">
                  {t('admin.users.add.passwordWarning')}
                </p>
              </div>

              <div className="relative group">
                <div className="absolute inset-0 bg-violet-600 blur-xl opacity-10 group-hover:opacity-20 transition-opacity"></div>
                <div className="relative bg-gray-50 p-6 rounded-2xl border border-gray-200 flex items-center justify-center gap-4">
                  <span className="text-2xl font-black text-gray-800 tracking-widest font-mono">
                    {createdSecret.initialPassword}
                  </span>
                  <button
                    onClick={handleCopyPassword}
                    className={`p-3 rounded-xl transition-all ${copied ? 'bg-green-500 text-white' : 'bg-white text-violet-600 border border-violet-100 shadow-sm hover:border-violet-300'}`}
                  >
                    {copied ? <FaCheck /> : <FaCopy />}
                  </button>
                </div>
              </div>

              <button
                onClick={handleFinalClose}
                className="w-full py-4 px-6 font-black text-white bg-violet-600 rounded-2xl shadow-xl shadow-violet-100 hover:bg-violet-700 transition-all transform active:scale-95"
              >
                {t('admin.users.add.close')}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AddUserModal;
