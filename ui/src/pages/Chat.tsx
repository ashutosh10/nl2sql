import React, { useState } from 'react'
import { chat as chatApi } from '../lib/api'

export default function Chat() {
  const [text, setText] = useState('What is the deployment frequency of terraform deployments for repo X in July 2025?')
  const [resp, setResp] = useState<any | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function send() {
    setLoading(true); setError(null)
    try {
      const r = await chatApi(text)
      setResp(r)
    } catch (e:any) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-4">
      <h2 className="text-2xl font-semibold">Chat</h2>
      <div className="flex gap-2">
        <input className="flex-1 border p-2" value={text} onChange={e => setText(e.target.value)} />
        <button className="px-4 py-2 bg-blue-600 text-white rounded" onClick={send} disabled={loading}>{loading ? 'Sending…' : 'Send'}</button>
      </div>
      {error && <div className="text-red-600">{error}</div>}
      {resp && (
        <div className="grid grid-cols-2 gap-4">
          <section className="bg-white border rounded p-3">
            <h3 className="font-medium mb-2">Intent</h3>
            <pre className="text-sm overflow-auto">{JSON.stringify(resp.intent, null, 2)}</pre>
          </section>
          <section className="bg-white border rounded p-3">
            <h3 className="font-medium mb-2">Logical Query</h3>
            <pre className="text-sm overflow-auto">{JSON.stringify(resp.logicalQuery, null, 2)}</pre>
          </section>
          <section className="bg-white border rounded p-3 col-span-2">
            <h3 className="font-medium mb-2">SQL</h3>
            <pre className="text-sm overflow-auto">{String(resp.sql)}</pre>
          </section>
          <section className="bg-white border rounded p-3 col-span-2">
            <h3 className="font-medium mb-2">Result</h3>
            <pre className="text-sm overflow-auto">{JSON.stringify(resp.result, null, 2)}</pre>
          </section>
        </div>
      )}
    </div>
  )
}
