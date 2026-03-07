import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FaSpinner, FaCheck, FaExclamationTriangle, FaTrash, FaArrowRight, FaCheckDouble, FaChevronRight } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

const ImportPage = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [files, setFiles] = useState([]); // List of file objects
    const [uploads, setUploads] = useState({}); // Key: fileName, Value: { status, id, error, data }
    const [sessionId, setSessionId] = useState(null);
    const [activeSessions, setActiveSessions] = useState([]);
    const [isUploading, setIsUploading] = useState(false);

    const fetchActiveSessions = async () => {
        try {
            const response = await fetch('/api/import/sessions');
            if (response.ok) {
                const data = await response.json();
                setActiveSessions(data);
            }
        } catch (error) {
            console.error("Fetch active sessions error", error);
        }
    };

    useEffect(() => {
        fetchActiveSessions();
    }, []);

    const discardSession = async (sessId) => {
        if (!window.confirm(t('import.confirmDiscardSession', 'Are you sure you want to discard this session and all uploaded files?'))) return;
        try {
            const response = await fetch(`/api/import/sessions/${sessId}`, {
                method: 'DELETE',
                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
            });
            if (response.ok) {
                console.log("Session deleted successfully", sessId);
                await fetchActiveSessions();
                if (sessionId === sessId) setSessionId(null);
            } else {
                const errorText = await response.text();
                console.error("Failed to delete session", sessId, response.status, errorText);
                alert("Failed to delete session: " + errorText);
            }
        } catch (error) {
            console.error("Discard session error", error);
            alert("Error deleting session: " + error.message);
        }
    };

    const handleFileSelect = (e) => {
        const selectedFiles = Array.from(e.target.files);
        // Avoid duplicates
        const newFiles = selectedFiles.filter(file => !files.some(f => f.name === file.name));
        setFiles(prev => [...prev, ...newFiles]);
        
        // Initialize upload state
        const newUploads = { ...uploads };
        newFiles.forEach(file => {
            if (!newUploads[file.name]) {
                newUploads[file.name] = { status: 'PENDING', progress: 0 };
            }
        });
        setUploads(newUploads);
    };

    const removeFile = (fileName) => {
        setFiles(prev => prev.filter(f => f.name !== fileName));
        setUploads(prev => {
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
                const response = await fetch(`/api/import/sessions/${sessionId}`, {
                    method: 'DELETE',
                    headers: { 'X-XSRF-TOKEN': getCsrfToken() }
                });
                if (response.ok) {
                    await fetchActiveSessions();
                }
            } catch (error) {
                console.error("Error discarding session during clear all", error);
            }
        }

        setFiles([]);
        setUploads({});
        setSessionId(null);
        setIsUploading(false);
    };

    const startUploadAll = async () => {
        const pendingFiles = files.filter(f => uploads[f.name]?.status === 'PENDING');
        if (pendingFiles.length === 0) return;

        setIsUploading(true);
        
        try {
            // 1. Create session
            const sessionResponse = await fetch(`/api/import/sessions?totalFiles=${pendingFiles.length}`, {
                method: 'POST',
                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
            });
            if (!sessionResponse.ok) throw new Error('Failed to create import session');
            const sessionData = await sessionResponse.json();
            setSessionId(sessionData.id);

            // 2. Upload files
            for (const file of pendingFiles) {
                uploadFile(file, sessionData.id);
            }
        } catch (error) {
            console.error("Session creation error", error);
            setIsUploading(false);
        }
    };

    const getCsrfToken = () => {
        const name = 'XSRF-TOKEN';
        const decodedCookie = decodeURIComponent(document.cookie);
        const ca = decodedCookie.split(';');
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) === ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name + "=") === 0) {
                return c.substring(name.length + 1, c.length);
            }
        }
        return null;
    };

    const uploadFile = async (file, currentSessionId) => {
        setUploads(prev => ({ ...prev, [file.name]: { ...prev[file.name], status: 'UPLOADING', progress: 0 } }));

        const formData = new FormData();
        formData.append('file', file);
        formData.append('async', 'true');
        if (currentSessionId) {
            formData.append('importSessionId', currentSessionId);
        }

        try {
            const url = `/api/import/upload?async=true${currentSessionId ? `&importSessionId=${currentSessionId}` : ''}`;
            const response = await fetch(url, { 
                method: 'POST',
                headers: {
                    'X-XSRF-TOKEN': getCsrfToken() 
                },
                body: formData
            });

            if (!response.ok) throw new Error('Upload failed');
            
            const data = await response.json();
            
            setUploads(prev => ({ 
                ...prev, 
                [file.name]: { 
                    ...prev[file.name], 
                    status: 'PROCESSING', 
                    id: data.id 
                } 
            }));
            
            pollStatus(file.name, data.id);

        } catch (error) {
             setUploads(prev => ({ 
                ...prev, 
                [file.name]: { ...prev[file.name], status: 'FAILED', error: error.message } 
            }));
        }
    };

    const pollStatus = (fileName, uploadId) => {
        const interval = setInterval(async () => {
            try {
                const response = await fetch(`/api/import/staged/${uploadId}`);
                if (!response.ok) throw new Error('Poll failed');
                
                const data = await response.json();
                
                if (data.status === 'PARSED' || data.status === 'FAILED' || data.status === 'PROMOTED') {
                    clearInterval(interval);
                    setUploads(prev => ({ 
                        ...prev, 
                        [fileName]: { 
                            ...prev[fileName], 
                            status: data.status, 
                            data: data 
                        } 
                    }));
                }
            } catch (error) {
                console.error("Polling error", error);
            }
        }, 1000);
    };

    const pendingFilesCount = files.filter(f => !uploads[f.name] || uploads[f.name].status === 'PENDING').length;
    const activeUploadsCount = Object.values(uploads).filter(u => ['UPLOADING', 'PROCESSING'].includes(u.status)).length;
    const failedUploadsCount = Object.values(uploads).filter(u => u.status === 'FAILED').length;
    const completedUploadsCount = Object.values(uploads).filter(u => ['PARSED', 'PROMOTED', 'FAILED'].includes(u.status)).length;
    
    const isActuallyUploading = activeUploadsCount > 0;
    const isFullyCompleted = files.length > 0 && completedUploadsCount === files.length;
    const hasPending = pendingFilesCount > 0;
    const hasFailed = failedUploadsCount > 0;

    useEffect(() => {
        if (isUploading && !isActuallyUploading && completedUploadsCount > 0) {
            setIsUploading(false);
        }
    }, [isActuallyUploading, isUploading, completedUploadsCount]);

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
            <h1 className="text-2xl font-bold mb-4">{t('import.bulkUploadTitle', 'Bulk Upload')}</h1>
            
            {activeSessions.length > 0 && (
                <div className="mb-8 p-6 bg-indigo-50 border border-indigo-100 rounded-[2rem] shadow-inner">
                    <h2 className="text-sm font-black uppercase tracking-[0.2em] mb-4 flex items-center gap-2 text-indigo-800">
                        <FaArrowRight className="text-indigo-600" /> {t('import.activeSessions', 'Ongoing Import Sessions')}
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {activeSessions.map(session => (
                            <div 
                                key={session.id} 
                                onClick={() => navigate(`/import/session/${session.id}`)}
                                className="group relative bg-white p-5 rounded-2xl border border-gray-100 shadow-md hover:shadow-xl hover:border-indigo-200 transition-all cursor-pointer overflow-hidden transform hover:-translate-y-1"
                            >
                                {/* Progress Bar at bottom */}
                                <div className="absolute bottom-0 left-0 w-full h-1 bg-gray-50">
                                    <div 
                                        className="h-full bg-indigo-500 transition-all duration-1000" 
                                        style={{ width: `${Math.min(100, (session.processedFiles / session.totalFiles) * 100)}%` }}
                                    />
                                </div>

                                <div className="flex justify-between items-start mb-4">
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

                                <div className="flex items-center gap-2 text-indigo-600 text-[10px] font-black uppercase tracking-widest opacity-0 group-hover:opacity-100 transition-all transform translate-x-[-10px] group-hover:translate-x-0">
                                    {t('common.details', 'Details')} <FaChevronRight size={8} />
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            <div className="mb-6">
                <input 
                    type="file" 
                    multiple 
                    onChange={handleFileSelect} 
                    className="block w-full text-sm text-slate-500
                        file:mr-4 file:py-2 file:px-4
                        file:rounded-full file:border-0
                        file:text-sm file:font-semibold
                        file:bg-violet-50 file:text-violet-700
                        hover:file:bg-violet-100"
                />
            </div>
            
            <div className="flex gap-4 mb-8">
                <button 
                    onClick={startUploadAll}
                    disabled={!uploadBtn.enabled}
                    className={`flex items-center justify-center gap-3 px-8 py-3 rounded-2xl font-black text-sm uppercase tracking-wider transition-all transform active:scale-95 shadow-xl ${uploadBtn.style}`}
                >
                    {uploadBtn.icon}
                    {uploadBtn.label}
                </button>

                <button 
                    onClick={clearAll}
                    disabled={files.length === 0 || isUploading}
                    className={`flex items-center justify-center gap-3 px-8 py-3 rounded-2xl font-black text-sm uppercase tracking-wider transition-all transform active:scale-95 shadow-xl ${
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
                        className="flex items-center justify-center gap-3 px-8 py-3 rounded-2xl font-black text-sm uppercase tracking-wider transition-all transform active:scale-95 shadow-xl bg-green-600 text-white shadow-green-100 hover:bg-green-700 hover:-translate-y-1"
                    >
                        <FaArrowRight /> {t('import.goToDashboard', 'Go to Session Dashboard')}
                    </button>
                )}
            </div>

            <div className="bg-white shadow rounded-lg overflow-hidden">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('import.table.fileName', 'File Name')}</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('import.table.status', 'Status')}</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('import.table.details', 'Details')}</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">{t('import.table.actions', 'Actions')}</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {files.map(file => {
                            const upload = uploads[file.name] || {};
                            return (
                                <tr key={file.name}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{file.name}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {upload.status === 'PENDING' && <span className="text-gray-500">{t('import.status.pending', 'Pending')}</span>}
                                        {upload.status === 'UPLOADING' && <span className="text-blue-500 flex items-center"><FaSpinner className="animate-spin mr-2" /> {t('import.status.uploading', 'Uploading...')}</span>}
                                        {upload.status === 'PROCESSING' && <span className="text-orange-500 flex items-center"><FaSpinner className="animate-spin mr-2" /> {t('import.status.processing', 'Processing...')}</span>}
                                        {upload.status === 'PARSED' && <span className="text-green-500 flex items-center"><FaCheck className="mr-2" /> {t('import.status.parsed', 'Parsed')}</span>}
                                        {upload.status === 'FAILED' && <span className="text-red-500 flex items-center"><FaExclamationTriangle className="mr-2" /> {t('import.status.failed', 'Failed')}</span>}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {upload.data?.metadata?.title && (
                                            <div>
                                                <div className="font-bold">{upload.data.metadata.title}</div>
                                                <div className="text-xs">{upload.data.metadata.authors?.join(', ')}</div>
                                            </div>
                                        )}
                                        {upload.error && <span className="text-red-500">{upload.error}</span>}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <button onClick={() => removeFile(file.name)} className="text-red-600 hover:text-red-900">
                                            <FaTrash />
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default ImportPage;
