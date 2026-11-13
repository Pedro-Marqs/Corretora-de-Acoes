import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import './styles/tokens.css'
import './styles/components.css'
import './styles/global.css'
import './styles/navigation.css'
import './styles/investor-pages.css'
import './styles/admin-pages.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
