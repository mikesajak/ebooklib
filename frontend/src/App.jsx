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
import AppSidebar from './Sidebar'

function App() {
  return (
    <BrowserRouter>
      <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
        <Header />
        <div style={{ display: 'flex', flexGrow: 1, overflow: 'hidden' }}>
          <AppSidebar />
          <main style={{ flexGrow: 1, overflowY: 'auto', padding: '1rem' }}>
            <Routes>
              <Route path="/" element={<BookTable />} />
              <Route path="/add-book" element={<AddBook />} />
              <Route path="/book/:id" element={<BookDetails />} />
              <Route path="/author/:id" element={<AuthorDetails />} />
              <Route path="/series/:id" element={<SeriesDetails />} />
              <Route path="/authors" element={<AuthorList />} />
              <Route path="/authors/add" element={<AddAuthor />} />
              <Route path="/series" element={<SeriesList />} />
            </Routes>
          </main>
        </div>
      </div>
    </BrowserRouter>
  )
}

export default App
