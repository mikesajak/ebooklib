import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaSpinner, FaCheck, FaExclamationTriangle, FaFilter, FaSyncAlt, FaArrowLeft, FaBan, FaInfoCircle } from 'react-icons/fa';

const ImportSummaryDashboard = () => {
    const { id: sessionId } = useParams();
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [session, setSession] = useState(null);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
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

    if (loading) return <div className="p-6 text-center"><FaSpinner className="animate-spin text-4xl mx-auto" /></div>;

    return (
        <div className="p-6">
            <div className="flex items-center gap-4 mb-6">
                <button onClick={() => navigate('/import')} className="text-gray-600 hover:text-gray-900">
                    <FaArrowLeft size={20} />
                </button>
                <h1 className="text-2xl font-bold">{t('import.dashboardTitle', 'Import Session Dashboard')}</h1>
                <span className="text-sm text-gray-500 font-mono">{sessionId}</span>
            </div>

            {session && (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                    <div className="bg-white p-4 rounded-lg shadow">
                        <div className="text-sm text-gray-500 uppercase">{t('import.totalFiles', 'Total Files')}</div>
                        <div className="text-2xl font-bold">{session.totalFiles}</div>
                    </div>
                    <div className="bg-white p-4 rounded-lg shadow border-l-4 border-blue-500">
                        <div className="text-sm text-gray-500 uppercase">{t('import.status', 'Session Status')}</div>
                        <div className="text-xl font-semibold text-blue-600">{session.status}</div>
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
                                <option value="ALL">All Items</option>
                                <option value="UNRESOLVED">Unresolved</option>
                                <option value="RESOLVED">Resolved</option>
                                <option value="IGNORED">Ignored</option>
                                <option value="ERROR">Error</option>
                            </select>
                        </div>

                        {selectedIds.length > 0 && (
                            <div className="flex items-center gap-2 border-l pl-4">
                                <span className="text-sm font-medium text-gray-700">{selectedIds.length} selected</span>
                                <button 
                                    onClick={() => bulkUpdateStatus('IGNORED')}
                                    className="bg-gray-100 text-gray-700 px-3 py-1 rounded text-sm hover:bg-gray-200"
                                >
                                    Ignore Selected
                                </button>
                                <button 
                                    onClick={() => bulkUpdateStatus('UNRESOLVED')}
                                    className="bg-gray-100 text-gray-700 px-3 py-1 rounded text-sm hover:bg-gray-200"
                                >
                                    Restore Selected
                                </button>
                            </div>
                        )}
                    </div>
                    <button onClick={() => { fetchSession(); fetchItems(); }} className="flex items-center gap-2 text-blue-600 hover:text-blue-800 text-sm">
                        <FaSyncAlt /> {t('common.refresh', 'Refresh')}
                    </button>
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
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Book Details</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Formats</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
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
                                    {item.status === 'UNRESOLVED' && <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">Unresolved</span>}
                                    {item.status === 'RESOLVED' && <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">Resolved</span>}
                                    {item.status === 'IGNORED' && <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800">Ignored</span>}
                                    {item.status === 'ERROR' && <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">Error</span>}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                    <div className="flex justify-end gap-2">
                                        {item.status === 'UNRESOLVED' && (
                                            <>
                                                <button 
                                                    onClick={() => navigate(`/import/resolve/${item.id}`)}
                                                    className="text-blue-600 hover:text-blue-900 flex items-center gap-1"
                                                    title="Resolve"
                                                >
                                                    <FaInfoCircle /> Resolve
                                                </button>
                                                <button 
                                                    onClick={() => updateItemStatus(item.id, 'IGNORED')}
                                                    className="text-gray-600 hover:text-gray-900 flex items-center gap-1"
                                                    title="Ignore"
                                                >
                                                    <FaBan /> Ignore
                                                </button>
                                            </>
                                        )}
                                        {item.status === 'IGNORED' && (
                                            <button 
                                                onClick={() => updateItemStatus(item.id, 'UNRESOLVED')}
                                                className="text-blue-600 hover:text-blue-900"
                                            >
                                                Restore
                                            </button>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {filteredItems.length === 0 && (
                            <tr>
                                <td colSpan="5" className="px-6 py-10 text-center text-gray-500 italic">
                                    No items found matching the filter.
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
