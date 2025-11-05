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

function App() {
  return (
    <BrowserRouter>
      <Header />
      <Routes>
        <Route path="/" element={<BookTable />} />
        <Route path="/add-book" element={<AddBook />} />
        <Route path="/book/:id" element={<BookDetails />} />
        <Route path="/author/:id" element={<AuthorDetails />} />
        <Route path="/series/:id" element={<SeriesDetails />} />
        <Route path="/authors" element={<AuthorList />} />
        <Route path="/series" element={<SeriesList />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
