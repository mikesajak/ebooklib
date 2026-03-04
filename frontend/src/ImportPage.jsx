import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FaCloudUploadAlt, FaSpinner, FaCheck, FaExclamationTriangle, FaTrash } from 'react-icons/fa';

const ImportPage = () => {
    const { t } = useTranslation();
    const [files, setFiles] = useState([]); // List of file objects
    const [uploads, setUploads] = useState({}); // Key: fileName, Value: { status, id, error, data }

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

    const startUploadAll = async () => {
        const pendingFiles = files.filter(f => uploads[f.name]?.status === 'PENDING');
        for (const file of pendingFiles) {
            uploadFile(file); // Don't await here to run in parallel
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

    const uploadFile = async (file) => {
        setUploads(prev => ({ ...prev, [file.name]: { ...prev[file.name], status: 'UPLOADING', progress: 0 } }));

        const formData = new FormData();
        formData.append('file', file);
        formData.append('async', 'true');

        try {
            const response = await fetch('/api/import/upload?async=true', { 
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
                // Don't mark as failed immediately, maybe transient network error
            }
        }, 1000);
    };

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">{t('import.bulkUploadTitle', 'Bulk Upload')}</h1>
            
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
            
            <div className="flex gap-2 mb-6">
                <button 
                    onClick={startUploadAll}
                    disabled={files.length === 0 || files.every(f => uploads[f.name]?.status !== 'PENDING')}
                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
                >
                    {t('import.startUpload', 'Start Upload')}
                </button>
            </div>

            <div className="bg-white shadow rounded-lg overflow-hidden">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">File Name</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Details</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {files.map(file => {
                            const upload = uploads[file.name] || {};
                            return (
                                <tr key={file.name}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{file.name}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {upload.status === 'PENDING' && <span className="text-gray-500">Pending</span>}
                                        {upload.status === 'UPLOADING' && <span className="text-blue-500 flex items-center"><FaSpinner className="animate-spin mr-2" /> Uploading...</span>}
                                        {upload.status === 'PROCESSING' && <span className="text-orange-500 flex items-center"><FaSpinner className="animate-spin mr-2" /> Processing...</span>}
                                        {upload.status === 'PARSED' && <span className="text-green-500 flex items-center"><FaCheck className="mr-2" /> Parsed</span>}
                                        {upload.status === 'FAILED' && <span className="text-red-500 flex items-center"><FaExclamationTriangle className="mr-2" /> Failed</span>}
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
