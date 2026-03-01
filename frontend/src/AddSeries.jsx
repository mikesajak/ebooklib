import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaLayerGroup, FaInfoCircle, FaPencilAlt } from 'react-icons/fa';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';
import { fetchWithCsrf } from './api';

const saveSeries = async (seriesData, isEditMode, seriesId) => {
  const method = isEditMode ? 'PUT' : 'POST';
  const url = isEditMode ? `/api/series/${seriesId}` : '/api/series';

  const response = await fetchWithCsrf(url, {
    method: method,
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(seriesData),
  });

  if (!response.ok) {
    let errorMessage = 'Failed to save series';
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorMessage;
    } catch (e) {
      // ignore if response is not json
    }
    throw new Error(errorMessage);
  }

  return response.json();
};

const AddSeries = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams(); // Get series ID from URL
  const isEditMode = !!id; // Determine if in edit mode
  const [series, setSeries] = useState({
    title: '',
    description: ''
  });
  const [originalSeries, setOriginalSeries] = useState(null); // Store original series data for comparison
  const [loading, setLoading] = useState(true);
  const [dirtyFields, setDirtyFields] = useState(new Set());

  const markDirty = (fieldName) => {
    setDirtyFields(prev => {
      const next = new Set(prev);
      next.add(fieldName);
      return next;
    });
  };

  const { mutate, isSaving, notification, setNotification } = useMutation(
    (seriesData) => saveSeries(seriesData, isEditMode, id),
    {
      onSuccess: (savedSeries) => {
        navigate(`/series/${savedSeries.id}`, { state: { notification: { type: 'success', message: t(isEditMode ? 'addSeries.updateSuccess' : 'addSeries.addSuccess') } } });
      }
    }
  );

  useEffect(() => {
    const fetchSeries = async () => {
      try {
        setLoading(true);
        const response = await fetchWithCsrf(`/api/series/${id}`);
        if (!response.ok) {
          throw new Error('Failed to fetch series details');
        }
        const data = await response.json();
        setSeries(data);
        setOriginalSeries(data); // Store original data
      } catch (err) {
        setNotification({ type: 'error', message: err.message });
      } finally {
        setLoading(false);
      }
    };

    if (isEditMode) {
      fetchSeries();
    } else {
      setLoading(false); // No need to load if not in edit mode
    }
  }, [id, isEditMode, setNotification]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    markDirty(name);
    setSeries(prevSeries => ({
      ...prevSeries,
      [name]: value
    }));
  };

  const handleSave = () => {
    mutate(series);
  };

  const handleCancel = () => {
    navigate(isEditMode ? `/series/${id}` : '/series');
  };

  const isFormValid = series.title.trim() !== '';

  const hasChanges = () => {
    if (!originalSeries || !series) return false;

    const normalize = (val) => val || '';

    if (normalize(originalSeries.title) !== normalize(series.title)) return true;
    if (normalize(originalSeries.description) !== normalize(series.description)) return true;

    return false;
  };

  const isSaveDisabled = !isFormValid || isSaving || (isEditMode && !hasChanges());

  const SectionHeader = ({ icon: Icon, title, description }) => (
    <div className="mb-4">
      <div className="flex items-center gap-2 text-amber-900 mb-1">
        <Icon className="text-amber-500" />
        <h3 className="font-extrabold uppercase text-xs tracking-widest">{title}</h3>
      </div>
      {description && <p className="text-xs text-gray-500">{description}</p>}
    </div>
  );

  const InputCard = ({ children, isDirty }) => (
    <div className={`p-4 rounded-xl border-2 transition-all shadow-sm mb-6 ${isDirty ? 'bg-yellow-50 border-yellow-200 ring-1 ring-yellow-100' : 'bg-white border-gray-100'}`}>
      {children}
    </div>
  );

  if (loading) {
    return (
      <AddPage title={t(isEditMode ? 'addSeries.editTitle' : 'addSeries.title')} notification={notification} setNotification={setNotification}>
        <p>{t('common.loading')}</p>
      </AddPage>
    );
  }

  return (
    <AddPage title={t(isEditMode ? 'addSeries.editTitle' : 'addSeries.title')} notification={notification} setNotification={setNotification}>
      <Form onSave={handleSave} onCancel={handleCancel} isSaveDisabled={isSaveDisabled}>
        
        <SectionHeader icon={FaLayerGroup} title="Series Information" description="Basic details for the series." />
        <InputCard isDirty={dirtyFields.has('title') || dirtyFields.has('description')}>
          <div className="mb-6">
            <div className="flex items-center gap-2 mb-2">
              <label className="block text-gray-700 text-sm font-bold" htmlFor="title">
                {t('addSeries.form.title')}
              </label>
              {dirtyFields.has('title') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <input type="text" id="title" name="title" value={series.title || ''} onChange={handleChange} className={`shadow-sm appearance-none border rounded-lg w-full py-2.5 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-2 focus:ring-amber-500 transition-all ${dirtyFields.has('title') ? 'border-yellow-300' : 'border-gray-300'}`} />
          </div>

          <div>
            <div className="flex items-center gap-2 mb-2">
              <label className="block text-gray-700 text-sm font-bold" htmlFor="description">
                {t('addSeries.form.description')}
              </label>
              {dirtyFields.has('description') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <textarea id="description" name="description" value={series.description || ''} onChange={handleChange} rows="4" className={`shadow-sm appearance-none border rounded-lg w-full py-2.5 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-2 focus:ring-amber-500 transition-all ${dirtyFields.has('description') ? 'border-yellow-300' : 'border-gray-300'}`} />
          </div>
        </InputCard>
      </Form>
    </AddPage>
  );
};

export default AddSeries;
