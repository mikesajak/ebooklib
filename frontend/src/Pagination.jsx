import React from 'react';
import { useTranslation } from 'react-i18next';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';

const Pagination = ({ page, size, totalPages, totalElements, onPageChange, onPageSizeChange, theme = 'indigo' }) => {
  const { t } = useTranslation();

  const themeClasses = {
    indigo: 'bg-indigo-600 border-indigo-600 text-white shadow-indigo-100',
    emerald: 'bg-indigo-600 border-indigo-600 text-white shadow-indigo-100', // Converging to indigo
    amber: 'bg-indigo-600 border-indigo-600 text-white shadow-indigo-100'    // Converging to indigo
  };

  const hoverClasses = {
    indigo: 'hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-200',
    emerald: 'hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-200',
    amber: 'hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-200'
  };

  const textClasses = {
    indigo: 'text-gray-500',
    emerald: 'text-gray-500',
    amber: 'text-gray-500'
  };

  if (totalPages <= 1 && totalElements <= 10) return null;

  return (
    <div className="flex flex-col sm:flex-row justify-between items-center gap-6 py-2">
      <div className="flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2">
          <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{t('common.pagination.rowsPerPage', 'Rows per page:')}</label>
          <select
            value={size}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
            className="bg-white border border-gray-200 text-gray-700 text-xs font-black rounded-lg focus:ring-2 focus:ring-indigo-500 p-1.5 outline-none transition-all shadow-sm cursor-pointer"
          >
            {[5, 10, 25, 50, 100].map((pageSize) => (
              <option key={pageSize} value={pageSize}>
                {pageSize}
              </option>
            ))}
          </select>
        </div>
        <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest bg-white px-3 py-1.5 rounded-lg border border-gray-100 shadow-sm">
          {t('common.pagination.showing', 'Showing')} <span className="text-gray-900 font-black mx-0.5">{page * size + 1}</span> 
          {t('common.pagination.to', 'to')} <span className="text-gray-900 font-black mx-0.5">{Math.min((page + 1) * size, totalElements)}</span> 
          {t('common.pagination.of', 'of')} <span className="text-gray-900 font-black mx-0.5">{totalElements}</span>
        </span>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={() => onPageChange(Math.max(0, page - 1))}
          disabled={page === 0}
          className="p-2.5 rounded-xl border border-gray-200 text-gray-400 hover:bg-white hover:text-indigo-600 hover:border-indigo-100 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-sm active:scale-95"
        >
          <FaChevronLeft size={12} />
        </button>

        <div className="flex gap-1.5">
          {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
            let pageNum = i;
            if (totalPages > 5 && page > 2) {
              pageNum = Math.min(page - 2 + i, totalPages - 1);
            }
            
            const isActive = page === pageNum;
            return (
              <button
                key={pageNum}
                onClick={() => onPageChange(pageNum)}
                className={`w-10 h-10 rounded-xl border text-xs font-black transition-all transform active:scale-95
                  ${isActive 
                    ? themeClasses[theme] + ' shadow-lg border-transparent' 
                    : `bg-white border-gray-200 ${textClasses[theme]} ${hoverClasses[theme]} shadow-sm`}`}
              >
                {pageNum + 1}
              </button>
            );
          })}
        </div>

        <button
          onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
          disabled={page >= totalPages - 1}
          className="p-2.5 rounded-xl border border-gray-200 text-gray-400 hover:bg-white hover:text-indigo-600 hover:border-indigo-100 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-sm active:scale-95"
        >
          <FaChevronRight size={12} />
        </button>
      </div>
    </div>
  );
};

export default Pagination;
