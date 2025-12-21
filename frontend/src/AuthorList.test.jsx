import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter, Link } from 'react-router-dom';
import { I18nextProvider } from 'react-i18next';
import i18n from './i18n';
import AuthorList from './AuthorList';
import { SearchProvider } from './SearchContext';

// Mock the PaginatedAuthorTable component
vi.mock('./PaginatedAuthorTable', () => ({
  __esModule: true,
  default: () => <div data-testid="paginated-author-table">Paginated Author Table</div>,
}));

// Mock the AuthorGroupTable component
vi.mock('./AuthorGroupTable', () => ({
  __esModule: true,
  default: ({ authors, openConfirmDialog }) => (
    <table data-testid="author-group-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Book Count</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {authors.map(author => (
          <tr key={author.id} data-testid={`author-row-${author.id}`}>
            <td>
              <Link to={`/author/${author.id}`}>{author.firstName} {author.lastName}</Link>
            </td>
            <td>({author.bookCount})</td>
            <td>
              <Link to={`/authors/${author.id}/edit`} data-testid={`edit-author-${author.id}`}>Edit</Link>
              <button onClick={() => openConfirmDialog(author)} data-testid={`delete-author-${author.id}`}>Delete</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  ),
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
        <SearchProvider>
          <BrowserRouter>
            <AuthorList />
          </BrowserRouter>
        </SearchProvider>
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

  it('displays author data correctly in grouped view with table', async () => {
    renderAuthorList('grouped');
    
    // The authors are grouped by lastName first letter by default: D (Doe), S (Smith), J (Jones)
    await waitFor(() => expect(screen.getByRole('heading', { level: 2, name: /^D/ })).toBeInTheDocument());
    
    // Expand groups
    fireEvent.click(screen.getByRole('heading', { level: 2, name: /^D/ }));
    fireEvent.click(screen.getByRole('heading', { level: 2, name: /^S/ }));

    await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());

    // Expect the table headers from AuthorGroupTable
    expect(screen.getAllByRole('columnheader', { name: 'Name' })[0]).toBeInTheDocument();
    expect(screen.getAllByRole('columnheader', { name: 'Book Count' })[0]).toBeInTheDocument();
    expect(screen.getAllByRole('columnheader', { name: 'Actions' })[0]).toBeInTheDocument();

    // Expect author data within the table
    expect(screen.getByRole('row', { name: /John Doe/i })).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /Jane Smith/i })).toBeInTheDocument();
  });

  it('handles edit action in grouped view', async () => {
    renderAuthorList('grouped');
    
    await waitFor(() => expect(screen.getByRole('heading', { level: 2, name: /^D/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('heading', { level: 2, name: /^D/ }));

    await waitFor(() => expect(screen.getByTestId('author-group-table')).toBeInTheDocument());

    // Test edit action (navigation)
    fireEvent.click(screen.getByTestId(`edit-author-${mockAuthors[0].id}`));
    expect(window.location.pathname).toBe(`/authors/${mockAuthors[0].id}/edit`);
  });

  it('handles delete action in grouped view', async () => {
    renderAuthorList('grouped');
    
    await waitFor(() => expect(screen.getByRole('heading', { level: 2, name: /^D/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('heading', { level: 2, name: /^D/ }));

    await waitFor(() => expect(screen.getByTestId('author-group-table')).toBeInTheDocument());

    // Test delete action: clicking delete button should open ConfirmationDialog
    fireEvent.click(screen.getByTestId(`delete-author-${mockAuthors[0].id}`));
    await waitFor(() => expect(screen.getByText(/Confirm Author Deletion/i)).toBeInTheDocument());
    expect(screen.getByText(`Are you sure you want to delete the author "${mockAuthors[0].firstName} ${mockAuthors[0].lastName}"? This will remove the author from all associated books.`)).toBeInTheDocument();
  });
});
