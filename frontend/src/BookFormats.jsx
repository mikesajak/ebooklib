import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FaTrash, FaPlus, FaSpinner, FaFileAlt, FaDownload } from 'react-icons/fa';
import ConfirmationDialog from './ConfirmationDialog';
import ImportReviewDialog from './ImportReviewDialog';
import { fetchWithCsrf } from './api';

const BookFormats = ({ bookId, formats: initialFormats, onUpdate }) => {
  const { t } = useTranslation();
  const [formats, setFormats] = useState(initialFormats || []);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);
  const [showDeleteConfirmDialog, setShowDeleteConfirmDialog] = useState(false);
  const [formatToDelete, setFormatToDelete] = useState(null);

  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [stagedUpload, setStagedUpload] = useState(null);
  const [isFinalizing, setIsFinalizing] = useState(false);

  const [authors, setAuthors] = useState([]);
  const [series, setSeries] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [authorsResponse, seriesResponse] = await Promise.all([
          fetchWithCsrf('/api/authors?size=1000&sort=firstName,asc&sort=lastName,asc'),
          fetchWithCsrf('/api/series?size=1000&sort=title,asc')
        ]);

        if (authorsResponse.ok) {
          const data = await authorsResponse.json();
          setAuthors(data.content || []);
        }
        if (seriesResponse.ok) {
          const data = await seriesResponse.json();
          setSeries(data.content || []);
        }
      } catch (err) {
        console.error("Failed to fetch reference data:", err);
      }
    };
    fetchData();
  }, []);

  const authorOptions = useMemo(() => 
    authors.map(a => ({ id: a.id, name: `${a.firstName} ${a.lastName}` })), 
    [authors]
  );

  const seriesOptions = useMemo(() => 
    series.map(s => ({ id: s.id, name: s.title })), 
    [series]
  );

  const fetchFormats = async () => {
    setLoading(true);
    try {
      const response = await fetchWithCsrf(`/api/books/${bookId}/formats`);
      if (!response.ok) throw new Error('Failed to fetch book formats');
      const data = await response.json();
      setFormats(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const formatBytes = (bytes, decimals = 2) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  };

  const handleFileChange = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setUploading(true);
    setProgress(0);

    const formData = new FormData();
    formData.append('file', file);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `/api/import/upload?currentBookId=${bookId}`);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        setProgress(Math.round((e.loaded * 100) / e.total));
      }
    };

    xhr.onload = () => {
      setUploading(false);
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const data = JSON.parse(xhr.responseText);
          setStagedUpload(data);
        } catch (e) {
          setError(t('bookFormats.parseFailure'));
        }
      } else {
        let errorMessage = t('bookFormats.uploadFailure');
        try {
          const errorData = JSON.parse(xhr.responseText);
          errorMessage = errorData.message || errorMessage;
        } catch (e) {}
        setError(errorMessage);
      }
    };

    xhr.onerror = () => {
      setUploading(false);
      setError(t('bookFormats.networkError'));
    };

    xhr.send(formData);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleFinalize = async (mergedData) => {
    setIsFinalizing(true);
    try {
      const response = await fetchWithCsrf('/api/import/finalize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...mergedData,
          bookId: bookId 
        }),
      });

      if (!response.ok) throw new Error(t('import.finalizeError'));

      setStagedUpload(null);
      fetchFormats();
      if (onUpdate) onUpdate();
    } catch (err) {
      setError(err.message);
    } finally {
      setIsFinalizing(false);
    }
  };

  const handleDeleteClick = (format) => {
    setFormatToDelete(format);
    setShowDeleteConfirmDialog(true);
  };

  const handleConfirmDelete = async () => {
    if (!formatToDelete) return;
    try {
      const response = await fetchWithCsrf(`/api/books/${bookId}/formats/${formatToDelete.id}`, {
        method: 'DELETE',
      });
      if (!response.ok) throw new Error(t('bookFormats.deleteFailure'));
      fetchFormats();
      if (onUpdate) onUpdate();
    } catch (err) {
      setError(err.message);
    } finally {
      setShowDeleteConfirmDialog(false);
      setFormatToDelete(null);
    }
  };

  return (
    <div className="space-y-4">
      {error && (
        <div className="p-3 bg-rose-50 border border-rose-100 text-rose-600 text-xs font-bold rounded-xl animate-fade-in flex justify-between items-center">
          <span>{error}</span>
          <button onClick={() => setError(null)}>✖</button>
        </div>
      )}

      {formats.length === 0 && !loading && !uploading && (
        <div className="p-8 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-200">
          <p className="text-sm text-gray-400 font-medium italic">{t('bookFormats.noFormats')}</p>
        </div>
      )}

      <div className="space-y-3">
        {formats.map(format => (
          <div key={format.id} className="group flex items-center justify-between p-4 bg-gray-50/50 hover:bg-white rounded-2xl border border-transparent hover:border-indigo-100 hover:shadow-md transition-all">
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-xl bg-white border border-gray-100 text-indigo-500 shadow-sm">
                <FaFileAlt size={16} />
              </div>
              <div>
                <div className="flex flex-col">
                  <span className="text-sm font-black text-gray-800 tracking-tight truncate max-w-[200px] sm:max-w-[300px]" title={format.fileName}>
                    {format.fileName}
                  </span>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="px-1.5 py-0.5 bg-indigo-50 text-indigo-600 text-[9px] font-black rounded border border-indigo-100 uppercase tracking-tighter">
                      {format.formatType}
                    </span>
                    <span className="text-[9px] font-black text-gray-400 uppercase tracking-tighter">
                      {formatBytes(format.size)}
                    </span>
                  </div>
                </div>
                <a 
                  href={`/api/books/${bookId}/formats/${format.id}/download`}
                  className="text-[10px] font-black text-indigo-600 hover:text-indigo-800 uppercase tracking-widest flex items-center gap-1 mt-1.5 transition-colors"
                >
                  <FaDownload size={8} /> {t('common.download', 'Download')}
                </a>
              </div>
            </div>
            <button 
              onClick={() => handleDeleteClick(format)} 
              className="p-2 text-gray-300 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-all opacity-0 group-hover:opacity-100"
              title={t('common.delete')}
            >
              <FaTrash size={14} />
            </button>
          </div>
        ))}

        {uploading && (
          <div className="p-4 bg-indigo-50/50 rounded-2xl border border-indigo-100 animate-pulse">
            <div className="flex justify-between items-center mb-2">
              <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest flex items-center gap-2">
                <FaSpinner className="animate-spin" /> {t('bookFormats.processing')}
              </span>
              <span className="text-[10px] font-black text-indigo-600">{progress}%</span>
            </div>
            <div className="w-full bg-indigo-100 rounded-full h-1.5 overflow-hidden">
              <div className="bg-indigo-600 h-full transition-all duration-300" style={{ width: `${progress}%` }}></div>
            </div>
          </div>
        )}
      </div>

      <div className="pt-2">
        <input
          type="file"
          ref={fileInputRef}
          className="hidden"
          onChange={handleFileChange}
          accept=".epub,.pdf,.mobi,.azw3"
        />
        <button 
          type="button" 
          onClick={() => fileInputRef.current.click()} 
          disabled={uploading}
          className={`w-full py-3 rounded-2xl font-black text-xs uppercase tracking-[0.2em] transition-all transform active:scale-95 shadow-xl flex items-center justify-center gap-2
            ${uploading 
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none' 
              : 'bg-indigo-600 text-white shadow-indigo-100 hover:bg-indigo-700 hover:-translate-y-1'}`}
        >
          {uploading ? <FaSpinner className="animate-spin" /> : <FaPlus />}
          {t('addBook.form.addFormat')}
        </button>
      </div>

      {stagedUpload && (
        <ImportReviewDialog
          stagedUpload={stagedUpload}
          authorOptions={authorOptions}
          seriesOptions={seriesOptions}
          onCancel={() => setStagedUpload(null)}
          onConfirm={handleFinalize}
          isProcessing={isFinalizing}
        />
      )}

      {showDeleteConfirmDialog && (
        <ConfirmationDialog
          title={t('bookFormats.deleteConfirmation.title', 'Delete Format')}
          message={t('bookFormats.deleteConfirmation.message', { formatType: formatToDelete?.formatType })}
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowDeleteConfirmDialog(false)}
          confirmButtonText={t('common.delete')}
          cancelButtonText={t('common.cancel')}
        />
      )}
    </div>
  );
};

export default BookFormats;
