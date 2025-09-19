import React, { useEffect, useRef, useState } from 'react'

type A2AEvent = {
  id: string; traceId: string; from: string; to: string; type: string;
  phase: 'request_sent' | 'response_received' | 'error'; timestampMs: number;
  payloadSummary?: string; error?: string; durationMs?: number;
}

export default function Monitor() {
  const [events, setEvents] = useState<A2AEvent[]>([])
  const [filter, setFilter] = useState('')
  const esRef = useRef<EventSource | null>(null)

  useEffect(() => {
    const es = new EventSource('/events')
    es.onmessage = (e) => {
      try {
        const ev = JSON.parse(e.data) as A2AEvent
        setEvents(prev => [ev, ...prev].slice(0, 500))
      } catch {}
    }
    es.onerror = () => { /* ignore */ }
    esRef.current = es
    return () => { es.close() }
  }, [])

  const filtered = events.filter(e => {
    const s = (e.from + ' ' + e.to + ' ' + e.phase + ' ' + (e.payloadSummary||'')).toLowerCase()
    return s.includes(filter.toLowerCase())
  })

  return (
    <div className="space-y-4">
      <h2 className="text-2xl font-semibold">Monitor</h2>
      <input className="border p-2 w-full" placeholder="Filter…" value={filter} onChange={e => setFilter(e.target.value)} />
      <div className="bg-white border rounded divide-y max-h-[70vh] overflow-auto">
        {filtered.map((e, i) => (
          <div key={i} className="p-3 text-sm">
            <div className="flex items-center justify-between">
              <div>
                <span className="font-medium">{e.from}</span>
                <span className="mx-2">→</span>
                <span className="font-medium">{e.to}</span>
                <span className="ml-2 text-gray-500">{e.phase}</span>
              </div>
              <div className="text-gray-500">{new Date(e.timestampMs).toLocaleTimeString()} {e.durationMs ? `(${e.durationMs}ms)` : ''}</div>
            </div>
            {e.error ? (
              <div className="text-red-600 mt-1">{e.error}</div>
            ) : (
              <div className="text-gray-700 mt-1">{e.payloadSummary}</div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
