import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';
import { fetchWithCsrf } from './api';
import { FaLock, FaUser, FaSignInAlt } from 'react-icons/fa';

const LoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();

  const from = location.state?.from?.pathname || '/';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      const params = new URLSearchParams();
      params.append('username', username);
      params.append('password', password);

      const response = await fetchWithCsrf('/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params,
      });

      if (response.ok) {
        await login(); // Refresh auth state in context
        navigate(from, { replace: true });
      } else {
        setError(t('auth.loginError'));
      }
    } catch (err) {
      console.error('Login error:', err);
      setError(t('auth.loginError'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] px-4">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden border border-gray-100 animate-fade-in">
        <div className="bg-indigo-600 p-8 text-center text-white relative">
          <div className="absolute top-0 left-0 w-full h-full opacity-10 bg-[url('/assets/book.jpg')] bg-cover bg-center mix-blend-overlay"></div>
          <div className="bg-white/20 w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-4 backdrop-blur-md border border-white/30 shadow-lg">
            <FaLock className="text-2xl" />
          </div>
          <h2 className="text-2xl font-black tracking-tight">{t('auth.loginTitle')}</h2>
          <p className="text-indigo-100 text-xs mt-1 font-bold uppercase tracking-widest opacity-80">{t('auth.libraryAccess')}</p>
        </div>

        <div className="p-8">
          {error && (
            <div className="p-4 mb-6 text-sm font-bold text-red-700 bg-red-50 rounded-2xl border border-red-100 flex items-center gap-3 animate-shake">
              <div className="bg-red-500 text-white p-1.5 rounded-lg text-xs"><FaSignInAlt /></div>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="flex items-center gap-2 mb-2 text-[10px] font-black text-gray-400 uppercase tracking-widest" htmlFor="username">
                <FaUser className="text-indigo-500" /> {t('auth.username')}
              </label>
              <input
                className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all font-medium text-gray-700"
                type="text"
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                disabled={isSubmitting}
                placeholder={t('auth.usernamePlaceholder')}
              />
            </div>

            <div>
              <label className="flex items-center gap-2 mb-2 text-[10px] font-black text-gray-400 uppercase tracking-widest" htmlFor="password">
                <FaLock className="text-indigo-500" /> {t('auth.password')}
              </label>
              <input
                className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all font-medium text-gray-700"
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                disabled={isSubmitting}
                placeholder="••••••••"
              />
            </div>

            <div className="flex flex-col gap-3 pt-2">
              <button
                className={`w-full py-3 px-6 font-black text-white rounded-xl shadow-lg transition-all flex items-center justify-center gap-2 transform active:scale-95 ${
                  isSubmitting ? 'bg-indigo-400 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700 hover:shadow-indigo-200'
                }`}
                type="submit"
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <div className="animate-spin h-5 w-5 border-2 border-white/30 border-t-white rounded-full"></div>
                ) : (
                  <>
                    <FaSignInAlt />
                    {t('auth.loginButton')}
                  </>
                )}
              </button>
              <button
                className="w-full py-3 px-6 font-bold text-gray-500 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 hover:text-gray-700 transition-all text-sm"
                type="button"
                onClick={handleCancel}
                disabled={isSubmitting}
              >
                {t('common.cancel')}
              </button>
            </div>
          </form>
        </div>
        
        <div className="p-6 bg-gray-50 border-t border-gray-100 text-center">
          <p className="text-[10px] text-gray-400 font-bold uppercase tracking-tighter">
            {t('common.footer')}
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
