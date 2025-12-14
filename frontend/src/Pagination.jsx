import React from 'react';
import { useTranslation } from 'react-i18next';

const Pagination = ({ page, size, totalPages, totalElements, onPageChange, onPageSizeChange }) => {
  const { t } = useTranslation();

  const handlePreviousPage = () => {
    onPageChange(Math.max(0, page - 1));
  };

  const handleNextPage = () => {
    onPageChange(Math.min(totalPages - 1, page + 1));
  };

  const handlePageSizeChange = (event) => {
    onPageSizeChange(Number(event.target.value));
  };

  return (
    <div className="flex justify-between items-center p-4">
      <div>
        {t('common.page')} {page + 1} {t('common.of')} {totalPages} ({totalElements} {t('common.total')})
      </div>

      <div className="flex items-center space-x-2">
        <label htmlFor="pageSize" className="mr-2">{t('common.pageSize')}:</label>
        <select id="pageSize" value={size} onChange={handlePageSizeChange} className="border border-gray-300 rounded p-1">
          <option value="5">5</option>
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
        </select>

        <button
          onClick={handlePreviousPage}
          disabled={page === 0}
          className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50"
        >
          {t('common.previous')}
        </button>

        <button
          onClick={handleNextPage}
          disabled={page === totalPages - 1}
          className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50"
        >
          {t('common.next')}
        </button>
      </div>
    </div>
  );
};

export default Pagination;