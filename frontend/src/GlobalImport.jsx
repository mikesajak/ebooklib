import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaCloudUploadAlt, FaFileImport } from 'react-icons/fa';
import { fetchWithCsrf } from './api';
import Notification from './Notification';
import ImportReviewDialog from './ImportReviewDialog';

const GlobalImport = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  
  const [isUploading, setIsUploading] = useState(false);
  const [isFinalizing, setIsFinalizing] = useState(false);
  const [stagedUpload, setStagedUpload] = useState(null);
  const [notification, setNotification] = useState(null);
  const [dragActive, setDragActive] = useState(false);

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

  const handleFile = async (file) => {
    if (!file) return;

    setIsUploading(true);
    setNotification(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetchWithCsrf('/api/import/upload', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(t('import.uploadError'));
      }

      const data = await response.json();
      setStagedUpload(data);
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
    } finally {
      setIsUploading(false);
    }
  };

  const onFileSelect = (e) => {
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleFinalize = async (mergedData) => {
    setIsFinalizing(true);
    try {
      const response = await fetchWithCsrf('/api/import/finalize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(mergedData),
      });

      if (!response.ok) {
        throw new Error(t('import.finalizeError'));
      }

      const finalizedBook = await response.json();
      navigate(`/book/${finalizedBook.id}`, { 
        state: { 
          notification: { 
            type: 'success', 
            message: t('import.success', { title: finalizedBook.title }) 
          } 
        } 
      });
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
      setIsFinalizing(false);
    }
  };

  return (
    <div className="container mx-auto p-6 max-w-4xl">
      <div className="flex items-center gap-3 mb-6">
        <FaFileImport className="text-3xl text-blue-600" />
        <h1 className="text-3xl font-bold text-gray-800">{t('import.title')}</h1>
      </div>

      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}

      <div className="bg-white rounded-xl shadow-lg border border-gray-200 overflow-hidden">
        <div className="p-8">
          <p className="text-gray-600 mb-8 leading-relaxed">
            {t('import.description')}
          </p>

          <form 
            className={`relative group border-4 border-dashed rounded-2xl p-12 transition-all flex flex-col items-center justify-center text-center
              ${dragActive ? 'border-blue-500 bg-blue-50 scale-[1.01]' : 'border-gray-200 bg-gray-50 hover:border-gray-300'}`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <input
              type="file"
              id="file-upload"
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer disabled:cursor-not-allowed"
              onChange={onFileSelect}
              accept=".epub,.pdf,.mobi,.azw3"
              disabled={isUploading}
            />
            
            <div className={`p-6 rounded-full mb-4 transition-colors ${dragActive ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-400 group-hover:text-gray-500'}`}>
              <FaCloudUploadAlt className={`text-6xl ${isUploading ? 'animate-bounce' : ''}`} />
            </div>

            <div className="space-y-2">
              <p className="text-xl font-bold text-gray-700">
                {isUploading ? t('import.uploading') : t('import.dropZoneTitle')}
              </p>
              <p className="text-gray-500">
                {t('import.dropZoneSubtitle')}
              </p>
            </div>

            {!isUploading && (
              <button 
                type="button"
                className="mt-6 px-8 py-3 bg-blue-600 text-white font-bold rounded-lg shadow-md hover:bg-blue-700 transition-colors pointer-events-none"
              >
                {t('import.selectButton')}
              </button>
            )}
          </form>

          <div className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-500 italic">
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 bg-green-500 rounded-full"></div>
              {t('import.featureMetadata')}
            </div>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
              {t('import.featureCovers')}
            </div>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
              {t('import.featureFormats')}
            </div>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 bg-orange-500 rounded-full"></div>
              {t('import.featureMatching')}
            </div>
          </div>
        </div>
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
    </div>
  );
};

export default GlobalImport;
