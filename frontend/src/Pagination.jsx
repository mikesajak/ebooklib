import React from 'react';
import { useTranslation } from 'react-i18next';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';

const Pagination = ({ page, size, totalPages, totalElements, onPageChange, onPageSizeChange, theme = 'indigo' }) => {
  const { t } = useTranslation();

  const themeClasses = {
    indigo: 'bg-indigo-600 border-indigo-600 text-white',
    emerald: 'bg-emerald-600 border-emerald-600 text-white',
    amber: 'bg-amber-600 border-amber-600 text-white'
  };

  const hoverClasses = {
    indigo: 'hover:bg-indigo-50',
    emerald: 'hover:bg-emerald-50',
    amber: 'hover:bg-amber-50'
  };

  const textClasses = {
    indigo: 'text-indigo-600',
    emerald: 'text-emerald-600',
    amber: 'text-amber-600'
  };

  if (totalPages <= 1 && totalElements <= 10) return null;

  return (
    <div className="flex flex-col sm:flex-row justify-between items-center gap-4 py-2">
      <div className="flex items-center gap-3">
        <label className="text-xs font-bold text-gray-400 uppercase tracking-tighter">Rows per page:</label>
        <select
          value={size}
          onChange={(e) => onPageSizeChange(Number(e.target.value))}
          className="bg-white border border-gray-200 text-gray-700 text-xs font-bold rounded-lg focus:ring-2 focus:ring-offset-1 focus:ring-gray-300 p-1.5 outline-none transition-all"
        >
          {[5, 10, 25, 50, 100].map((pageSize) => (
            <option key={pageSize} value={pageSize}>
              {pageSize}
            </option>
          ))}
        </select>
        <span className="text-xs text-gray-400 font-medium">
          Showing <span className="text-gray-700 font-bold">{page * size + 1}</span> to <span className="text-gray-700 font-bold">{Math.min((page + 1) * size, totalElements)}</span> of <span className="text-gray-700 font-bold">{totalElements}</span>
        </span>
      </div>

      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(Math.max(0, page - 1))}
          disabled={page === 0}
          className="p-2 rounded-lg border border-gray-200 text-gray-400 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        >
          <FaChevronLeft className="text-xs" />
        </button>

        <div className="flex gap-1">
          {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
            // Simple logic for windowed pagination
            let pageNum = i;
            if (totalPages > 5 && page > 2) {
              pageNum = Math.min(page - 2 + i, totalPages - 1);
            }
            
            const isActive = page === pageNum;
            return (
              <button
                key={pageNum}
                onClick={() => onPageChange(pageNum)}
                className={`w-8 h-8 rounded-lg border text-xs font-bold transition-all
                  ${isActive 
                    ? themeClasses[theme] + ' shadow-sm' 
                    : `bg-white border-gray-200 text-gray-500 ${hoverClasses[theme]} ${textClasses[theme]} hover:border-transparent`}`}
              >
                {pageNum + 1}
              </button>
            );
          })}
        </div>

        <button
          onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
          disabled={page >= totalPages - 1}
          className="p-2 rounded-lg border border-gray-200 text-gray-400 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        >
          <FaChevronRight className="text-xs" />
        </button>
      </div>
    </div>
  );
};

export default Pagination;
