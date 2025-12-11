import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';

const createSeries = async (seriesData) => {
  const response = await fetch('/api/series', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(seriesData),
  });

  if (!response.ok) {
    let errorMessage = 'Failed to create series';
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
  const [series, setSeries] = useState({
    name: '',
  });

  const { mutate, isSaving, notification, setNotification } = useMutation(createSeries, {
    onSuccess: () => {
      navigate('/series', { state: { notification: { type: 'success', message: 'Series added successfully!' } } });
    }
  });

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
    navigate('/series');
  };

  const isFormValid = series.name.trim() !== '';
  const isSaveDisabled = !isFormValid || isSaving;

  return (
    <AddPage title={t('addSeries.title')} notification={notification} setNotification={setNotification}>
      <Form onSave={handleSave} onCancel={handleCancel} isSaveDisabled={isSaveDisabled}>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="name">{t('addSeries.form.name')}:</label>
          <input type="text" id="name" name="name" value={series.name} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
      </Form>
    </AddPage>
  );
};

export default AddSeries;
