import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaSpinner, FaCheck, FaExclamationTriangle, FaFilter, FaSyncAlt, FaArrowLeft, FaBan, FaInfoCircle, FaMagic } from 'react-icons/fa';

const ImportSummaryDashboard = () => {
    const { id: sessionId } = useParams();
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [session, setSession] = useState(null);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isProcessing, setIsProcessing] = useState(false);
    const [filter, setFilter] = useState('ALL'); // ALL, UNRESOLVED, RESOLVED, IGNORED, ERROR
    const [selectedIds, setSelectedIds] = useState([]);

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

    useEffect(() => {
        fetchSession();
        fetchItems();
        const interval = setInterval(() => {
            fetchSession();
            fetchItems();
        }, 5000);
        return () => clearInterval(interval);
    }, [sessionId]);

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

    return (
        <div className="p-6">
            <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate('/import')} className="text-gray-600 hover:text-gray-900">
                        <FaArrowLeft size={20} />
                    </button>
                    <div>
                        <h1 className="text-2xl font-bold">{t('import.dashboardTitle', 'Import Session Dashboard')}</h1>
                        <p className="text-xs text-gray-500 font-mono">{sessionId}</p>
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
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                    <div className="bg-white p-4 rounded-lg shadow">
                        <div className="text-sm text-gray-500 uppercase">{t('import.totalFiles', 'Total Files')}</div>
                        <div className="text-2xl font-bold">{session.totalFiles}</div>
                    </div>
                    <div className="bg-white p-4 rounded-lg shadow border-l-4 border-blue-500">
                        <div className="text-sm text-gray-500 uppercase">{t('import.statusLabel', 'Session Status')}</div>
                        <div className="text-xl font-semibold text-blue-600">
                            {t(`import.status.${session.status.toLowerCase()}`, session.status)}
                        </div>
                    </div>
                    <div className="bg-white p-4 rounded-lg shadow border-l-4 border-green-500">
                        <div className="text-sm text-gray-500 uppercase">{t('import.processed', 'Processed')}</div>
                        <div className="text-2xl font-bold text-green-600">{session.processedFiles}</div>
                    </div>
                    <div className="bg-white p-4 rounded-lg shadow border-l-4 border-red-500">
                        <div className="text-sm text-gray-500 uppercase">{t('import.failed', 'Failed')}</div>
                        <div className="text-2xl font-bold text-red-600">{session.failedFiles}</div>
                    </div>
                </div>
            )}

            <div className="bg-white shadow rounded-lg overflow-hidden">
                <div className="p-4 border-b flex flex-wrap justify-between items-center gap-4">
                    <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2">
                            <FaFilter className="text-gray-400" />
                            <select 
                                value={filter} 
                                onChange={(e) => { setFilter(e.target.value); setSelectedIds([]); }}
                                className="border rounded px-2 py-1 text-sm focus:ring-blue-500 focus:border-blue-500"
                            >
                                <option value="ALL">{t('import.filters.all', 'All Items')}</option>
                                <option value="UNRESOLVED">{t('import.filters.unresolved', 'Unresolved')}</option>
                                <option value="RESOLVED">{t('import.filters.resolved', 'Resolved')}</option>
                                <option value="IGNORED">{t('import.filters.ignored', 'Ignored')}</option>
                                <option value="ERROR">{t('import.filters.error', 'Error')}</option>
                            </select>
                        </div>

                        {selectedIds.length > 0 && (
                            <div className="flex items-center gap-2 border-l pl-4">
                                <span className="text-sm font-medium text-gray-700">
                                    {t('common.selectedCount', '{{count}} selected', { count: selectedIds.length })}
                                </span>
                                <button 
                                    onClick={() => bulkUpdateStatus('IGNORED')}
                                    className="bg-gray-100 text-gray-700 px-3 py-1 rounded text-sm hover:bg-gray-200"
                                >
                                    {t('import.actions.ignoreSelected', 'Ignore Selected')}
                                </button>
                                <button 
                                    onClick={() => bulkUpdateStatus('UNRESOLVED')}
                                    className="bg-gray-100 text-gray-700 px-3 py-1 rounded text-sm hover:bg-gray-200"
                                >
                                    {t('import.actions.restoreSelected', 'Restore Selected')}
                                </button>
                                
                                <div className="relative group">
                                    <button 
                                        className="bg-purple-100 text-purple-700 px-3 py-1 rounded text-sm hover:bg-purple-200 font-bold flex items-center gap-1"
                                        disabled={isProcessing}
                                    >
                                        <FaMagic className={isProcessing ? "animate-spin" : ""} /> {t('import.autoResolveSelected')}
                                    </button>
                                    <div className="absolute left-0 mt-1 w-48 bg-white border border-gray-200 rounded-lg shadow-xl hidden group-hover:block z-50 overflow-hidden">
                                        <button onClick={() => handleAutoResolve('TRUST_INCOMING')} className="w-full text-left px-4 py-2 text-sm hover:bg-purple-50 transition-colors">{t('import.strategies.trustIncoming')}</button>
                                        <button onClick={() => handleAutoResolve('TRUST_EXISTING')} className="w-full text-left px-4 py-2 text-sm hover:bg-purple-50 transition-colors">{t('import.strategies.trustExisting')}</button>
                                        <button onClick={() => handleAutoResolve('NEW_ONLY')} className="w-full text-left px-4 py-2 text-sm hover:bg-purple-50 transition-colors">{t('import.strategies.newOnly')}</button>
                                    </div>
                                </div>
                            </div>
                        )}

                        {selectedIds.length === 0 && items.some(i => i.status === 'UNRESOLVED') && (
                            <div className="relative group border-l pl-4">
                                <button 
                                    className="text-purple-600 px-3 py-1 rounded text-sm hover:bg-purple-50 font-bold flex items-center gap-1 border border-purple-200"
                                    disabled={isProcessing}
                                >
                                    <FaMagic className={isProcessing ? "animate-spin" : ""} /> {t('import.autoResolveAll')}
                                </button>
                                <div className="absolute left-0 mt-1 w-48 bg-white border border-gray-200 rounded-lg shadow-xl hidden group-hover:block z-50 overflow-hidden">
                                    <button onClick={() => handleAutoResolve('TRUST_INCOMING')} className="w-full text-left px-4 py-2 text-sm hover:bg-purple-50 transition-colors">{t('import.strategies.trustIncoming')}</button>
                                    <button onClick={() => handleAutoResolve('TRUST_EXISTING')} className="w-full text-left px-4 py-2 text-sm hover:bg-purple-50 transition-colors">{t('import.strategies.trustExisting')}</button>
                                    <button onClick={() => handleAutoResolve('NEW_ONLY')} className="w-full text-left px-4 py-2 text-sm hover:bg-purple-50 transition-colors">{t('import.strategies.newOnly')}</button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left w-10">
                                <input 
                                    type="checkbox" 
                                    className="rounded text-blue-600 focus:ring-blue-500"
                                    checked={filteredItems.length > 0 && selectedIds.length === filteredItems.length}
                                    onChange={toggleSelectAll}
                                />
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('import.table.bookDetails', 'Book Details')}</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('import.table.formats', 'Formats')}</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('import.table.status', 'Status')}</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">{t('import.table.actions', 'Actions')}</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {filteredItems.map(item => (
                            <tr key={item.id} className={selectedIds.includes(item.id) ? 'bg-blue-50' : ''}>
                                <td className="px-6 py-4">
                                    <input 
                                        type="checkbox" 
                                        className="rounded text-blue-600 focus:ring-blue-500"
                                        checked={selectedIds.includes(item.id)}
                                        onChange={() => toggleSelect(item.id)}
                                    />
                                </td>
                                <td className="px-6 py-4">
                                    <div className="text-sm font-medium text-gray-900">{item.title}</div>
                                    <div className="text-xs text-gray-500">{item.authors.join(', ')}</div>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex flex-wrap gap-1">
                                        {item.formats.map(f => (
                                            <span key={f.uploadId} className="px-2 py-0.5 bg-blue-100 text-blue-800 text-xs rounded" title={f.fileName}>
                                                {f.contentType.split('/').pop().toUpperCase()}
                                            </span>
                                        ))}
                                    </div>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    {item.status === 'UNRESOLVED' && <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">{t('import.status.unresolved', 'Unresolved')}</span>}
                                    {item.status === 'RESOLVED' && <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">{t('import.status.resolved', 'Resolved')}</span>}
                                    {item.status === 'IGNORED' && <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800">{t('import.status.ignored', 'Ignored')}</span>}
                                    {item.status === 'ERROR' && <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">{t('import.status.error', 'Error')}</span>}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                    <div className="flex justify-end gap-2">
                                        {(item.status === 'UNRESOLVED' || item.status === 'RESOLVED') && (
                                            <>
                                                <button 
                                                    onClick={() => navigate(`/import/resolve/${item.id}`)}
                                                    className="text-blue-600 hover:text-blue-900 flex items-center gap-1"
                                                    title={item.status === 'RESOLVED' ? t('import.actions.edit', "Edit Resolution") : t('import.actions.resolve', "Resolve")}
                                                >
                                                    <FaInfoCircle /> {item.status === 'RESOLVED' ? t('import.actions.edit', "Edit") : t('import.actions.resolve', "Resolve")}
                                                </button>
                                                {item.status === 'UNRESOLVED' && (
                                                    <button 
                                                        onClick={() => updateItemStatus(item.id, 'IGNORED')}
                                                        className="text-gray-600 hover:text-gray-900 flex items-center gap-1"
                                                        title={t('import.actions.ignore', "Ignore")}
                                                    >
                                                        <FaBan /> {t('import.actions.ignore', "Ignore")}
                                                    </button>
                                                )}
                                            </>
                                        )}
                                        {item.status === 'IGNORED' && (
                                            <button 
                                                onClick={() => updateItemStatus(item.id, 'UNRESOLVED')}
                                                className="text-blue-600 hover:text-blue-900"
                                            >
                                                {t('import.actions.restore', "Restore")}
                                            </button>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {filteredItems.length === 0 && (
                            <tr>
                                <td colSpan="5" className="px-6 py-10 text-center text-gray-500 italic">
                                    {t('common.noItemsFound', 'No items found matching the filter.')}
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default ImportSummaryDashboard;
