import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FaTools, FaTrashAlt, FaShieldAlt, FaHdd, FaInbox } from 'react-icons/fa';
import { fetchWithCsrf } from './api';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';

const MaintenancePanel = () => {
  const { t } = useTranslation();
  const [isPurging, setIsPurging] = useState(false);
  const [notification, setNotification] = useState(null);
  const [showPurgeConfirm, setShowPurgeConfirm] = useState(false);
  const [stagingStats, setStagingStats] = useState(null);
  const [loadingStats, setLoadingStats] = useState(true);

  const fetchStagingStats = async () => {
    try {
      const response = await fetch('/api/admin/maintenance/staging-stats');
      if (response.ok) {
        const data = await response.json();
        setStagingStats(data);
      }
    } catch (err) {
      console.error('Failed to fetch staging stats', err);
    } finally {
      setLoadingStats(false);
    }
  };

  useEffect(() => {
    fetchStagingStats();
  }, []);

  const handlePurgeStaging = async () => {
    setIsPurging(true);
    setShowPurgeConfirm(false);
    try {
      const response = await fetchWithCsrf('/api/admin/maintenance/purge-staging', {
        method: 'POST',
      });

      if (response.ok) {
        const count = await response.json();
        setNotification({ 
          type: 'success', 
          message: t('admin.maintenance.staging.success', { count }) 
        });
        fetchStagingStats();
      } else {
        setNotification({ type: 'error', message: t('admin.maintenance.staging.failure') });
      }
    } catch (err) {
      console.error('Failed to purge staging', err);
      setNotification({ type: 'error', message: t('admin.maintenance.staging.failure') });
    } finally {
      setIsPurging(false);
    }
  };

  const hasExpiredItems = stagingStats?.expiredItems > 0;

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {notification && (
        <Notification
          type={notification.type}
          message={notification.message}
          onClose={() => setNotification(null)}
        />
      )}

      <div className="animate-slide-in-left mb-12">
        <div className="flex items-center gap-3 mb-2 text-indigo-600">
          <FaTools className="text-xl" />
          <span className="text-xs font-black uppercase tracking-[0.3em]">{t('admin.dashboard')}</span>
        </div>
        <h1 className="text-4xl font-black text-gray-800 tracking-tighter">
          System <span className="text-indigo-600">Maintenance</span>
        </h1>
        <p className="text-gray-400 font-bold mt-2">{t('admin.maintenance.description')}</p>
      </div>

      <div className="grid grid-cols-1 gap-8">
        <div className="bg-white rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 p-8 animate-fade-in relative overflow-hidden group">
          <div className="absolute -right-4 -top-4 opacity-5 transform group-hover:scale-110 transition-transform duration-700">
            <FaInbox size={160} />
          </div>
          
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-8 relative z-10">
            <div className="flex items-start gap-6">
              <div className="p-4 rounded-3xl bg-rose-50 text-rose-600 shadow-inner">
                <FaTrashAlt size={32} />
              </div>
              <div className="space-y-4">
                <div>
                  <h2 className="text-2xl font-black text-gray-800 tracking-tight mb-1">
                    {t('admin.maintenance.staging.title')}
                  </h2>
                  <p className="text-sm font-bold text-gray-400 max-w-md leading-relaxed">
                    {t('admin.maintenance.staging.description')}
                  </p>
                </div>

                <div className="flex flex-wrap gap-4">
                  <div className="bg-gray-50 px-4 py-3 rounded-2xl border border-gray-100 min-w-[140px]">
                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">{t('admin.maintenance.staging.totalItems')}</p>
                    <p className="text-xl font-black text-gray-700">{loadingStats ? '...' : stagingStats?.totalItems || 0}</p>
                  </div>
                  <div className={`px-4 py-3 rounded-2xl border min-w-[140px] transition-all ${hasExpiredItems ? 'bg-amber-50 border-amber-100 animate-pulse-slow' : 'bg-gray-50 border-gray-100'}`}>
                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">{t('admin.maintenance.staging.expiredItems')}</p>
                    <p className={`text-xl font-black ${hasExpiredItems ? 'text-amber-600' : 'text-gray-700'}`}>
                      {loadingStats ? '...' : stagingStats?.expiredItems || 0}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <button
              onClick={() => setShowPurgeConfirm(true)}
              disabled={isPurging || !hasExpiredItems}
              className={`flex items-center justify-center gap-3 px-10 py-5 rounded-2xl font-black text-sm uppercase tracking-wider transition-all transform active:scale-95 shadow-xl ${
                isPurging || !hasExpiredItems
                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none' 
                  : 'bg-rose-600 text-white shadow-rose-100 hover:bg-rose-700 hover:-translate-y-1'
              }`}
            >
              {isPurging ? (
                <div className="animate-spin h-5 w-5 border-2 border-white/30 border-t-white rounded-full"></div>
              ) : (
                <>
                  <FaTrashAlt />
                  {t('admin.maintenance.staging.button')}
                </>
              )}
            </button>
          </div>
        </div>

        <div className="bg-white rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 p-8 animate-fade-in [animation-delay:100ms] opacity-50 relative overflow-hidden">
          <div className="absolute inset-0 bg-gray-50/50 backdrop-blur-[1px] flex items-center justify-center z-10">
            <div className="bg-white px-4 py-2 rounded-full border border-gray-200 shadow-sm flex items-center gap-2">
              <FaShieldAlt className="text-indigo-600" />
              <span className="text-[10px] font-black uppercase tracking-widest text-gray-500">Coming in TASK-250</span>
            </div>
          </div>
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="flex items-start gap-4 text-gray-400">
              <div className="p-4 rounded-2xl bg-gray-50">
                <FaHdd size={24} />
              </div>
              <div>
                <h2 className="text-xl font-black tracking-tight mb-1">Deep Storage Scan</h2>
                <p className="text-sm font-bold max-w-md">Perform a full verification of the physical storage against database records.</p>
              </div>
            </div>
            <button disabled className="px-8 py-4 bg-gray-100 text-gray-400 rounded-2xl font-black text-sm uppercase tracking-wider cursor-not-allowed">
              Start Scan
            </button>
          </div>
        </div>
      </div>

      {showPurgeConfirm && (
        <ConfirmationDialog
          onCancel={() => setShowPurgeConfirm(false)}
          onConfirm={handlePurgeStaging}
          title={t('admin.maintenance.staging.confirmTitle')}
          message={t('admin.maintenance.staging.confirmMessage')}
          confirmButtonText={t('admin.maintenance.staging.button')}
          confirmColor="bg-rose-600"
        />
      )}
    </div>
  );
};

export default MaintenancePanel;
