import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaSpinner, FaCheck, FaExclamationTriangle, FaFilter, FaSyncAlt, FaArrowLeft, FaBan, FaInfoCircle, FaMagic, FaTrash, FaHourglassHalf, FaRedo } from 'react-icons/fa';
import Notification from './Notification';

import { useImport } from './ImportContext';

const ImportSummaryDashboard = () => {
    const { id: sessionId } = useParams();
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { sessions } = useImport();
    
    // Find this specific session in the global context
    const sessionFromContext = useMemo(() => 
        sessions.find(s => s.id === sessionId), 
    [sessions, sessionId]);

    const [session, setSession] = useState(null);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isProcessing, setIsProcessing] = useState(false);
    const [filter, setFilter] = useState('ALL'); // ALL, UNRESOLVED, RESOLVED, IGNORED, ERROR, PROCESSING, STAGED
    const [selectedIds, setSelectedIds] = useState([]);
    const [notification, setNotification] = useState(null);

    const fetchSession = async () => {
        try {
            const response = await fetch(`/api/import/sessions/${sessionId}`);
            if (!response.ok) throw new Error('Failed to fetch session');
            const data = await response.json();
            setSession(data);
        } catch (error) {
            console.error("Fetch session error", error);
        }
    };

    const fetchItems = async () => {
        try {
            const response = await fetch(`/api/import/sessions/${sessionId}/items`);
            if (!response.ok) throw new Error('Failed to fetch items');
            const data = await response.json();
            setItems(data);
        } catch (error) {
            console.error("Fetch items error", error);
        } finally {
            setLoading(false);
        }
    };

    // Initial load
    useEffect(() => {
        fetchSession();
        fetchItems();
    }, [sessionId]);

    // Update local session state and items when global context session changes (SSE update)
    useEffect(() => {
        if (sessionFromContext) {
            setSession(sessionFromContext);
            fetchItems(); // Re-fetch items to get updated statuses/metadata
        }
    }, [sessionFromContext]);

    const updateItemStatus = async (itemId, status) => {
        try {
            const response = await fetch(`/api/import/items/${itemId}/status?status=${status}`, {
                method: 'PATCH',
                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
            });
            if (response.ok) {
                fetchItems();
            }
        } catch (error) {
            console.error("Update item status error", error);
        }
    };

    const handleRetry = async (itemId) => {
        try {
            const response = await fetch(`/api/import/staged/${itemId}/retry`, {
                method: 'POST',
                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
            });
            if (response.ok) {
                fetchItems();
                setNotification({ type: 'success', message: t('import.status.processing') });
            } else {
                setNotification({ type: 'error', message: t('common.error') });
            }
        } catch (error) {
            console.error("Retry item error", error);
            setNotification({ type: 'error', message: t('common.error') });
        }
    };

    const bulkUpdateStatus = async (status) => {
        if (selectedIds.length === 0) return;
        try {
            const params = new URLSearchParams();
            selectedIds.forEach(id => params.append('ids', id));
            params.append('status', status);
            
            const response = await fetch(`/api/import/items/bulk-status?${params.toString()}`, {
                method: 'PATCH',
                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
            });
            if (response.ok) {
                setSelectedIds([]);
                fetchItems();
            }
        } catch (error) {
            console.error("Bulk update error", error);
        }
    };

    const handleAutoResolve = async (strategy) => {
        const ids = selectedIds.length > 0 ? selectedIds : null;
        const confirmMsg = ids 
            ? t('import.confirmAutoResolveSelected', 'Apply auto-resolution to {{count}} selected items?', { count: ids.length })
            : t('import.confirmAutoResolveAll', 'Apply auto-resolution to all unresolved items in this session?');
            
        if (!window.confirm(confirmMsg)) return;

        setIsProcessing(true);
        try {
            const params = new URLSearchParams();
            if (ids) ids.forEach(id => params.append('ids', id));
            params.append('strategy', strategy);

            const response = await fetch(`/api/import/sessions/${sessionId}/auto-resolve?${params.toString()}`, {
                method: 'POST',
                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
            });

            if (response.ok) {
                setSelectedIds([]);
                fetchItems();
                fetchSession();
            } else {
                alert(t('import.autoResolveError'));
            }
        } catch (error) {
            console.error("Auto-resolve error", error);
            alert(t('import.autoResolveError'));
        } finally {
            setIsProcessing(false);
        }
    };

    const toggleSelectAll = () => {
        if (selectedIds.length === filteredItems.length && filteredItems.length > 0) {
            setSelectedIds([]);
        } else {
            setSelectedIds(filteredItems.map(item => item.id));
        }
    };

    const toggleSelect = (id) => {
        setSelectedIds(prev => 
            prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
        );
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

    const filteredItems = items.filter(item => {
        if (filter === 'ALL') return true;
        return item.status === filter;
    });

    const finalizeSession = async () => {
        const unresolvedCount = items.filter(i => i.status === 'UNRESOLVED').length;
        let msg = t('import.confirmFinalize', 'Are you sure you want to complete this import session?');
        if (unresolvedCount > 0) {
            msg += `\n\n${t('import.unresolvedWarning', 'Warning: there are still {{count}} unresolved items. They will NOT be imported.', { count: unresolvedCount })}`;
        }

        if (!window.confirm(msg)) return;

        try {
            const response = await fetch(`/api/import/sessions/${sessionId}/finalize`, {
                method: 'POST',
                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
            });
            if (response.ok) {
                navigate('/import', { state: { notification: { message: t('import.finalizeSuccess', 'Import session completed successfully.'), type: 'success' } } });
            } else {
                const errorText = await response.text();
                alert("Failed to finalize session: " + errorText);
            }
        } catch (error) {
            console.error("Finalize session error", error);
            alert("Error finalizing session: " + error.message);
        }
    };

    const discardSession = async () => {
        if (!window.confirm(t('import.confirmDiscardSession', 'Are you sure you want to discard this session and all uploaded files?'))) return;
        try {
            const response = await fetch(`/api/import/sessions/${sessionId}`, {
                method: 'DELETE',
                headers: { 'X-XSRF-TOKEN': getCsrfToken() }
            });
            if (response.ok) {
                navigate('/import', { state: { notification: { message: t('import.discardSuccess', 'Import session discarded.'), type: 'info' } } });
            } else {
                const errorText = await response.text();
                alert("Failed to discard session: " + errorText);
            }
        } catch (error) {
            console.error("Discard session error", error);
            alert("Error discarding session: " + error.message);
        }
    };

    if (loading) return <div className="p-6 text-center"><FaSpinner className="animate-spin text-4xl mx-auto" /></div>;

    const pendingCount = items.filter(i => i.status === 'PROCESSING' || i.status === 'STAGED').length;

    return (
        <div className="p-6">
            {notification && (
                <Notification
                    type={notification.type}
                    message={notification.message}
                    onClose={() => setNotification(null)}
                />
            )}

            <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate('/import')} className="p-3 bg-white text-gray-400 hover:text-gray-600 border border-gray-200 rounded-xl transition-all shadow-sm">
                        <FaArrowLeft size={20} />
                    </button>
                    <div>
                        <h1 className="text-2xl font-black text-gray-900 tracking-tight">{t('import.dashboardTitle', 'Import Session Dashboard')}</h1>
                        <p className="text-[10px] text-indigo-400 font-black uppercase tracking-widest mt-0.5">{sessionId}</p>
                    </div>
                </div>

                <div className="flex gap-3 items-center">
                    <button 
                        onClick={() => { fetchSession(); fetchItems(); }}
                        className="p-3 bg-white text-gray-400 hover:text-indigo-600 border border-gray-200 hover:border-indigo-100 rounded-xl shadow-sm transition-all transform active:scale-95"
                        title={t('common.refresh', 'Refresh')}
                    >
                        <FaSyncAlt className={loading ? 'animate-spin' : ''} />
                    </button>
                    <button 
                        onClick={discardSession}
                        className="px-8 py-3 text-rose-600 bg-rose-50 border border-rose-100 rounded-2xl hover:bg-rose-100 font-black text-xs uppercase tracking-widest transition-all shadow-xl shadow-rose-50/50 transform hover:-translate-y-1 active:scale-95 flex items-center gap-2"
                    >
                        <FaTrash /> {t('import.discardSession', 'Discard Session')}
                    </button>
                    <button 
                        onClick={finalizeSession}
                        disabled={items.filter(i => i.status === 'RESOLVED').length === 0 && items.length > 0}
                        className="px-8 py-3 bg-indigo-600 text-white rounded-2xl hover:bg-indigo-700 font-black text-xs uppercase tracking-widest transition-all shadow-xl shadow-indigo-100 disabled:opacity-50 disabled:cursor-not-allowed transform hover:-translate-y-1 active:scale-95 flex items-center gap-2"
                    >
                        <FaCheck /> {t('import.completeImport', 'Complete Import')}
                    </button>
                </div>
            </div>

            {session && (
                <div className="grid grid-cols-1 md:grid-cols-5 gap-6 mb-8">
                    <div className="bg-white p-5 rounded-2xl shadow-md border border-gray-100">
                        <div className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">{t('import.totalFiles', 'Total Files')}</div>
                        <div className="text-3xl font-black text-gray-800">{session.totalFiles}</div>
                    </div>
                    <div className="bg-white p-5 rounded-2xl shadow-md border border-gray-100 border-l-4 border-indigo-500">
                        <div className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">{t('import.statusLabel', 'Session Status')}</div>
                        <div className="text-xl font-black text-indigo-600 uppercase">
                            {t(`import.status.${session.status.toLowerCase()}`)}
                        </div>
                    </div>
                    <div className="bg-white p-5 rounded-2xl shadow-md border border-gray-100 border-l-4 border-emerald-500">
                        <div className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">{t('import.processed', 'Processed')}</div>
                        <div className="text-3xl font-black text-emerald-600">{session.processedFiles}</div>
                    </div>
                    <div className="bg-white p-5 rounded-2xl shadow-md border border-gray-100 border-l-4 border-amber-500">
                        <div className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">{t('import.status.pending', 'Pending')}</div>
                        <div className="text-3xl font-black text-amber-600">{pendingCount}</div>
                    </div>
                    <div className="bg-white p-5 rounded-2xl shadow-md border border-gray-100 border-l-4 border-rose-500">
                        <div className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">{t('import.failed', 'Failed')}</div>
                        <div className="text-3xl font-black text-rose-600">{session.failedFiles}</div>
                    </div>
                </div>
            )}

            <div className="bg-white shadow-xl rounded-2xl overflow-hidden border border-gray-100">
                <div className="p-6 border-b border-gray-100 bg-gray-50/30 flex flex-wrap justify-between items-center gap-4">
                    <div className="flex flex-wrap items-center gap-4">
                        <div className="flex items-center gap-3">
                            <input 
                                type="checkbox" 
                                className="w-5 h-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500 transition-all cursor-pointer shadow-sm"
                                checked={selectedIds.length === filteredItems.length && filteredItems.length > 0}
                                onChange={toggleSelectAll}
                            />
                            <div className="flex items-center gap-2">
                                <FaFilter className="text-gray-400" size={12} />
                                <select 
                                    value={filter} 
                                    onChange={(e) => { setFilter(e.target.value); setSelectedIds([]); }}
                                    className="bg-white border border-gray-200 rounded-lg px-3 py-1.5 text-[10px] font-black uppercase tracking-widest focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all cursor-pointer shadow-sm"
                                >
                                    <option value="ALL">{t('import.filters.all', 'All Items')}</option>
                                    <option value="UNRESOLVED">{t('import.filters.unresolved', 'Unresolved')}</option>
                                    <option value="RESOLVED">{t('import.filters.resolved', 'Resolved')}</option>
                                    <option value="IGNORED">{t('import.filters.ignored', 'Ignored')}</option>
                                    <option value="ERROR">{t('import.filters.error', 'Error')}</option>
                                    <option value="PROCESSING">{t('import.status.processing', 'Processing')}</option>
                                </select>
                            </div>
                        </div>

                        {selectedIds.length > 0 && (
                            <div className="flex items-center gap-3 border-l border-gray-200 pl-4 animate-fade-in">
                                <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest bg-indigo-50 px-2 py-1 rounded-md">
                                    {t('common.selectedCount', '{{count}} selected', { count: selectedIds.length })}
                                </span>
                                <div className="flex gap-2">
                                    <button 
                                        onClick={() => bulkUpdateStatus('IGNORED')}
                                        className="bg-white text-gray-500 border border-gray-200 px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-tighter hover:bg-rose-50 hover:text-rose-600 hover:border-rose-100 transition-all shadow-sm"
                                    >
                                        {t('import.actions.ignoreSelected', 'Ignore Selected')}
                                    </button>
                                    <button 
                                        onClick={() => bulkUpdateStatus('UNRESOLVED')}
                                        className="bg-white text-gray-500 border border-gray-200 px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-tighter hover:bg-emerald-50 hover:text-emerald-600 hover:border-emerald-100 transition-all shadow-sm"
                                    >
                                        {t('import.actions.restoreSelected', 'Restore Selected')}
                                    </button>
                                </div>
                                
                                <div className="relative group border-l border-gray-200 pl-4">
                                    <button 
                                        className="bg-purple-50 text-purple-600 border border-purple-100 px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-tighter hover:bg-purple-100 transition-all shadow-sm flex items-center gap-1"
                                        disabled={isProcessing}
                                    >
                                        <FaMagic className={isProcessing ? "animate-spin" : ""} /> {t('import.autoResolveSelected')}
                                    </button>
                                    <div className="absolute left-0 mt-1 w-48 bg-white border border-gray-200 rounded-xl shadow-xl hidden group-hover:block z-50 overflow-hidden ring-4 ring-black/5">
                                        <button onClick={() => handleAutoResolve('TRUST_INCOMING')} className="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-purple-50 hover:text-purple-700 transition-colors border-b border-gray-50">{t('import.strategies.trustIncoming')}</button>
                                        <button onClick={() => handleAutoResolve('TRUST_EXISTING')} className="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-purple-50 hover:text-purple-700 transition-colors border-b border-gray-50">{t('import.strategies.trustExisting')}</button>
                                        <button onClick={() => handleAutoResolve('NEW_ONLY')} className="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-purple-50 hover:text-purple-700 transition-colors">{t('import.strategies.newOnly')}</button>
                                    </div>
                                </div>
                            </div>
                        )}

                        {selectedIds.length === 0 && items.some(i => i.status === 'UNRESOLVED') && (
                            <div className="relative group border-l border-gray-200 pl-4">
                                <button 
                                    className="bg-purple-50 text-purple-600 border border-purple-100 px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-tighter hover:bg-purple-100 transition-all shadow-sm flex items-center gap-1"
                                    disabled={isProcessing}
                                >
                                    <FaMagic className={isProcessing ? "animate-spin" : ""} /> {t('import.autoResolveAll')}
                                </button>
                                <div className="absolute left-0 mt-1 w-48 bg-white border border-gray-200 rounded-xl shadow-xl hidden group-hover:block z-50 overflow-hidden ring-4 ring-black/5">
                                    <button onClick={() => handleAutoResolve('TRUST_INCOMING')} className="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-purple-50 hover:text-purple-700 transition-colors border-b border-gray-50">{t('import.strategies.trustIncoming')}</button>
                                    <button onClick={() => handleAutoResolve('TRUST_EXISTING')} className="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-purple-50 hover:text-purple-700 transition-colors border-b border-gray-50">{t('import.strategies.trustExisting')}</button>
                                    <button onClick={() => handleAutoResolve('NEW_ONLY')} className="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-purple-50 hover:text-purple-700 transition-colors">{t('import.strategies.newOnly')}</button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50/50">
                            <tr className="text-gray-500 uppercase text-[10px] font-black tracking-widest border-b border-gray-200">
                                <th className="px-6 py-4 text-left w-10">
                                    <input 
                                        type="checkbox" 
                                        className="w-5 h-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500 transition-all cursor-pointer shadow-sm"
                                        checked={filteredItems.length > 0 && selectedIds.length === filteredItems.length}
                                        onChange={toggleSelectAll}
                                    />
                                </th>
                                <th className="px-6 py-4 text-left">{t('import.table.bookDetails', 'Book Details')}</th>
                                <th className="px-6 py-4 text-left">{t('import.table.formats', 'Formats')}</th>
                                <th className="px-6 py-4 text-left">{t('import.table.status', 'Status')}</th>
                                <th className="px-6 py-4 text-right">{t('import.table.actions', 'Actions')}</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-100">
                            {filteredItems.map(item => (
                                <tr key={item.id} className={`group hover:bg-indigo-50/30 transition-all duration-200 ${selectedIds.includes(item.id) ? 'bg-indigo-50/50' : ''}`}>
                                    <td className="px-6 py-4">
                                        <input 
                                            type="checkbox" 
                                            className="w-5 h-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500 transition-all cursor-pointer shadow-sm"
                                            checked={selectedIds.includes(item.id)}
                                            onChange={() => toggleSelect(item.id)}
                                        />
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-sm font-black text-gray-800 tracking-tight">{item.title}</div>
                                        {item.authors?.length > 0 && (
                                            <div className="text-[10px] font-bold text-gray-400 uppercase tracking-tighter mt-0.5">{item.authors.join(', ')}</div>
                                        )}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex flex-wrap gap-1">
                                            {item.formats.map(f => (
                                                <span key={f.uploadId} className="px-2 py-0.5 bg-indigo-50 text-indigo-600 text-[9px] font-black rounded border border-indigo-100 uppercase tracking-tighter" title={f.fileName}>
                                                    {f.contentType.split('/').pop().toUpperCase()}
                                                </span>
                                            ))}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        {item.status === 'UNRESOLVED' && <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-amber-50 text-amber-600 uppercase tracking-tighter border border-amber-100">{t('import.status.unresolved')}</span>}
                                        {item.status === 'RESOLVED' && <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-emerald-50 text-emerald-600 uppercase tracking-tighter border border-emerald-100">{t('import.status.resolved')}</span>}
                                        {item.status === 'IGNORED' && <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-gray-100 text-gray-600 uppercase tracking-tighter border border-gray-200">{t('import.status.ignored')}</span>}
                                        {item.status === 'ERROR' && <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-rose-50 text-rose-600 uppercase tracking-tighter border border-rose-100">{t('import.status.error')}</span>}
                                        {item.status === 'PROCESSING' && (
                                            <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-indigo-50 text-indigo-600 uppercase tracking-tighter border border-indigo-100 gap-2 items-center">
                                                <FaHourglassHalf className="animate-spin-slow" /> {t('import.status.processing')}
                                            </span>
                                        )}
                                        {item.status === 'STAGED' && <span className="px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full bg-blue-50 text-blue-600 uppercase tracking-tighter border border-blue-100">{t('import.status.staged')}</span>}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <div className="flex justify-end gap-2 transition-all">
                                            {(item.status === 'UNRESOLVED' || item.status === 'RESOLVED') && (
                                                <>
                                                    <button 
                                                        onClick={() => navigate(`/import/resolve/${item.id}`)}
                                                        className="flex items-center gap-2 px-3 py-1.5 bg-indigo-50 text-indigo-600 hover:bg-indigo-100 rounded-lg transition-all font-black text-[10px] uppercase tracking-widest border border-indigo-100 shadow-sm transform active:scale-95"
                                                        title={item.status === 'RESOLVED' ? t('import.actions.edit', "Edit Resolution") : t('import.actions.resolve', "Resolve")}
                                                    >
                                                        <FaInfoCircle size={14} />
                                                        {item.status === 'RESOLVED' ? t('import.actions.edit') : t('import.actions.resolve')}
                                                    </button>
                                                    {item.status === 'UNRESOLVED' && (
                                                        <button 
                                                            onClick={() => updateItemStatus(item.id, 'IGNORED')}
                                                            className="flex items-center gap-2 px-3 py-1.5 bg-gray-50 text-gray-400 hover:text-rose-600 hover:bg-rose-50 hover:border-rose-100 rounded-lg transition-all font-black text-[10px] uppercase tracking-widest border border-gray-200 shadow-sm transform active:scale-95"
                                                            title={t('import.actions.ignore', "Ignore")}
                                                        >
                                                            <FaBan size={14} />
                                                            {t('import.actions.ignore')}
                                                        </button>
                                                    )}
                                                </>
                                            )}
                                            {item.status === 'ERROR' && (
                                                <button 
                                                    onClick={() => handleRetry(item.id)}
                                                    className="flex items-center gap-2 px-3 py-1.5 bg-amber-50 text-amber-600 hover:bg-amber-100 rounded-lg transition-all font-black text-[10px] uppercase tracking-widest border border-amber-100 shadow-sm transform active:scale-95"
                                                    title={t('import.actions.retry', "Retry")}
                                                >
                                                    <FaRedo size={12} />
                                                    {t('import.actions.retry')}
                                                </button>
                                            )}
                                            {item.status === 'IGNORED' && (
                                                <button 
                                                    onClick={() => updateItemStatus(item.id, 'UNRESOLVED')}
                                                    className="flex items-center gap-2 px-3 py-1.5 bg-emerald-50 text-emerald-600 hover:bg-emerald-100 rounded-lg transition-all font-black text-[10px] uppercase tracking-widest border border-emerald-100 shadow-sm transform active:scale-95"
                                                >
                                                    <FaSyncAlt size={12} />
                                                    {t('import.actions.restore', "Restore")}
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {filteredItems.length === 0 && (
                                <tr>
                                    <td colSpan="5" className="px-6 py-12 text-center text-gray-400 italic text-sm">
                                        {t('common.noItemsFound', 'No items found matching the filter.')}
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default ImportSummaryDashboard;
