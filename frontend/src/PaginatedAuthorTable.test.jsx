import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter } from 'react-router-dom';
import { I18nextProvider } from 'react-i18next';
import i18n from './i18n';
import PaginatedAuthorTable from './PaginatedAuthorTable';

describe('PaginatedAuthorTable', () => {
  const mockAuthorsPage1 = {
    content: [
      { id: '1', firstName: 'John', lastName: 'Doe' },
      { id: '2', firstName: 'Jane', lastName: 'Smith' },
    ],
    totalPages: 2,
    totalElements: 3,
    number: 0, // current page (0-indexed)
    size: 2,
  };

  const mockAuthorsPage2 = {
    content: [
      { id: '3', firstName: 'Peter', lastName: 'Jones' },
    ],
    totalPages: 2,
    totalElements: 3,
    number: 1, // current page (0-indexed)
    size: 2,
  };

  beforeEach(() => {
    global.fetch = vi.fn((url) => {
      if (url.includes('page=0')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockAuthorsPage1),
        });
      }
      if (url.includes('page=1')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockAuthorsPage2),
        });
      }
      return Promise.reject(new Error('Unknown URL'));
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderPaginatedAuthorTable = () => {
    render(
      <I18nextProvider i18n={i18n}>
        <BrowserRouter>
          <PaginatedAuthorTable />
        </BrowserRouter>
      </I18nextProvider>
    );
  };

  it('renders without crashing', async () => {
    renderPaginatedAuthorTable();
    // Expect loading message first
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    // Then wait for the table to appear after data is fetched
    await waitFor(() => expect(screen.getByRole('table')).toBeInTheDocument());
  });

  it('displays loading message initially', () => {
    global.fetch.mockImplementationOnce(() => new Promise(() => {})); // Never resolve fetch
    renderPaginatedAuthorTable();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('displays author data after successful fetch', async () => {
    renderPaginatedAuthorTable();
    await waitFor(() => {
      expect(screen.getByText(/John/i)).toBeInTheDocument();
      expect(screen.getByText(/Doe/i)).toBeInTheDocument();
      expect(screen.getByText(/Jane/i)).toBeInTheDocument();
      expect(screen.getByText(/Smith/i)).toBeInTheDocument();
    });
    // Verify that the Pagination component is rendered by checking for an element it renders, e.g., the page info or buttons.
    expect(screen.getByText(/Page \d of \d \(\d+ total\)/)).toBeInTheDocument();
  });

  it('handles error state', async () => {
    global.fetch.mockImplementationOnce(() => Promise.resolve({ ok: false }));
    renderPaginatedAuthorTable();
    await waitFor(() => expect(screen.getByText(/Error:/)).toBeInTheDocument());
  });

  it('navigates to the next page', async () => {
    renderPaginatedAuthorTable();
    await waitFor(() => expect(screen.getByText(/John/i)).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /Next/i }));

    await waitFor(() => {
      expect(screen.queryByText(/John/i)).not.toBeInTheDocument();
      expect(screen.getByText(/Peter/i)).toBeInTheDocument();
      expect(screen.getByText(/Jones/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Page \d of \d \(\d+ total\)/)).toBeInTheDocument();
  });

  it('navigates to the previous page', async () => {
    renderPaginatedAuthorTable();
    await waitFor(() => expect(screen.getByText(/John/i)).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /Next/i }));
    await waitFor(() => expect(screen.getByText(/Peter/i)).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /Previous/i }));
    await waitFor(() => expect(screen.getByText(/John/i)).toBeInTheDocument());
    expect(screen.getByText(/Page \d of \d \(\d+ total\)/)).toBeInTheDocument();
  });

  it('changes page size', async () => {
    // Mock for page size change, assuming the API would return 3 items on page 0 if size=5
    global.fetch.mockImplementation((url) => {
      if (url.includes('size=5')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: '1', firstName: 'John', lastName: 'Doe' },
              { id: '2', firstName: 'Jane', lastName: 'Smith' },
              { id: '3', firstName: 'Peter', lastName: 'Jones' },
            ],
            totalPages: 1,
            totalElements: 3,
            number: 0,
            size: 5,
          }),
        });
      }
      // Default mock for initial render
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(mockAuthorsPage1),
      });
    });

    renderPaginatedAuthorTable();
    await waitFor(() => expect(screen.getByText(/John/i)).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/Page Size/i), { target: { value: '5' } });

    await waitFor(() => {
      expect(screen.getByText(/Peter/i)).toBeInTheDocument(); // All authors should be on one page
    });
    expect(screen.getByText(/Page \d of \d \(\d+ total\)/)).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith('/api/authors?page=0&size=5&sort=lastName,asc&sort=firstName,asc');
  });
});
