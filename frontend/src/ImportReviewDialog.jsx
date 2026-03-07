import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FaCheckCircle, FaExclamationCircle, FaPlusCircle, FaCopy, FaCheck, FaSpinner } from 'react-icons/fa';
import MergeMetadataView from './MergeMetadataView';
import { fetchWithCsrf } from './api';

const ImportReviewDialog = ({
  stagedUpload,
  existingBook: initialExistingBook,
  draftBook,
  dirtyFields,
  authorOptions,
  seriesOptions,
  onCancel,
  onConfirm,
  isProcessing
}) => {
  const { t } = useTranslation();
  
  const [activeMatch, setActiveMatch] = useState(initialExistingBook || null);
  const [mergedData, setMergedData] = useState(null);
  const [isLoadingMatch, setIsLoadingMatch] = useState(false);
  const [confirmDuplicate, setConfirmDuplicate] = useState(false);

  const candidates = stagedUpload.validation?.candidates || [];
  
  // Find duplicate flag for current active match
  const isDuplicate = activeMatch && candidates.find(c => c.bookId === activeMatch.id)?.duplicateFormat;

  // When a candidate is clicked, fetch its full details
  const handleSelectCandidate = async (candidate) => {
    if (activeMatch?.id === candidate.bookId) return;
    
    setConfirmDuplicate(false); // Reset duplicate confirmation when switching
    setIsLoadingMatch(true);
    try {
      const response = await fetchWithCsrf(`/api/books/${candidate.bookId}`);
      if (response.ok) {
        const fullBook = await response.json();
        setActiveMatch(fullBook);
      }
    } catch (err) {
      console.error("Failed to fetch candidate details:", err);
    } finally {
      setIsLoadingMatch(false);
    }
  };

  const handleSelectNew = () => {
    setActiveMatch(null);
  };

  const handleConfirm = () => {
    if (mergedData) {
      onConfirm({
        ...mergedData,
        uploadId: stagedUpload.id,
        bookId: activeMatch?.id, // Send selected bookId for update, or null for create
        skipFormatLink: isDuplicate && !confirmDuplicate // Skip file if duplicate but checkbox not checked
      });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-5xl overflow-hidden flex flex-col max-h-[95vh] border border-gray-100">
        <header className="px-8 py-6 bg-gray-50/50 border-b flex justify-between items-center">
          <div>
            <h2 className="text-xl font-black text-gray-800 tracking-tight">
              {activeMatch ? t('import.review.titleUpdate') : t('import.review.titleNew')}
            </h2>
            <p className="text-[10px] font-black text-indigo-400 uppercase tracking-widest mt-1">{t('import.review.statusTitle')}</p>
          </div>
          <button 
            onClick={onCancel}
            className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all"
            disabled={isProcessing}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </header>

        <div className="flex-grow overflow-hidden flex flex-col md:row">
          <div className="flex flex-grow overflow-hidden">
            {/* Left Panel: Candidate Selection (if not targeted) */}
            {!initialExistingBook && candidates.length > 0 && (
              <div className="w-full md:w-1/3 bg-gray-50 border-r border-gray-100 overflow-y-auto p-6">
                <div className="space-y-4">
                  <button
                    onClick={handleSelectNew}
                    className={`w-full text-left p-4 rounded-2xl border-2 transition-all flex items-start gap-3 transform active:scale-[0.98]
                      ${!activeMatch ? 'border-indigo-500 bg-white shadow-lg shadow-indigo-100 ring-4 ring-indigo-50' : 'border-transparent bg-white/50 hover:bg-white hover:border-gray-200 shadow-sm'}`}
                  >
                    <FaPlusCircle className={`mt-1 text-lg ${!activeMatch ? 'text-indigo-600' : 'text-gray-300'}`} />
                    <div>
                      <div className="font-black text-gray-800 text-sm tracking-tight">{t('import.review.titleNew')}</div>
                      <div className="text-[10px] text-gray-400 font-bold leading-tight mt-1 uppercase tracking-tighter">Create fresh entry</div>
                    </div>
                  </button>

                  <div className="relative py-2 flex items-center gap-4">
                    <div className="flex-grow h-px bg-gray-200"></div>
                    <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest whitespace-nowrap">Matches Found</span>
                    <div className="flex-grow h-px bg-gray-200"></div>
                  </div>

                  {candidates.map(c => (
                    <button
                      key={c.bookId}
                      onClick={() => handleSelectCandidate(c)}
                      className={`w-full text-left p-4 rounded-2xl border-2 transition-all flex items-start gap-3 transform active:scale-[0.98]
                        ${activeMatch?.id === c.bookId ? 'border-emerald-500 bg-white shadow-lg shadow-emerald-100 ring-4 ring-emerald-50' : 'border-transparent bg-white/50 hover:bg-white hover:border-gray-200 shadow-sm'}`}
                    >
                      {c.score >= 80 ? (
                        <FaCheckCircle className={`mt-1 text-lg ${activeMatch?.id === c.bookId ? 'text-emerald-600' : 'text-emerald-300'}`} />
                      ) : (
                        <FaExclamationCircle className={`mt-1 text-lg ${activeMatch?.id === c.bookId ? 'text-amber-500' : 'text-amber-300'}`} />
                      )}
                      <div className="overflow-hidden">
                        <div className="font-black text-gray-800 text-sm tracking-tight truncate" title={c.title}>{c.title}</div>
                        <div className="text-[10px] text-gray-400 font-bold truncate mt-0.5">{c.authors.join(', ') || 'Unknown Author'}</div>
                        <div className={`mt-2 inline-block px-2 py-0.5 rounded-full text-[9px] font-black uppercase tracking-tighter ${c.score >= 80 ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}>
                          Score: {c.score}%
                        </div>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Right Panel: Metadata Merge */}
            <div className={`flex-grow p-8 overflow-y-auto custom-scrollbar ${isLoadingMatch ? 'opacity-50 pointer-events-none' : ''}`}>
              <div className="mb-8 p-4 bg-blue-50/50 rounded-2xl border border-blue-100 text-sm flex items-center gap-4">
                <div className="bg-blue-500 text-white p-2 rounded-lg shadow-sm">
                  <FaCopy size={12} />
                </div>
                <div>
                  <p className="text-blue-900 font-bold leading-tight">{t('import.review.intro')}</p>
                  <p className="text-[10px] text-blue-600 font-black uppercase tracking-widest mt-1">
                    {t('import.review.fileName')}: <span className="font-mono">{stagedUpload.fileName}</span>
                  </p>
                </div>
              </div>

              {isDuplicate && (
                <div className="mb-8 p-6 bg-rose-50 border-2 border-rose-100 rounded-2xl shadow-sm">
                  <div className="flex items-start gap-4">
                    <div className="bg-rose-500 text-white p-3 rounded-xl shadow-sm">
                      <FaExclamationCircle size={20} />
                    </div>
                    <div>
                      <h4 className="font-black text-rose-900 uppercase text-xs tracking-widest mb-1">Potential Duplicate Detected</h4>
                      <p className="text-sm text-rose-700 font-medium">
                        This book already has a format with the same file name or size. Adding it again might create a duplicate.
                      </p>
                      <label className="mt-4 flex items-center gap-3 cursor-pointer group">
                        <div className="relative">
                          <input 
                            type="checkbox" 
                            checked={confirmDuplicate} 
                            onChange={(e) => setConfirmDuplicate(e.target.checked)}
                            className="peer sr-only"
                          />
                          <div className="w-10 h-6 bg-rose-200 rounded-full peer peer-checked:bg-rose-600 transition-colors"></div>
                          <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full transition-transform peer-checked:translate-x-4"></div>
                        </div>
                        <span className="text-sm font-black text-rose-900 group-hover:text-rose-700 transition-colors uppercase tracking-tight">
                          Yes, add this potential duplicate
                        </span>
                      </label>
                    </div>
                  </div>
                </div>
              )}

              <MergeMetadataView 
                stagedUpload={stagedUpload}
                existingBook={activeMatch}
                draftBook={draftBook}
                dirtyFields={dirtyFields}
                authorOptions={authorOptions}
                seriesOptions={seriesOptions}
                onMergedDataChange={setMergedData}
              />
            </div>
          </div>
        </div>

        <footer className="px-8 py-6 bg-gray-50 border-t border-gray-100 flex justify-end gap-4">
          <button 
            onClick={onCancel} 
            className="px-8 py-3 text-gray-500 bg-white border border-gray-200 rounded-2xl hover:bg-gray-50 font-black text-xs uppercase tracking-widest transition-all transform active:scale-95 shadow-sm"
            disabled={isProcessing}
          >
            {t('common.cancel')}
          </button>
          <button 
            onClick={handleConfirm} 
            disabled={isProcessing || !mergedData || isLoadingMatch}
            className={`px-10 py-3 text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl transition-all transform active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2
              ${activeMatch 
                ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-100 hover:-translate-y-1' 
                : 'bg-indigo-600 hover:bg-indigo-700 shadow-indigo-100 hover:-translate-y-1'}`}
          >
            {isProcessing ? <FaSpinner className="animate-spin" /> : <FaCheck />}
            {activeMatch ? t('import.review.buttonUpdate', 'Finalize & Update') : t('import.review.buttonImport', 'Finalize & Import')}
          </button>
        </footer>
      </div>
    </div>
  );
};

export default ImportReviewDialog;
