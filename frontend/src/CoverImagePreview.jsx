import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { FaTimes, FaSearchPlus } from 'react-icons/fa';

/**
 * CoverImagePreview component
 * Renders a thumbnail image for a book cover that, when clicked, opens a temporary
 * Lightbox modal displaying the cover at an expanded size.
 *
 * Quality protection:
 * Uses natural dimensions (naturalWidth / naturalHeight) of the image to ensure low-res
 * images are never upscaled beyond 100% of their original pixel resolution. High-res covers
 * scale down appropriately within viewport boundaries (max 85vh / 90vw).
 *
 * Extensibility:
 * The `mode` prop defaults to 'modal' (Option 1). In the future, supporting 'popover' (Option 2)
 * can be implemented inside this component without requiring changes to consuming code.
 */
const CoverImagePreview = ({
  src,
  alt = 'Book Cover',
  title = '',
  className = 'w-full h-full object-cover',
  containerClassName = '',
  mode = 'modal',
  showExpandBadge = true,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [naturalWidth, setNaturalWidth] = useState(null);
  const [naturalHeight, setNaturalHeight] = useState(null);

  const handleImageLoad = (e) => {
    if (e.target.naturalWidth && e.target.naturalHeight) {
      setNaturalWidth(e.target.naturalWidth);
      setNaturalHeight(e.target.naturalHeight);
    }
  };

  const handleOpen = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (src) {
      setIsOpen(true);
    }
  };

  const handleClose = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    setIsOpen(false);
  };

  // Close on Escape key press
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        handleClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    // Lock body scroll when modal is open
    document.body.style.overflow = 'hidden';

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!src) return null;

  const modalContent = isOpen && mode === 'modal' && (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200"
      onClick={handleClose}
      role="dialog"
      aria-modal="true"
      aria-label={title ? `Cover preview for ${title}` : 'Cover preview'}
    >
      {/* Main Modal Card */}
      <div 
        className="relative flex flex-col items-center max-w-[90vw] max-h-[90vh] bg-gray-900/90 p-4 rounded-2xl shadow-2xl border border-gray-700/50"
        onClick={(e) => e.stopPropagation()} // Prevent backdrop click from closing when clicking modal content
      >
        {/* Close Button */}
        <button
          onClick={handleClose}
          className="absolute -top-3 -right-3 p-2.5 bg-gray-800 hover:bg-gray-700 text-white rounded-full shadow-lg border border-gray-600 transition-transform active:scale-95 z-10"
          aria-label="Close cover preview"
        >
          <FaTimes size={16} />
        </button>

        {/* Image Container with Natural Dimensions Lock */}
        <div className="relative flex items-center justify-center overflow-hidden rounded-xl">
          <img
            src={src}
            alt={alt}
            className="w-auto h-auto max-w-[90vw] max-h-[80vh] object-contain rounded-lg shadow-md"
            style={{
              // Protect against upscaling beyond natural size if natural dimensions are loaded
              maxWidth: naturalWidth ? `min(90vw, ${naturalWidth}px)` : '90vw',
              maxHeight: naturalHeight ? `min(80vh, ${naturalHeight}px)` : '80vh',
            }}
          />
        </div>

        {/* Optional Title & Info Bar */}
        {title && (
          <div className="mt-3 text-center px-4">
            <p className="text-sm font-semibold text-gray-200 truncate max-w-[70vw]">
              {title}
            </p>
            {naturalWidth && naturalHeight && (
              <p className="text-[11px] text-gray-400 mt-0.5">
                {naturalWidth} × {naturalHeight} px
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );

  return (
    <>
      {/* Thumbnail Trigger */}
      <div 
        className={`relative group cursor-pointer overflow-hidden ${containerClassName}`}
        onClick={handleOpen}
        title={title ? `${title} (Click to enlarge)` : 'Click to enlarge'}
      >
        <img 
          src={src} 
          alt={alt} 
          onLoad={handleImageLoad}
          className={className} 
        />
        {showExpandBadge && (
          <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white">
            <FaSearchPlus size={16} className="drop-shadow" />
          </div>
        )}
      </div>

      {modalContent && createPortal(modalContent, document.body)}
    </>
  );
};

export default CoverImagePreview;
