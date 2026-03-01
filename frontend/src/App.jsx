import './App.css'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Header from './Header'
import BookTable from './BookTable'
import BookDetails from './BookDetails'
import AuthorDetails from './AuthorDetails'
import SeriesDetails from './SeriesDetails'
import AuthorList from './AuthorList'
import SeriesList from './SeriesList'
import AddBook from './AddBook'
import AddAuthor from './AddAuthor'
import AddSeries from './AddSeries'
import AppSidebar from './Sidebar'
import LoginPage from './LoginPage'
import AdminDashboard from './AdminDashboard'
import UserManagement from './UserManagement'
import ProtectedRoute from './ProtectedRoute'
import { SearchProvider } from './SearchContext'
import { AuthProvider } from './AuthContext'

function App() {
  return (
    <AuthProvider>
      <SearchProvider>
        <BrowserRouter>
          <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <Header />
            <div style={{ display: 'flex', flexGrow: 1, overflow: 'hidden' }}>
              <AppSidebar />
              <main style={{ flexGrow: 1, overflowY: 'auto', padding: '1rem' }}>
                <Routes>
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/" element={<ProtectedRoute><BookTable /></ProtectedRoute>} />
                  <Route path="/books/add" element={<ProtectedRoute><AddBook /></ProtectedRoute>} />
                  <Route path="/book/:id" element={<ProtectedRoute><BookDetails /></ProtectedRoute>} />
                  <Route path="/books/:id/edit" element={<ProtectedRoute><AddBook /></ProtectedRoute>} />
                  <Route path="/author/:id" element={<ProtectedRoute><AuthorDetails /></ProtectedRoute>} />
                  <Route path="/authors" element={<ProtectedRoute><AuthorList /></ProtectedRoute>} />
                  <Route path="/authors/add" element={<ProtectedRoute><AddAuthor /></ProtectedRoute>} />
                  <Route path="/authors/:id/edit" element={<ProtectedRoute><AddAuthor /></ProtectedRoute>} />
                  <Route path="/series" element={<ProtectedRoute><SeriesList /></ProtectedRoute>} />
                  <Route path="/series/add" element={<ProtectedRoute><AddSeries /></ProtectedRoute>} />
                  <Route path="/series/:id" element={<ProtectedRoute><SeriesDetails /></ProtectedRoute>} />
                  <Route path="/admin" element={<ProtectedRoute><AdminDashboard /></ProtectedRoute>} />
                  <Route path="/admin/users" element={<ProtectedRoute><UserManagement /></ProtectedRoute>} />
                </Routes>
              </main>
            </div>
          </div>
        </BrowserRouter>
      </SearchProvider>
    </AuthProvider>
  )
}
export default App
