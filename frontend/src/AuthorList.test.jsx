import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter } from 'react-router-dom';
import { I18nextProvider } from 'react-i18next';
import i18n from './i18n';
import AuthorList from './AuthorList';

// Mock the PaginatedAuthorTable component
vi.mock('./PaginatedAuthorTable', () => ({
  __esModule: true,
  default: () => <div data-testid="paginated-author-table">Paginated Author Table</div>,
}));

describe('AuthorList', () => {
  const mockAuthors = [
    { id: '1', firstName: 'John', lastName: 'Doe', nationality: 'American' },
    { id: '2', firstName: 'Jane', lastName: 'Smith', nationality: 'British' },
    { id: '3', firstName: 'Peter', lastName: 'Jones', nationality: 'American' },
  ];

  beforeEach(() => {
    // Mock fetch for the AuthorList component's initial data fetch
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: mockAuthors, totalPages: 1, totalElements: mockAuthors.length }),
      })
    );
    // Clear localStorage before each test to ensure a clean state
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderAuthorList = (initialViewMode = 'grouped') => {
    // Set initial view mode in localStorage for the component to pick up
    localStorage.setItem('authorListViewMode', initialViewMode);
    render(
      <I18nextProvider i18n={i18n}>
        <BrowserRouter>
          <AuthorList />
        </BrowserRouter>
      </I18nextProvider>
    );
  };

  it('renders without crashing', async () => {
    renderAuthorList();
    await waitFor(() => expect(screen.getByText('Authors')).toBeInTheDocument());
  });

  it('displays loading message initially', () => {
    global.fetch.mockImplementationOnce(() => new Promise(() => {})); // Never resolve fetch
    renderAuthorList();
    expect(screen.getByText('Loading authors...')).toBeInTheDocument();
  });

  it('displays error message on fetch failure', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: false,
        status: 500,
      })
    );
    renderAuthorList();
    await waitFor(() => expect(screen.getByText(/Error:/)).toBeInTheDocument());
  });

  it('shows grouped view by default and allows switching to plain view', async () => {
    renderAuthorList('grouped');
    await waitFor(() => expect(screen.getByTestId('grouping-criteria-select')).toBeInTheDocument());
    expect(screen.queryByTestId('paginated-author-table')).not.toBeInTheDocument();

    fireEvent.click(screen.getByText('Plain List'));
    expect(localStorage.getItem('authorListViewMode')).toBe('plain');
    await waitFor(() => expect(screen.getByTestId('paginated-author-table')).toBeInTheDocument());
    expect(screen.queryByTestId('grouping-criteria-select')).not.toBeInTheDocument();
  });

  it('shows plain view if stored in localStorage and allows switching to grouped view', async () => {
    renderAuthorList('plain');
    await waitFor(() => expect(screen.getByTestId('paginated-author-table')).toBeInTheDocument());
    expect(screen.queryByTestId('grouping-criteria-select')).not.toBeInTheDocument();

    fireEvent.click(screen.getByText('Grouped View'));
    expect(localStorage.getItem('authorListViewMode')).toBe('grouped');
    await waitFor(() => expect(screen.getByTestId('grouping-criteria-select')).toBeInTheDocument());
    expect(screen.queryByTestId('paginated-author-table')).not.toBeInTheDocument();
  });

  it('persists viewMode to localStorage', async () => {
    renderAuthorList();
    await waitFor(() => expect(screen.getByText('Grouped View')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Plain List'));
    expect(localStorage.getItem('authorListViewMode')).toBe('plain');

    // Re-render the component to check if the state is loaded from localStorage
    renderAuthorList();
    await waitFor(() => expect(screen.getByTestId('paginated-author-table')).toBeInTheDocument());
  });
});
