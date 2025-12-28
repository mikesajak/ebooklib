import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';

const saveSeries = async (seriesData, isEditMode, seriesId) => {
  const method = isEditMode ? 'PUT' : 'POST';
  const url = isEditMode ? `/api/series/${seriesId}` : '/api/series';

  const response = await fetch(url, {
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
        const response = await fetch(`/api/series/${id}`);
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
  }, [id, isEditMode]);

  const handleChange = (e) => {
    const { name, value } = e.target;
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
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="title">{t('addSeries.form.title')}:</label>
          <input type="text" id="title" name="title" value={series.title} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="description">{t('addSeries.form.description')}:</label>
          <textarea id="description" name="description" value={series.description} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
      </Form>
    </AddPage>
  );
};

export default AddSeries;
