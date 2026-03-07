import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaArrowLeft, FaChevronLeft, FaChevronRight, FaCheck, FaBan, FaSpinner } from 'react-icons/fa';
import MergeMetadataView from './MergeMetadataView';
import { fetchWithCsrf } from './api';

const ResolveItemPage = () => {
    const { id: itemId } = useParams();
    const { t } = useTranslation();
    const navigate = useNavigate();

    const [item, setItem] = useState(null);
    const [allSessionItems, setAllSessionItems] = useState([]);
    const [stagedUpload, setStagedUpload] = useState(null);
    const [existingBook, setExistingBook] = useState(null);
    const [mergedData, setMergedData] = useState(null);
    const [authorOptions, setAuthorOptions] = useState([]);
    const [seriesOptions, setSeriesOptions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isProcessing, setIsProcessing] = useState(false);

    const fetchData = useCallback(async () => {
        try {
            setLoading(true);
            // 1. Fetch the ResolutionItem
            const response = await fetchWithCsrf(`/api/import/items/${itemId}`);
            if (!response.ok) throw new Error('Failed to fetch resolution item');
            const itemData = await response.json();
            setItem(itemData);

            // 2. Fetch all items in session for navigation
            const sessionItemsResponse = await fetchWithCsrf(`/api/import/sessions/${itemData.importSessionId}/items`);
            if (sessionItemsResponse.ok) {
                const sessionItems = await sessionItemsResponse.json();
                setAllSessionItems(sessionItems);
            }

            // 3. Fetch full details of the first format (to get enrichment and validation)
            if (itemData.formats && itemData.formats.length > 0) {
                const uploadResponse = await fetchWithCsrf(`/api/import/staged/${itemData.formats[0].uploadId}`);
                if (uploadResponse.ok) {
                    const uploadData = await uploadResponse.json();
                    
                    // Inject enrichment from item if it's there
                    if (itemData.metadataJson) {
                        try {
                            const enrichment = JSON.parse(itemData.metadataJson);
                            uploadData.metadata = { ...uploadData.metadata, enrichment };
                        } catch (e) {
                            console.error("Failed to parse item enrichment", e);
                        }
                    }
                    
                    setStagedUpload(uploadData);

                    // 4. Fetch the best candidate if available
                    const bestCandidate = uploadData.validation?.candidates?.find(c => c.score >= 80) || uploadData.validation?.candidates?.[0];
                    if (bestCandidate) {
                        const bookResponse = await fetchWithCsrf(`/api/books/${bestCandidate.bookId}`);
                        if (bookResponse.ok) {
                            const bookData = await bookResponse.json();
                            setExistingBook(bookData);
                        }
                    }
                }
            }

            // 5. Fetch options
            const [authorsRes, seriesRes] = await Promise.all([
                fetchWithCsrf('/api/authors'),
                fetchWithCsrf('/api/series')
            ]);
            if (authorsRes.ok) {
                const authorsData = await authorsRes.json();
                setAuthorOptions(authorsData.content.map(a => ({ id: a.id, name: `${a.firstName} ${a.lastName}`.trim() })));
            }
            if (seriesRes.ok) {
                const seriesData = await seriesRes.json();
                setSeriesOptions(seriesData.content.map(s => ({ id: s.id, name: s.title })));
            }

        } catch (error) {
            console.error("Error fetching resolution data:", error);
        } finally {
            setLoading(false);
        }
    }, [itemId]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleConfirm = async () => {
        if (!mergedData || isProcessing) return;
        setIsProcessing(true);
        try {
            const response = await fetchWithCsrf('/api/import/finalize', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ...mergedData,
                    uploadId: stagedUpload.id,
                    bookId: existingBook?.id
                })
            });

            if (response.ok) {
                // Go to next unresolved
                goToNext();
            }
        } catch (error) {
            console.error("Finalization error:", error);
        } finally {
            setIsProcessing(false);
        }
    };

    const handleIgnore = async () => {
        try {
            const response = await fetchWithCsrf(`/api/import/items/${itemId}/status?status=IGNORED`, { method: 'PATCH' });
            if (response.ok) {
                goToNext();
            }
        } catch (error) {
            console.error("Ignore error:", error);
        }
    };

    const unresolvedItems = allSessionItems.filter(i => i.status === 'UNRESOLVED' || i.id === itemId);
    const currentIndex = unresolvedItems.findIndex(i => i.id === itemId);
    const prevItem = currentIndex > 0 ? unresolvedItems[currentIndex - 1] : null;
    const nextItem = currentIndex < unresolvedItems.length - 1 ? unresolvedItems[currentIndex + 1] : null;

    const goToNext = () => {
        if (nextItem) {
            navigate(`/import/resolve/${nextItem.id}`);
        } else {
            // If no more unresolved, go back to dashboard
            navigate(`/import/session/${item.importSessionId}`);
        }
    };

    if (loading) return (
        <div className="flex flex-col items-center justify-center min-h-[60vh]">
            <FaSpinner className="animate-spin text-4xl text-indigo-600 mb-4" />
            <p className="text-gray-500 italic">{t('common.loading')}</p>
        </div>
    );

    if (!item) return (
        <div className="p-8 text-center">
            <h2 className="text-2xl font-bold text-gray-800 mb-4">Item Not Found</h2>
            <Link to="/import" className="text-indigo-600 hover:underline">Back to Import List</Link>
        </div>
    );

    return (
        <div className="max-w-6xl mx-auto p-4 md:p-6">
            <header className="flex flex-wrap items-center justify-between gap-4 mb-8">
                <div className="flex items-center gap-4">
                    <button 
                        onClick={() => navigate(`/import/session/${item.importSessionId}`)}
                        className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-all"
                    >
                        <FaArrowLeft size={20} />
                    </button>
                    <div>
                        <h1 className="text-2xl font-black text-gray-900 tracking-tight">
                            {t('import.resolve.title', 'Resolve Conflict')}
                        </h1>
                        <p className="text-sm text-gray-500 font-medium">
                            {currentIndex + 1} of {unresolvedItems.length} unresolved items
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        onClick={() => navigate(`/import/resolve/${prevItem.id}`)}
                        disabled={!prevItem}
                        className="p-2 border rounded-lg hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                        title="Previous Unresolved"
                    >
                        <FaChevronLeft />
                    </button>
                    <button
                        onClick={() => navigate(`/import/resolve/${nextItem.id}`)}
                        disabled={!nextItem}
                        className="p-2 border rounded-lg hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                        title="Next Unresolved"
                    >
                        <FaChevronRight />
                    </button>
                </div>
            </header>

            <div className="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden flex flex-col min-h-[70vh]">
                <div className="p-6 md:p-8 flex-grow">
                    <div className="mb-6 flex flex-wrap items-center justify-between gap-4 border-b border-gray-100 pb-6">
                        <div>
                            <h2 className="text-xl font-bold text-gray-800">{item.title}</h2>
                            <p className="text-gray-500">{item.authors.join(', ')}</p>
                        </div>
                        <div className="flex gap-2">
                            {item.formats.map(f => (
                                <span key={f.uploadId} className="px-3 py-1 bg-indigo-50 text-indigo-700 text-xs font-bold rounded-full border border-indigo-100 uppercase">
                                    {f.contentType.split('/').pop()}
                                </span>
                            ))}
                        </div>
                    </div>

                    {stagedUpload && (
                        <MergeMetadataView 
                            stagedUpload={stagedUpload}
                            existingBook={existingBook}
                            draftBook={null} // No draft for now in this view
                            dirtyFields={new Set()}
                            authorOptions={authorOptions}
                            seriesOptions={seriesOptions}
                            onMergedDataChange={setMergedData}
                        />
                    )}
                </div>

                <footer className="p-6 bg-gray-50 border-t border-gray-100 flex flex-wrap items-center justify-between gap-4">
                    <button
                        onClick={handleIgnore}
                        className="flex items-center gap-2 px-6 py-3 text-gray-600 hover:text-gray-900 font-bold transition-all"
                    >
                        <FaBan /> {t('import.resolve.ignore', 'Ignore Item')}
                    </button>

                    <div className="flex gap-3">
                        <button
                            onClick={() => navigate(`/import/session/${item.importSessionId}`)}
                            className="px-6 py-3 text-gray-700 bg-white border border-gray-300 rounded-xl hover:bg-gray-50 font-bold transition-all shadow-sm"
                        >
                            {t('common.cancel')}
                        </button>
                        <button
                            onClick={handleConfirm}
                            disabled={isProcessing || !mergedData}
                            className="flex items-center gap-2 px-8 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 font-bold transition-all shadow-lg shadow-indigo-200 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isProcessing ? <FaSpinner className="animate-spin" /> : <FaCheck />}
                            {t('import.resolve.confirm', 'Confirm & Resolve')}
                        </button>
                    </div>
                </footer>
            </div>
        </div>
    );
};

export default ResolveItemPage;
