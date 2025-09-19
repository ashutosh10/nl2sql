import React from 'react'
import ReactDOM from 'react-dom/client'
import { createBrowserRouter, RouterProvider, Link, Outlet } from 'react-router-dom'
import './index.css'
import Designer from './pages/Designer'
import Chat from './pages/Chat'
import Monitor from './pages/Monitor'

function Layout() {
  return (
    <div className="min-h-screen flex">
      <aside className="w-64 bg-white border-r p-4 space-y-2">
        <h1 className="text-xl font-semibold mb-4">NL2SQL Studio</h1>
        <nav className="flex flex-col gap-2">
          <Link to="/designer" className="text-blue-600 hover:underline">Semantic Designer</Link>
          <Link to="/chat" className="text-blue-600 hover:underline">Chat</Link>
          <Link to="/monitor" className="text-blue-600 hover:underline">Monitor</Link>
        </nav>
      </aside>
      <main className="flex-1 p-6">
        <Outlet />
      </main>
    </div>
  )
}

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { path: '/', element: <Designer /> },
      { path: '/designer', element: <Designer /> },
      { path: '/chat', element: <Chat /> },
      { path: '/monitor', element: <Monitor /> },
    ],
  },
])

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
)
