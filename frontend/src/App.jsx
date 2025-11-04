import './App.css'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import BookTable from './BookTable'
import BookDetails from './BookDetails'
import AuthorDetails from './AuthorDetails'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<BookTable />} />
        <Route path="/book/:id" element={<BookDetails />} />
        <Route path="/author/:id" element={<AuthorDetails />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
