import { getCsrfToken } from './api-utils';

export const fetchWithCsrf = async (url, options = {}) => {
  const method = (options.method || 'GET').toUpperCase();
  const stateChangingMethods = ['POST', 'PUT', 'DELETE', 'PATCH'];

  const headers = { ...(options.headers || {}) };

  if (stateChangingMethods.includes(method)) {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  return fetch(url, { ...options, headers });
};
