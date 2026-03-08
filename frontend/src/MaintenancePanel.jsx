import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { FaTools, FaTrashAlt, FaHdd, FaInbox, FaSync, FaCheckCircle, FaExclamationCircle, FaChevronLeft } from 'react-icons/fa';
import { fetchWithCsrf } from './api';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';
import { useImport } from './ImportContext';

const MaintenancePanel = () => {
  const { t } = useTranslation();
  const { scanStats, refreshScanStats } = useImport();
  
  const [isPurgingStaging, setIsPurgingStaging] = useState(false);
  const [notification, setNotification] = useState(null);
  const [showStagingPurgeConfirm, setShowStagingPurgeConfirm] = useState(false);
  const [showStoragePurgeConfirm, setShowStoragePurgeConfirm] = useState(false);
  const [stagingStats, setStagingStats] = useState(null);
  const [loadingStaging, setLoadingStaging] = useState(true);

  // Local state for immediate UI feedback before SSE arrives
  const [isStartingScan, setIsStartingScan] = useState(false);
  const [isStartingPurge, setIsStartingPurge] = useState(false);

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
      setLoadingStaging(false);
    }
  };

  useEffect(() => {
    fetchStagingStats();
    refreshScanStats(); // Ensure we have latest stats on mount
  }, []);

  const handlePurgeStaging = async () => {
    setIsPurgingStaging(true);
    setShowStagingPurgeConfirm(false);
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
      setNotification({ type: 'error', message: t('admin.maintenance.staging.failure') });
    } finally {
      setIsPurgingStaging(false);
    }
  };

  const handleStartScan = async () => {
    setIsStartingScan(true);
    try {
      const response = await fetchWithCsrf('/api/admin/maintenance/storage-scan', {
        method: 'POST',
      });
      if (response.ok) {
        // SSE will provide the update
      }
    } catch (err) {
      console.error('Failed to start scan', err);
    } finally {
      setIsStartingScan(false);
    }
  };

  const handleStartStoragePurge = async () => {
    setIsStartingPurge(true);
    setShowStoragePurgeConfirm(false);
    try {
      const response = await fetchWithCsrf('/api/admin/maintenance/storage-purge', {
        method: 'POST',
      });
      if (response.ok) {
        // SSE will provide the update
      } else {
        setNotification({ type: 'error', message: t('admin.maintenance.scan.purgeFailure') });
      }
    } catch (err) {
      setNotification({ type: 'error', message: t('admin.maintenance.scan.purgeFailure') });
    } finally {
      setIsStartingPurge(false);
    }
  };

  const hasExpiredItems = stagingStats?.expiredItems > 0;
  const isScanRunning = scanStats?.status === 'RUNNING';
  const isPurgingStorageRunning = scanStats?.status === 'PURGING';
  const isOperationRunning = isScanRunning || isPurgingStorageRunning;

  const formatLastScan = () => {
    if (!scanStats || scanStats.status === 'IDLE') return t('admin.maintenance.scan.statusIdle');
    if (scanStats.status === 'FAILED') return t('admin.maintenance.scan.statusFailed', { error: scanStats.error });
    const date = scanStats.finishedAt || scanStats.startedAt;
    return t('admin.maintenance.scan.statusCompleted', { date: new Date(date).toLocaleString() });
  };

  const formatSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

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
          <Link to="/admin" className="text-xs font-black uppercase tracking-[0.3em] hover:text-indigo-800 transition-colors flex items-center gap-2 group/back">
            <FaChevronLeft className="text-[8px] transform group-hover/back:-translate-x-1 transition-transform" />
            {t('admin.backToDashboard')}
          </Link>
        </div>
        <h1 className="text-4xl font-black text-gray-800 tracking-tighter">
          {t('admin.maintenance.titlePrefix', 'System')} <span className="text-indigo-600">{t('admin.maintenance.titleSuffix', 'Maintenance')}</span>
        </h1>
        <p className="text-gray-400 font-bold mt-2">{t('admin.maintenance.description')}</p>
      </div>

      <div className="grid grid-cols-1 gap-8">
        {/* Staging Cleanup Card */}
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
                    <p className="text-xl font-black text-gray-700">{loadingStaging ? '...' : stagingStats?.totalItems || 0}</p>
                  </div>
                  <div className={`px-4 py-3 rounded-2xl border min-w-[140px] transition-all ${hasExpiredItems ? 'bg-amber-50 border-amber-100 animate-pulse-slow' : 'bg-gray-50 border-gray-100'}`}>
                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">{t('admin.maintenance.staging.expiredItems')}</p>
                    <p className={`text-xl font-black ${hasExpiredItems ? 'text-amber-600' : 'text-gray-700'}`}>
                      {loadingStaging ? '...' : stagingStats?.expiredItems || 0}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <button
              onClick={() => setShowStagingPurgeConfirm(true)}
              disabled={isPurgingStaging || !hasExpiredItems}
              className={`flex items-center justify-center gap-3 px-10 py-5 rounded-2xl font-black text-sm uppercase tracking-wider transition-all transform active:scale-95 shadow-xl ${
                isPurgingStaging || !hasExpiredItems
                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none' 
                  : 'bg-rose-600 text-white shadow-rose-100 hover:bg-rose-700 hover:-translate-y-1'
              }`}
            >
              {isPurgingStaging ? (
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

        {/* Deep Storage Scan Card */}
        <div className="bg-white rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 p-8 animate-fade-in [animation-delay:100ms] relative overflow-hidden group">
          <div className="absolute -right-4 -top-4 opacity-5 transform group-hover:scale-110 transition-transform duration-700">
            <FaHdd size={160} />
          </div>

          <div className="flex flex-col gap-8 relative z-10">
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-8">
              <div className="flex items-start gap-6">
                <div className={`p-4 rounded-3xl shadow-inner ${isOperationRunning ? 'bg-indigo-600 text-white animate-pulse' : 'bg-indigo-50 text-indigo-600'}`}>
                  <FaSync size={32} className={isOperationRunning ? 'animate-spin' : ''} />
                </div>
                <div className="space-y-2">
                  <h2 className="text-2xl font-black text-gray-800 tracking-tight mb-1">
                    {t('admin.maintenance.scan.title')}
                  </h2>
                  <p className="text-sm font-bold text-gray-400 max-w-md leading-relaxed">
                    {t('admin.maintenance.scan.description')}
                  </p>
                  <p className="text-[10px] font-black text-indigo-400 uppercase tracking-widest">{formatLastScan()}</p>
                </div>
              </div>

              <button
                onClick={handleStartScan}
                disabled={isOperationRunning || isStartingScan}
                className={`flex items-center justify-center gap-3 px-10 py-5 rounded-2xl font-black text-sm uppercase tracking-wider transition-all transform active:scale-95 shadow-xl ${
                  isOperationRunning || isStartingScan
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none' 
                    : 'bg-indigo-600 text-white shadow-indigo-100 hover:bg-indigo-700 hover:-translate-y-1'
                }`}
              >
                {isStartingScan ? (
                  <div className="animate-spin h-5 w-5 border-2 border-white/30 border-t-white rounded-full"></div>
                ) : (
                  <>
                    <FaSync />
                    {isOperationRunning ? t('admin.maintenance.scan.running') : t('admin.maintenance.scan.button')}
                  </>
                )}
              </button>
            </div>

            {/* Scan/Purge Progress & Results */}
            {(isOperationRunning || (scanStats?.status === 'COMPLETED')) && (
              <div className="mt-4 p-6 bg-gray-50 rounded-[2rem] border border-gray-100 animate-fade-in">
                <div className="flex flex-col md:flex-row justify-between items-center gap-6">
                  <div className="w-full md:w-1/2">
                    <div className="flex justify-between items-center mb-2">
                      <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest">
                        {isPurgingStorageRunning ? t('admin.maintenance.scan.purgeResults') : t('admin.maintenance.scan.progress')}
                      </span>
                      {!isScanRunning && (
                        <span className="text-sm font-black text-indigo-600">{scanStats.progressPercent}%</span>
                      )}
                    </div>
                    <div className="h-3 bg-white rounded-full overflow-hidden border border-gray-200 relative">
                      {isScanRunning ? (
                        /* Indeterminate progress for scan */
                        <div className="absolute inset-0 bg-indigo-600/20">
                          <div className="h-full bg-indigo-600 w-1/3 rounded-full animate-indeterminate-progress"></div>
                        </div>
                      ) : (
                        /* Deterministic progress for purge or completed state */
                        <div 
                          className="h-full bg-indigo-600 rounded-full transition-all duration-500"
                          style={{ width: `${scanStats.progressPercent}%` }}
                        ></div>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-wrap justify-center gap-6">
                    <div className="text-center min-w-[100px]">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">{t('admin.maintenance.scan.totalScanned')}</p>
                      <p className="text-xl font-black text-gray-700">{scanStats.totalFilesScanned}</p>
                      <p className="text-[9px] font-bold text-gray-400 uppercase">{formatSize(scanStats.totalScannedSize)}</p>
                    </div>
                    <div className="text-center min-w-[100px]">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">{t('admin.maintenance.scan.orphansFound')}</p>
                      <p className={`text-xl font-black ${scanStats.orphanedFilesFound > 0 ? 'text-rose-600' : 'text-green-600'}`}>
                        {scanStats.orphanedFilesFound}
                      </p>
                      <p className="text-[9px] font-bold text-gray-400 uppercase">{formatSize(scanStats.orphanedSize)}</p>
                    </div>
                  </div>
                </div>

                {scanStats.status === 'COMPLETED' && scanStats.orphanedFilesFound === 0 && (
                  <div className="mt-6 flex items-center gap-2 text-green-600 text-xs font-bold justify-center">
                    <FaCheckCircle /> {t('admin.maintenance.scan.noOrphans')}
                  </div>
                )}

                {scanStats.status === 'COMPLETED' && scanStats.orphanedFilesFound > 0 && (
                  <div className="mt-6 p-6 bg-rose-50 border border-rose-100 rounded-[2rem] flex flex-col md:flex-row justify-between items-start gap-6">
                    <div className="flex-1 w-full">
                      <div className="flex items-center gap-2 text-rose-600 text-xs font-black uppercase tracking-widest mb-3">
                        <FaExclamationCircle /> {t('admin.maintenance.scan.results')}
                      </div>
                      <div className="max-h-40 overflow-y-auto space-y-1 pr-2 custom-scrollbar">
                        {scanStats.orphanedFileKeys.map(key => (
                          <div key={key} className="text-[10px] font-mono text-rose-800 bg-white/50 px-2 py-1 rounded border border-rose-100/50">
                            {key}
                          </div>
                        ))}
                      </div>
                    </div>
                    
                    <button
                      onClick={() => setShowStoragePurgeConfirm(true)}
                      disabled={isStartingPurge}
                      className="whitespace-nowrap flex items-center justify-center gap-3 px-8 py-4 rounded-xl font-black text-xs uppercase tracking-wider transition-all transform active:scale-95 bg-rose-600 text-white shadow-lg shadow-rose-100 hover:bg-rose-700 hover:-translate-y-0.5"
                    >
                      <FaTrashAlt size={14} />
                      {t('admin.maintenance.scan.purgeButton')}
                    </button>
                  </div>
                )}

                {isPurgingStorageRunning && (
                   <div className="mt-6 flex items-center gap-3 text-indigo-600 text-xs font-black uppercase tracking-widest justify-center animate-pulse">
                     <FaSync className="animate-spin" /> {t('admin.maintenance.scan.purging')}
                   </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {showStagingPurgeConfirm && (
        <ConfirmationDialog
          onCancel={() => setShowStagingPurgeConfirm(false)}
          onConfirm={handlePurgeStaging}
          title={t('admin.maintenance.staging.confirmTitle')}
          message={t('admin.maintenance.staging.confirmMessage')}
          confirmButtonText={t('admin.maintenance.staging.button')}
          confirmColor="bg-rose-600"
        />
      )}

      {showStoragePurgeConfirm && (
        <ConfirmationDialog
          onCancel={() => setShowStoragePurgeConfirm(false)}
          onConfirm={handleStartStoragePurge}
          title={t('admin.maintenance.scan.confirmPurgeTitle')}
          message={t('admin.maintenance.scan.confirmPurgeMessage', { count: scanStats?.orphanedFilesFound })}
          confirmButtonText={t('admin.maintenance.scan.purgeButton')}
          confirmColor="bg-rose-600"
        />
      )}
    </div>
  );
};

export default MaintenancePanel;
