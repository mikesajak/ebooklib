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
    } catch (e) {}
    throw new Error(errorMessage);
  }

  return response.json();
};

const SectionHeader = ({ icon: Icon, title, description }) => (
  <div className="mb-4">
    <div className="flex items-center gap-2 text-amber-900 mb-1">
      <Icon className="text-amber-500" />
      <h3 className="font-extrabold uppercase text-xs tracking-widest">{title}</h3>
    </div>
    {description && <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">{description}</p>}
  </div>
);

const InputCard = ({ children, isDirty }) => (
  <div className={`p-6 rounded-2xl border-2 transition-all shadow-sm mb-10 ${isDirty ? 'bg-yellow-50 border-yellow-200 ring-4 ring-yellow-50' : 'bg-white border-gray-100'}`}>
    {children}
  </div>
);

const AddSeries = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditMode = !!id;
  const [series, setSeries] = useState({ title: '', description: '' });
  const [originalSeries, setOriginalSeries] = useState(null);
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
      onSuccess: (data) => {
        const successMessage = t(isEditMode ? 'addSeries.updateSuccess' : 'addSeries.successMessage');
        navigate(`/series/${data.id}`, { state: { notification: { type: 'success', message: successMessage } } });
      }
    }
  );

  useEffect(() => {
    const fetchSeries = async () => {
      try {
        setLoading(true);
        const response = await fetchWithCsrf(`/api/series/${id}`);
        if (!response.ok) throw new Error('Failed to fetch series details');
        const data = await response.json();
        setSeries(data);
        setOriginalSeries(data);
      } catch (err) {
        setNotification({ type: 'error', message: err.message });
      } finally {
        setLoading(false);
      }
    };

    if (isEditMode) fetchSeries();
    else setLoading(false);
  }, [id, isEditMode, setNotification]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    markDirty(name);
    setSeries(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = () => mutate(series);
  const handleCancel = () => navigate(isEditMode ? `/series/${id}` : '/series');

  const hasChanges = () => {
    if (!originalSeries || !series) return false;
    const normalize = (val) => val || '';
    if (normalize(originalSeries.title) !== normalize(series.title)) return true;
    if (normalize(originalSeries.description) !== normalize(series.description)) return true;
    return false;
  };

  const isSaveDisabled = !series.title.trim() || isSaving || (isEditMode && !hasChanges());



  if (loading) return <AddPage title={t(isEditMode ? 'addSeries.editTitle' : 'addSeries.title')} color="amber" icon={FaLayerGroup}><p>{t('common.loading')}</p></AddPage>;

  return (
    <AddPage title={t(isEditMode ? 'addSeries.editTitle' : 'addSeries.title')} notification={notification} setNotification={setNotification} color="amber" icon={FaLayerGroup}>
      <Form onSave={handleSave} onCancel={handleCancel} isSaveDisabled={isSaveDisabled} color="amber">
        
        <SectionHeader icon={FaLayerGroup} title={t('addSeries.section.identity', 'Series Identity')} description="Key information for the book series." />
        <InputCard isDirty={dirtyFields.has('title')}>
          <div className="flex items-center gap-2 mb-3">
            <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="title">
              {t('addSeries.form.name')}
            </label>
            {dirtyFields.has('title') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
          </div>
          <input type="text" id="title" name="title" value={series.title} onChange={handleChange} className={`shadow-sm appearance-none border-2 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-amber-500/10 focus:border-amber-500 transition-all ${dirtyFields.has('title') ? 'border-yellow-300' : 'border-gray-100 bg-gray-50/30'}`} />
        </InputCard>

        <SectionHeader icon={FaInfoCircle} title={t('addSeries.section.description', 'Description')} description="Additional context and summary of the series." />
        <InputCard isDirty={dirtyFields.has('description')}>
          <div className="mb-2">
            <div className="flex items-center gap-2 mb-3">
              <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="description">{t('addSeries.form.description', 'Description')}</label>
              {dirtyFields.has('description') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <textarea id="description" name="description" value={series.description || ''} onChange={handleChange} rows="6" className={`shadow-sm appearance-none border-2 rounded-2xl w-full py-3 px-4 text-gray-700 leading-tight font-medium focus:outline-none focus:ring-4 focus:ring-amber-500/10 focus:border-amber-500 transition-all ${dirtyFields.has('description') ? 'border-yellow-300' : 'border-gray-100 bg-gray-50/30'}`} />
          </div>
        </InputCard>
      </Form>
    </AddPage>
  );
};

export default AddSeries;
