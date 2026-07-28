import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter } from 'react-router-dom';
import { I18nextProvider } from 'react-i18next';
import i18n from './i18n';
import UserProfile from './UserProfile';
import { AuthContext } from './AuthContext';

describe('UserProfile', () => {
  const mockAuthContext = {
    user: { username: 'testuser', roles: ['ROLE_USER'] },
    isAuthenticated: true,
    isAdmin: false,
    loading: false,
    login: vi.fn(),
    logout: vi.fn(),
    checkAuth: vi.fn(),
  };

  const renderComponent = (contextValue = mockAuthContext) => {
    return render(
      <I18nextProvider i18n={i18n}>
        <AuthContext.Provider value={contextValue}>
          <BrowserRouter>
            <UserProfile />
          </BrowserRouter>
        </AuthContext.Provider>
      </I18nextProvider>
    );
  };

  test('renders user profile details and password form', () => {
    renderComponent();

    expect(screen.getByText('testuser')).toBeInTheDocument();
    expect(screen.getByLabelText(/Current Password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^New Password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Confirm New Password/i)).toBeInTheDocument();
  });

  test('shows error when passwords do not match', async () => {
    renderComponent();

    fireEvent.change(screen.getByLabelText(/Current Password/i), { target: { value: 'oldPass' } });
    fireEvent.change(screen.getByLabelText(/^New Password/i), { target: { value: 'newPass123' } });
    fireEvent.change(screen.getByLabelText(/Confirm New Password/i), { target: { value: 'differentPass' } });

    fireEvent.click(screen.getByRole('button', { name: /Update Password/i }));

    await waitFor(() => {
      expect(screen.getByText(/New passwords do not match/i)).toBeInTheDocument();
    });
  });
});
