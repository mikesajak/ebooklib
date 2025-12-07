import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const useMutation = (mutationFn, { onSuccess, onError } = {}) => {
  const [isSaving, setIsSaving] = useState(false);
  const [notification, setNotification] = useState(null);
  const navigate = useNavigate();

  const mutate = async (data) => {
    setIsSaving(true);
    setNotification(null);
    try {
      const result = await mutationFn(data);
      if (onSuccess) {
        onSuccess(result);
      }
    } catch (err) {
      if (onError) {
        onError(err);
      } else {
        setNotification({ type: 'error', message: err.message || 'An error occurred' });
      }
    } finally {
      setIsSaving(false);
    }
  };

  return { mutate, isSaving, notification, setNotification };
};

export default useMutation;
