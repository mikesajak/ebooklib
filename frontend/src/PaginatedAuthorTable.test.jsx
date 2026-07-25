import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter } from 'react-router-dom';
import { I18nextProvider } from 'react-i18next';
import i18n from './i18n';
import PaginatedAuthorTable from './PaginatedAuthorTable';
import { SearchProvider } from './SearchContext';
import { AuthProvider } from './AuthContext';

describe('PaginatedAuthorTable', () => {
  const mockAuthorsPage1 = {
    content: [
      { id: '1', firstName: 'John', lastName: 'Doe' },
      { id: '2', firstName: 'Jane', lastName: 'Smith' },
    ],
    page: {
      totalPages: 2,
      totalElements: 3,
      number: 0,
      size: 2,
    },
  };

  const mockAuthorsPage2 = {
    content: [
      { id: '3', firstName: 'Peter', lastName: 'Jones' },
    ],
    page: {
      totalPages: 2,
      totalElements: 3,
      number: 1,
      size: 2,
    },
  };

  beforeEach(() => {
    global.fetch = vi.fn((url) => {
      const urlStr = typeof url === 'string' ? url : url.url;

      if (urlStr.includes('/api/me')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ username: 'test-user', roles: ['ROLE_USER'] }),
        });
      }

      // Ensure we have an absolute URL for the URL constructor
      const absoluteUrl = urlStr.startsWith('http') ? urlStr : `http://localhost${urlStr.startsWith('/') ? '' : '/'}${urlStr}`;
      const urlObj = new URL(absoluteUrl);
      const page = urlObj.searchParams.get('page');

      if (urlStr.includes('/api/authors/search')) {
        if (page === '1') {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockAuthorsPage2),
          });
        }
        // Default to page 0
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockAuthorsPage1),
        });
      }
      return Promise.reject(new Error(`Unknown URL: ${urlStr}`));
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderPaginatedAuthorTable = () => {
    render(
      <I18nextProvider i18n={i18n}>
        <AuthProvider>
          <SearchProvider>
            <BrowserRouter>
              <PaginatedAuthorTable />
            </BrowserRouter>
          </SearchProvider>
        </AuthProvider>
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
    expect(screen.getByText(/Showing/)).toBeInTheDocument();
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
    expect(screen.getByText(/Showing/)).toBeInTheDocument();
  });

  it('navigates to the previous page', async () => {
    renderPaginatedAuthorTable();
    await waitFor(() => expect(screen.getByText(/John/i)).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /Next/i }));
    await waitFor(() => expect(screen.getByText(/Peter/i)).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /Previous/i }));
    await waitFor(() => expect(screen.getByText(/John/i)).toBeInTheDocument());
    expect(screen.getByText(/Showing/)).toBeInTheDocument();
  });

  it('changes page size', async () => {
    // Mock for page size change, assuming the API would return 3 items on page 0 if size=5
    global.fetch.mockImplementation((url) => {
      const urlStr = typeof url === 'string' ? url : url.url;
      if (urlStr.includes('/api/me')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ username: 'test-user', roles: ['ROLE_USER'] }),
        });
      }
      if (urlStr.includes('/api/authors/search') && urlStr.includes('size=5')) {
                  return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                      content: [
                        { id: '1', firstName: 'John', lastName: 'Doe' },
                        { id: '2', firstName: 'Jane', lastName: 'Smith' },
                        { id: '3', firstName: 'Peter', lastName: 'Jones' },
                      ],
                      page: {
                        totalPages: 1,
                        totalElements: 3,
                        number: 0,
                        size: 5,
                      },
                    }),
                  });      }
      // Default mock for initial render
      if (url.includes('/api/authors/search') && url.includes('page=0')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockAuthorsPage1),
        });
      }
      return Promise.reject(new Error(`Unknown URL in changes page size test: ${url}`));
    });

    renderPaginatedAuthorTable();
    await waitFor(() => expect(screen.getByText(/John/i)).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/Page Size/i), { target: { value: '5' } });

    await waitFor(() => {
      expect(screen.getByText(/Peter/i)).toBeInTheDocument(); // All authors should be on one page
    });
    expect(screen.queryByText(/Showing/)).not.toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('/api/authors/search'), expect.anything());
    expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('size=5'), expect.anything());
  });
});
