import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { FaSpinner, FaCheck, FaExclamationTriangle, FaTrash, FaArrowRight, FaCheckDouble, FaChevronRight, FaFolderOpen, FaFileMedical, FaSearchPlus, FaRedo } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import { fetchWithCsrf } from './api';
import { useImport } from './ImportContext';

const formatBytes = (bytes, decimals = 1) => {
    if (bytes === 0) return '0 B';
    if (!bytes || isNaN(bytes)) return '—';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
};

const ImportPage = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { sessions, refreshSessions } = useImport();
    
    // Local state for selected files (File objects)
    const [files, setFiles] = useState([]); 
    
    // Backend state: Map of filename -> session item data
    const [sessionItemsMap, setSessionItemsMap] = useState({});
    
    // Local state for upload progress/status before backend confirms
    const [localUploadStatus, setLocalUploadStatus] = useState({});

    const [sessionId, setSessionId] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [supportedFormats, setSupportedFormats] = useState([]);
    const [showFolderConfirm, setShowFolderConfirm] = useState(false);
    
    // Find our specific session in the global context
    const currentSessionFromContext = useMemo(() => 
        sessions.find(s => s.id === sessionId),
    [sessions, sessionId]);

    // Fetch items from backend
    const fetchSessionItems = useCallback(async () => {
        if (!sessionId) return;
        try {
            const response = await fetchWithCsrf(`/api/import/sessions/${sessionId}/items`);
            if (response.ok) {
                const items = await response.json();
                const newMap = {};
                items.forEach(item => {
                    // Map resolution item format back to local uploads entry
                    item.formats.forEach(format => {
                        const entry = {
                            status: item.status, // Use the shared resolution status
                            id: format.uploadId,
                            data: item
                        };
                        if (format.fileName) newMap[format.fileName] = entry;
                        if (format.uploadId) newMap[format.uploadId] = entry;
                    });
                });
                setSessionItemsMap(newMap);
            } else if (response.status === 404) {
                // Session was cancelled or deleted on backend - clean up state and stop polling
                setSessionId(null);
                setSessionItemsMap({});
                setLocalUploadStatus({});
            }
        } catch (err) {
            console.error("Failed to sync items for session", sessionId, err);
        }
    }, [sessionId]);

    // Keep refs of state so setInterval can evaluate current state without re-triggering useEffect
    const filesRef = useRef(files);
    const localUploadStatusRef = useRef(localUploadStatus);
    const sessionItemsMapRef = useRef(sessionItemsMap);

    useEffect(() => {
        filesRef.current = files;
        localUploadStatusRef.current = localUploadStatus;
        sessionItemsMapRef.current = sessionItemsMap;
    });

    // Poll for updates (Backup mechanism - steady 3 second interval, no synchronous loop)
    useEffect(() => {
        if (!sessionId) return;

        const intervalId = setInterval(() => {
            const currentFiles = filesRef.current;
            const currentSessionItems = sessionItemsMapRef.current;
            const currentLocal = localUploadStatusRef.current;

            const hasIncomplete = currentFiles.some(f => {
                const local = currentLocal[f.name];
                if (local?.id && currentSessionItems[local.id]) {
                    return ['PENDING', 'QUEUED', 'UPLOADING', 'PROCESSING'].includes(currentSessionItems[local.id].status);
                }
                const backendItem = currentSessionItems[f.name];
                if (backendItem) {
                    return ['PENDING', 'QUEUED', 'UPLOADING', 'PROCESSING'].includes(backendItem.status);
                }
                const status = local?.status || 'PENDING';
                return ['PENDING', 'QUEUED', 'UPLOADING', 'PROCESSING'].includes(status);
            });

            if (hasIncomplete || isUploading) {
                fetchSessionItems();
            }
        }, 3000);

        return () => clearInterval(intervalId);
    }, [sessionId, fetchSessionItems, isUploading]);

    // Primary sync on SSE updates for active sessions
    useEffect(() => {
        if (currentSessionFromContext && (currentSessionFromContext.status === 'ACTIVE' || currentSessionFromContext.status === 'CREATED')) {
            fetchSessionItems();
        }
    }, [currentSessionFromContext?.processedFiles, currentSessionFromContext?.failedFiles, fetchSessionItems, currentSessionFromContext]);

    // Scan options
    const [maxDepth, setMaxDepth] = useState(5);
    const [noDepthLimit, setNoDepthLimit] = useState(false);
    const [isScanning, setIsScanning] = useState(false);

    const fileInputRef = useRef(null);
    const folderInputRef = useRef(null);

    useEffect(() => {
        const fetchFormats = async () => {
            try {
                const response = await fetch('/api/import/supported-formats');
                if (response.ok) {
                    const data = await response.json();
                    setSupportedFormats(data);
                }
            } catch (error) {
                console.error("Failed to fetch supported formats", error);
            }
        };
        fetchFormats();
    }, []);

    const supportedExtSet = useMemo(() => 
        new Set(supportedFormats.map(f => f.extension.toLowerCase()))
    , [supportedFormats]);

    const acceptedExtensions = useMemo(() => 
        supportedFormats.map(f => `.${f.extension}`).join(',')
    , [supportedFormats]);

    const discardSession = async (sessId) => {
        if (!window.confirm(t('import.confirmDiscardSession', 'Are you sure you want to discard this session and all uploaded files?'))) return;
        try {
            const response = await fetchWithCsrf(`/api/import/sessions/${sessId}`, {
                method: 'DELETE'
            });
            if (response.ok) {
                refreshSessions();
                if (sessionId === sessId) {
                    setSessionId(null);
                    setSessionItemsMap({});
                    setLocalUploadStatus({});
                }
            }
        } catch (error) {
            console.error("Discard session error", error);
        }
    };

    const processFiles = (selectedFiles, depthLimit = null) => {
        setIsScanning(true);
        
        // Use a timeout to allow the UI to update
        setTimeout(() => {
            const newFileList = [];
            const newLocalStatus = {};

            for (let i = 0; i < selectedFiles.length; i++) {
                const file = selectedFiles[i];
                const ext = file.name.split('.').pop().toLowerCase();
                
                if (!supportedExtSet.has(ext)) continue;
                
                if (file.webkitRelativePath && depthLimit !== null) {
                    const depth = file.webkitRelativePath.split('/').length - 1;
                    if (depth > depthLimit) continue;
                }

                if (!files.some(f => f.name === file.name)) {
                    newFileList.push(file);
                    newLocalStatus[file.name] = { status: 'PENDING', progress: 0 };
                }
            }

            if (newFileList.length > 0) {
                setFiles(prev => [...prev, ...newFileList]);
                setLocalUploadStatus(prev => ({ ...prev, ...newLocalStatus }));
            }
            
            setIsScanning(false);
        }, 100);
    };

    const handleFileSelect = (e) => {
        processFiles(e.target.files);
    };

    const handleFolderSelect = (e) => {
        processFiles(e.target.files, noDepthLimit ? null : maxDepth);
        setShowFolderConfirm(false);
    };

    const triggerFolderPicker = () => {
        folderInputRef.current.click();
    };

    const removeFile = (fileName) => {
        setFiles(prev => prev.filter(f => f.name !== fileName));
        setLocalUploadStatus(prev => {
            const next = { ...prev };
            delete next[fileName];
            return next;
        });
        setSessionItemsMap(prev => {
            const next = { ...prev };
            delete next[fileName];
            return next;
        });
    };

    const clearAll = async () => {
        const message = sessionId 
            ? t('import.confirmClearAllWithSession', 'Are you sure you want to clear the list and discard the current import session?')
            : t('import.confirmClearAll', 'Are you sure you want to clear the list of selected files?');
        
        if (!window.confirm(message)) return;

        if (sessionId) {
            try {
                await fetchWithCsrf(`/api/import/sessions/${sessionId}`, { method: 'DELETE' });
                refreshSessions();
            } catch (error) {
                console.error("Error discarding session during clear all", error);
            }
        }

        setFiles([]);
        setSessionItemsMap({});
        setLocalUploadStatus({});
        setSessionId(null);
        setIsUploading(false);
    };

    const uploadFile = async (file, currentSessionId) => {
        setLocalUploadStatus(prev => ({ ...prev, [file.name]: { ...prev[file.name], status: 'UPLOADING' } }));

        const formData = new FormData();
        formData.append('file', file);
        formData.append('async', 'true');
        if (currentSessionId) {
            formData.append('importSessionId', currentSessionId);
        }

        try {
            const url = `/api/import/upload?async=true${currentSessionId ? `&importSessionId=${currentSessionId}` : ''}`;
            const response = await fetchWithCsrf(url, { 
                method: 'POST',
                body: formData
            });

            if (!response.ok) throw new Error('Upload failed');
            const data = await response.json();
            
            setLocalUploadStatus(prev => ({ 
                ...prev, 
                [file.name]: { ...prev[file.name], status: data.status || 'QUEUED', id: data.id } 
            }));
            
            // Trigger a sync to get the backend state as soon as possible
            fetchSessionItems();

        } catch (error) {
             setLocalUploadStatus(prev => ({ 
                ...prev, [file.name]: { ...prev[file.name], status: 'FAILED', error: error.message } 
            }));
        }
    };

    const retrySingleFile = async (file) => {
        const local = localUploadStatus[file.name];
        const uploadId = local?.id || sessionItemsMap[file.name]?.id;
        
        setLocalUploadStatus(prev => ({
            ...prev,
            [file.name]: { status: 'PROCESSING' }
        }));

        if (uploadId) {
            try {
                const response = await fetchWithCsrf(`/api/import/staged/${uploadId}/retry`, { method: 'POST' });
                if (response.ok) {
                    fetchSessionItems();
                } else {
                    await uploadFile(file, sessionId);
                }
            } catch (err) {
                await uploadFile(file, sessionId);
            }
        } else {
            await uploadFile(file, sessionId);
        }
    };

    const retryFailedFiles = async () => {
        const failed = files.filter(f => {
            const status = getFileStatus(f.name).status;
            return status === 'FAILED' || status === 'ERROR';
        });
        for (const file of failed) {
            await retrySingleFile(file);
        }
    };

    const startUploadAll = async () => {
        // Filter files that are not yet in sessionItemsMap and not currently uploading
        const pendingFiles = files.filter(f => {
            const backendItem = sessionItemsMap[f.name];
            const local = localUploadStatus[f.name];
            if (backendItem && backendItem.status !== 'FAILED' && backendItem.status !== 'ERROR') return false;
            return !local || local.status === 'PENDING';
        });

        if (pendingFiles.length === 0) return;

        setIsUploading(true);

        // Mark pending files as queued
        const initialStatus = {};
        pendingFiles.forEach(f => {
            initialStatus[f.name] = { status: 'QUEUED' };
        });
        setLocalUploadStatus(prev => ({ ...prev, ...initialStatus }));
        
        try {
            let currentSessionId = sessionId;
            if (!currentSessionId) {
                const sessionResponse = await fetchWithCsrf(`/api/import/sessions?totalFiles=${pendingFiles.length}`, {
                    method: 'POST'
                });
                if (!sessionResponse.ok) throw new Error('Failed to create import session');
                const sessionData = await sessionResponse.json();
                currentSessionId = sessionData.id;
                setSessionId(currentSessionId);
                refreshSessions();
            }

            // Upload in parallel with concurrency limit of 3
            const concurrencyLimit = 3;
            let index = 0;
            const runWorker = async () => {
                while (index < pendingFiles.length) {
                    const file = pendingFiles[index++];
                    await uploadFile(file, currentSessionId);
                }
            };

            const workers = [];
            for (let i = 0; i < Math.min(concurrencyLimit, pendingFiles.length); i++) {
                workers.push(runWorker());
            }
            await Promise.all(workers);
        } catch (error) {
            console.error("Session creation error", error);
            setIsUploading(false);
        }
    };

    // Derived State for UI
    const getFileStatus = (fileName) => {
        const local = localUploadStatus[fileName];
        if (local?.id && sessionItemsMap[local.id]) {
            return sessionItemsMap[local.id];
        }

        const backendItem = sessionItemsMap[fileName];
        if (backendItem) return backendItem; // { status, id, data }
        
        if (local) return local; // { status, progress, error }
        
        return { status: 'PENDING' };
    };

    const activeUploadsCount = files.filter(f => {
        const status = getFileStatus(f.name).status;
        return ['UPLOADING', 'PROCESSING'].includes(status);
    }).length;

    const completedUploadsCount = files.filter(f => {
        const status = getFileStatus(f.name).status;
        return ['PARSED', 'PROMOTED', 'UNRESOLVED', 'RESOLVED', 'STAGED', 'FAILED', 'ERROR'].includes(status);
    }).length;
    
    const isActuallyUploading = activeUploadsCount > 0;
    const isFullyCompleted = files.length > 0 && completedUploadsCount === files.length;
    const hasPending = files.some(f => {
        const status = getFileStatus(f.name).status;
        return status === 'PENDING';
    });

    useEffect(() => {
        if (isUploading && !isActuallyUploading && (completedUploadsCount > 0 || hasPending === false)) {
            // If we think we are uploading, but no active uploads remain, turn off flag
            setIsUploading(false);
        }
    }, [isActuallyUploading, isUploading, completedUploadsCount, hasPending]);

    const getUploadButtonConfig = () => {
        if (isActuallyUploading) {
            return {
                label: t('import.uploadingLabel', 'Uploading...'),
                icon: <FaSpinner className="animate-spin" />,
                style: 'bg-indigo-600 text-white shadow-indigo-100',
                enabled: false
            };
        }

        if (isFullyCompleted && !hasPending) {
            const hasFailed = files.some(f => getFileStatus(f.name).status === 'FAILED');
            if (hasFailed) {
                return {
                    label: t('import.uploadCompleteWithErrors', 'Upload Complete (with errors)'),
                    icon: <FaExclamationTriangle />,
                    style: 'bg-amber-500 text-white shadow-amber-100',
                    enabled: false
                };
            }
            return {
                label: t('import.uploadFinished', 'Upload Finished'),
                icon: <FaCheckDouble />,
                style: 'bg-green-600 text-white shadow-green-100',
                enabled: false
            };
        }

        const isReady = hasPending && !isActuallyUploading;
        return {
            label: t('import.startUpload', 'Start Upload'),
            icon: <FaCheck />,
            style: isReady ? 'bg-indigo-600 text-white shadow-indigo-100 hover:bg-indigo-700 hover:-translate-y-1' : 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none',
            enabled: isReady
        };
    };

    const uploadBtn = getUploadButtonConfig();

    return (
        <div className="p-6">
            <h1 className="text-2xl font-black text-gray-900 tracking-tight mb-6">{t('import.bulkUploadTitle', 'Bulk Upload')}</h1>
            
            {sessions.length > 0 && (
                <div className="mb-8 p-6 bg-indigo-50 border border-indigo-100 rounded-[2rem] shadow-inner animate-fade-in">
                    <h2 className="text-sm font-black uppercase tracking-[0.2em] mb-4 flex items-center gap-2 text-indigo-800">
                        <FaArrowRight className="text-indigo-600" /> {t('import.activeSessions', 'Ongoing Import Sessions')}
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {sessions.map(session => {
                            const total = session.totalFiles || 1;
                            const processed = session.processedFiles || 0;
                            const failed = session.failedFiles || 0;
                            const queuedOrProcessing = Math.max(0, total - (processed + failed));

                            const processedPct = Math.min(100, (processed / total) * 100);
                            const failedPct = Math.min(100 - processedPct, (failed / total) * 100);
                            const activePct = Math.max(0, 100 - (processedPct + failedPct));

                            return (
                                <div 
                                    key={session.id} 
                                    onClick={() => navigate(`/import/session/${session.id}`)}
                                    className="group relative bg-white p-5 rounded-2xl border border-gray-100 shadow-md hover:shadow-xl hover:border-indigo-200 transition-all cursor-pointer overflow-hidden transform hover:-translate-y-1"
                                >
                                    <div className="flex justify-between items-start mb-3">
                                        <div>
                                            <div className="text-sm font-black text-gray-800 tracking-tight">
                                                {new Date(session.createdAt).toLocaleDateString()}
                                                <span className="ml-2 text-[10px] text-gray-400 font-normal">
                                                    {new Date(session.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                                </span>
                                            </div>
                                            <div className="text-[10px] font-black text-indigo-600 uppercase tracking-widest mt-1">
                                                {session.processedFiles} / {session.totalFiles} {t('import.processedCount', 'processed')}
                                            </div>
                                        </div>
                                        
                                        <button 
                                            onClick={(e) => { e.stopPropagation(); discardSession(session.id); }}
                                            className="p-2 text-gray-300 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-all"
                                            title={t('common.delete', 'Delete')}
                                        >
                                            <FaTrash size={14} />
                                        </button>
                                    </div>

                                    {/* Multi-Segment Progress Bar */}
                                    <div className="flex h-2 w-full bg-gray-100 rounded-full overflow-hidden mb-3 border border-gray-100">
                                        <div 
                                            className="h-full bg-emerald-500 transition-all duration-500" 
                                            style={{ width: `${processedPct}%` }}
                                            title={`Parsed: ${processed}`}
                                        />
                                        <div 
                                            className="h-full bg-amber-500 animate-pulse transition-all duration-500" 
                                            style={{ width: `${activePct}%` }}
                                            title={`Queued/Processing: ${queuedOrProcessing}`}
                                        />
                                        <div 
                                            className="h-full bg-rose-500 transition-all duration-500" 
                                            style={{ width: `${failedPct}%` }}
                                            title={`Failed: ${failed}`}
                                        />
                                    </div>

                                    {/* Granular Breakdown Pills */}
                                    <div className="flex flex-wrap items-center gap-1.5 mb-2 text-[9px] font-black uppercase tracking-wider">
                                        <span className="px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-100">{processed} {t('import.status.parsed', 'Parsed')}</span>
                                        {queuedOrProcessing > 0 && (
                                            <span className="px-2 py-0.5 rounded-md bg-amber-50 text-amber-700 border border-amber-100 animate-pulse">{queuedOrProcessing} {t('import.status.processing', 'Processing / Queued')}</span>
                                        )}
                                        {failed > 0 && (
                                            <span className="px-2 py-0.5 rounded-md bg-rose-50 text-rose-700 border border-rose-100">{failed} {t('import.status.failed', 'Failed')}</span>
                                        )}
                                    </div>

                                    <div className="flex items-center gap-2 text-indigo-600 text-[10px] font-black uppercase tracking-widest opacity-0 group-hover:opacity-100 transition-all transform translate-x-[-10px] group-hover:translate-x-0">
                                        {t('common.details', 'Details')} <FaChevronRight size={8} />
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}

            <div className="flex flex-wrap gap-4 mb-8">
                <input 
                    type="file" 
                    multiple 
                    accept={acceptedExtensions}
                    onChange={handleFileSelect} 
                    className="hidden"
                    ref={fileInputRef}
                />
                <input 
                    type="file" 
                    webkitdirectory="true"
                    directory="true"
                    onChange={handleFolderSelect} 
                    className="hidden"
                    ref={folderInputRef}
                />

                <button 
                    onClick={() => fileInputRef.current.click()}
                    className="flex items-center justify-center gap-3 px-8 py-3 bg-white text-gray-600 border border-gray-200 rounded-2xl font-black text-xs uppercase tracking-widest hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-100 transition-all shadow-sm transform active:scale-95"
                >
                    <FaFileMedical /> {t('import.selectFiles')}
                </button>

                <button 
                    onClick={() => setShowFolderConfirm(true)}
                    className="flex items-center justify-center gap-3 px-8 py-3 bg-white text-gray-600 border border-gray-200 rounded-2xl font-black text-xs uppercase tracking-widest hover:bg-amber-50 hover:text-amber-600 hover:border-amber-100 transition-all shadow-sm transform active:scale-95"
                >
                    <FaFolderOpen /> {t('import.selectFolder')}
                </button>

                <div className="flex-grow"></div>

                {files.some(f => ['FAILED', 'ERROR'].includes(getFileStatus(f.name).status)) && (
                    <button 
                        onClick={retryFailedFiles}
                        disabled={isUploading}
                        className="flex items-center justify-center gap-3 px-8 py-3 bg-amber-500 text-white rounded-2xl font-black text-xs uppercase tracking-widest hover:bg-amber-600 transition-all shadow-xl shadow-amber-100 transform active:scale-95"
                    >
                        <FaRedo /> {t('import.actions.retryFailed', 'Retry Failed')}
                    </button>
                )}

                <button 
                    onClick={startUploadAll}
                    disabled={!uploadBtn.enabled}
                    className={`flex items-center justify-center gap-3 px-10 py-3 rounded-2xl font-black text-xs uppercase tracking-widest transition-all transform active:scale-95 shadow-xl ${uploadBtn.style}`}
                >
                    {uploadBtn.icon}
                    {uploadBtn.label}
                </button>

                <button 
                    onClick={clearAll}
                    disabled={files.length === 0 || isUploading}
                    className={`flex items-center justify-center gap-3 px-8 py-3 rounded-2xl font-black text-xs uppercase tracking-widest transition-all transform active:scale-95 shadow-xl ${
                        files.length === 0 || isUploading
                            ? 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none' 
                            : 'bg-rose-50 text-rose-600 border border-rose-100 shadow-rose-50 hover:bg-rose-100 hover:-translate-y-1'
                    }`}
                >
                    <FaTrash />
                    {t('import.clearAll', 'Clear All')}
                </button>

                {sessionId && (
                    <button 
                        onClick={() => navigate(`/import/session/${sessionId}`)}
                        className="flex items-center justify-center gap-3 px-8 py-3 rounded-2xl font-black text-xs uppercase tracking-widest transition-all transform active:scale-95 shadow-xl bg-green-600 text-white shadow-green-100 hover:bg-green-700 hover:-translate-y-1"
                    >
                        <FaArrowRight /> {t('import.goToDashboard', 'Go to Session Dashboard')}
                    </button>
                )}
            </div>

            <div className="bg-white shadow-xl rounded-[2.5rem] overflow-hidden border border-gray-100 animate-fade-in relative">
                {isScanning && (
                    <div className="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-10 flex items-center justify-center flex-col gap-4">
                        <FaSpinner className="text-indigo-600 animate-spin text-4xl" />
                        <span className="font-black text-xs uppercase tracking-widest text-indigo-600">{t('import.status.processing')}</span>
                    </div>
                )}
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50/50">
                            <tr className="text-gray-500 uppercase text-[10px] font-black tracking-widest border-b border-gray-200">
                                <th className="px-6 py-5 text-left">{t('import.table.fileName', 'File Name')}</th>
                                <th className="px-6 py-5 text-left">{t('import.table.size', 'Size')}</th>
                                <th className="px-6 py-5 text-left">{t('import.table.status', 'Status')}</th>
                                <th className="px-6 py-5 text-left">{t('import.table.details', 'Details')}</th>
                                <th className="px-6 py-5 text-right">{t('import.table.actions', 'Actions')}</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-100">
                            {files.map(file => {
                                const statusObj = getFileStatus(file.name);
                                const status = statusObj.status;
                                const data = statusObj.data;
                                const error = statusObj.error;

                                return (
                                    <tr key={file.name} className="group hover:bg-indigo-50/30 transition-all duration-200">
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-700">{file.name}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono">{file.size != null ? formatBytes(file.size) : '—'}</td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            {status === 'PENDING' && (
                                                <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-gray-100 text-gray-600 uppercase tracking-tighter border border-gray-200">
                                                    {t('import.status.pending', 'Pending')}
                                                </span>
                                            )}
                                            {status === 'QUEUED' && (
                                                <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-slate-100 text-slate-700 uppercase tracking-tighter border border-slate-200">
                                                    {t('import.status.queued', 'Queued')}
                                                </span>
                                            )}
                                            {status === 'UPLOADING' && (
                                                <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-indigo-50 text-indigo-600 uppercase tracking-tighter border border-indigo-100 animate-pulse">
                                                    <FaSpinner className="animate-spin mr-1.5" /> {t('import.status.uploading', 'Uploading...')}
                                                </span>
                                            )}
                                            {status === 'PROCESSING' && (
                                                <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-amber-50 text-amber-600 uppercase tracking-tighter border border-amber-100 animate-pulse">
                                                    <FaSpinner className="animate-spin mr-1.5" /> {t('import.status.processing', 'Processing...')}
                                                </span>
                                            )}
                                            {['PARSED', 'PROMOTED', 'UNRESOLVED', 'RESOLVED', 'STAGED'].includes(status) && (
                                                <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-emerald-50 text-emerald-600 uppercase tracking-tighter border border-emerald-100">
                                                    <FaCheck className="mr-1.5" /> {t('import.status.parsed', 'Parsed')}
                                                </span>
                                            )}
                                            {(status === 'FAILED' || status === 'ERROR') && (
                                                <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-rose-50 text-rose-600 uppercase tracking-tighter border border-rose-100">
                                                    <FaExclamationTriangle className="mr-1.5" /> {t('import.status.failed', 'Failed')}
                                                </span>
                                            )}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {data?.metadata?.title && (
                                                <div>
                                                    <div className="font-bold">{data.metadata.title}</div>
                                                    <div className="text-xs">{data.metadata.authors?.join(', ')}</div>
                                                </div>
                                            )}
                                            {error && <span className="text-rose-500 font-bold text-[10px] uppercase tracking-tighter">{error}</span>}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                            <div className="flex items-center justify-end gap-2">
                                                {(status === 'FAILED' || status === 'ERROR') && (
                                                    <button 
                                                        onClick={() => retrySingleFile(file)}
                                                        className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-50 text-amber-600 hover:bg-amber-100 rounded-lg transition-all font-black text-[10px] uppercase tracking-widest border border-amber-100 shadow-sm transform active:scale-95"
                                                        title={t('import.actions.retry', 'Retry')}
                                                    >
                                                        <FaRedo size={12} /> {t('import.actions.retry', 'Retry')}
                                                    </button>
                                                )}
                                                <button 
                                                    onClick={() => removeFile(file.name)} 
                                                    disabled={isUploading && (status === 'UPLOADING' || status === 'PROCESSING')}
                                                    className={`p-2 rounded-lg transition-all ${isUploading && (status === 'UPLOADING' || status === 'PROCESSING') ? 'text-gray-200 cursor-not-allowed' : 'text-rose-600 hover:bg-rose-50'}`}
                                                    title={t('common.remove', 'Remove')}
                                                >
                                                    <FaTrash />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                            {files.length === 0 && (
                                <tr>
                                    <td colSpan="5" className="px-6 py-20 text-center text-gray-400 italic text-sm">
                                        <FaFolderOpen className="text-4xl mx-auto mb-4 opacity-20" />
                                        {t('common.noFilesSelected', 'No files selected for import.')}
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {showFolderConfirm && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fade-in">
                    <div className="bg-white rounded-[2.5rem] shadow-2xl w-full max-w-lg overflow-hidden border border-gray-100">
                        <div className="p-8">
                            <div className="flex items-center gap-4 mb-6">
                                <div className="p-4 rounded-3xl bg-amber-50 text-amber-600 shadow-inner">
                                    <FaSearchPlus size={32} />
                                </div>
                                <div>
                                    <h2 className="text-2xl font-black text-gray-800 tracking-tight">{t('import.folderScanConfirmTitle')}</h2>
                                    <p className="text-[10px] font-black text-amber-600 uppercase tracking-widest mt-1">{t('import.selectFolder')}</p>
                                </div>
                            </div>
                            
                            <p className="text-gray-500 font-bold text-sm leading-relaxed mb-8">
                                {t('import.folderScanConfirmMessage')}
                            </p>

                            <div className="space-y-6 bg-gray-50 p-6 rounded-3xl border border-gray-100">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-black text-gray-700 uppercase tracking-wider">{t('import.noDepthLimit')}</label>
                                    <button 
                                        onClick={() => setNoDepthLimit(!noDepthLimit)}
                                        className={`relative w-12 h-6 rounded-full transition-colors ${noDepthLimit ? 'bg-indigo-600' : 'bg-gray-200'}`}
                                    >
                                        <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-transform ${noDepthLimit ? 'left-7' : 'left-1'}`}></div>
                                    </button>
                                </div>

                                {!noDepthLimit && (
                                    <div className="animate-fade-in">
                                        <div className="flex justify-between items-center mb-2">
                                            <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{t('import.maxDepth')}</label>
                                            <span className="text-sm font-black text-indigo-600">{maxDepth}</span>
                                        </div>
                                        <input 
                                            type="range" 
                                            min="1" 
                                            max="20" 
                                            value={maxDepth} 
                                            onChange={(e) => setMaxDepth(parseInt(e.target.value))}
                                            className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
                                        />
                                        <p className="text-[9px] text-gray-400 font-medium mt-2 italic">{t('import.depthHelp')}</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="px-8 py-6 bg-gray-50 border-t border-gray-100 flex justify-end gap-4">
                            <button 
                                onClick={() => setShowFolderConfirm(false)}
                                className="px-8 py-3 text-gray-500 bg-white border border-gray-200 rounded-2xl hover:bg-gray-50 font-black text-xs uppercase tracking-widest transition-all transform active:scale-95 shadow-sm"
                            >
                                {t('common.cancel')}
                            </button>
                            <button 
                                onClick={triggerFolderPicker}
                                className="px-10 py-3 bg-indigo-600 text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-indigo-100 hover:bg-indigo-700 transition-all transform active:scale-95"
                            >
                                {t('import.selectFolder')}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ImportPage;