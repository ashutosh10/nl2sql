import React, { useEffect, useState } from 'react'
import { getRegistry, putRegistry, type Registry } from '../lib/api'

export default function Designer() {
  const [reg, setReg] = useState<Registry | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showEntities, setShowEntities] = useState(true)
  const [showMetrics, setShowMetrics] = useState(true)
  const [entityExpanded, setEntityExpanded] = useState<boolean[]>([])
  const [metricExpanded, setMetricExpanded] = useState<boolean[]>([])

  useEffect(() => {
    getRegistry().then(r => {
      setReg(r)
      setEntityExpanded(Array(r.entities.length).fill(true))
      setMetricExpanded(Array(r.metrics.length).fill(true))
    }).catch(e => setError(String(e))).finally(() => setLoading(false))
  }, [])

  function addEntity() {
    if (!reg) return
    setReg({ ...reg, entities: [...reg.entities, { name: 'entity', table: 'table', pk: 'id', fields: [] }] })
    setEntityExpanded(prev => [...prev, true])
  }
  function addMetric() {
    if (!reg) return
    setReg({ ...reg, metrics: [...reg.metrics, { name: 'metric', entity: '', expression: 'COUNT(*)', timeField: undefined, dimensions: [] }] })
    setMetricExpanded(prev => [...prev, true])
  }
  function addField(ei: number) {
    if (!reg) return
    const copy = structuredClone(reg)
    copy.entities[ei].fields.push({ name: 'field', type: 'text', description: '' } as any)
    setReg(copy)
  }
  function save() {
    if (!reg) return
    putRegistry(reg).catch(e => setError(String(e)))
  }

  if (loading) return <div>Loading…</div>
  if (error) return <div className="text-red-600">{error}</div>
  if (!reg) return <div>No registry</div>

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Semantic Designer</h2>
          <p className="text-gray-500">Define entities, fields, and metrics powering NL2SQL.</p>
        </div>
        <button className="px-4 py-2 bg-blue-600 text-white rounded shadow hover:bg-blue-700" onClick={save}>Save</button>
      </div>

      <div className="space-y-6">
        {/* Entities Section */}
        <section className="bg-white border rounded-lg shadow-sm">
          <header className="flex items-center justify-between px-4 py-3 border-b">
            <div>
              <h3 className="text-lg font-medium">Entities</h3>
              <p className="text-sm text-gray-500">Define tables, primary keys, fields, and descriptions to guide the LLM.</p>
            </div>
            <div className="flex items-center gap-2">
              <button className="px-3 py-1.5 border rounded hover:bg-gray-50" onClick={addEntity}>+ Entity</button>
              <button aria-label={showEntities ? 'Collapse' : 'Expand'}
                className="p-1.5 rounded hover:bg-gray-50"
                onClick={() => setShowEntities(v => !v)}>
                <svg className={`w-5 h-5 transition-transform ${showEntities ? '' : '-rotate-90'}`} viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 10.94l3.71-3.71a.75.75 0 111.06 1.06l-4.24 4.24a.75.75 0 01-1.06 0L5.21 8.29a.75.75 0 01.02-1.08z" clipRule="evenodd" />
                </svg>
              </button>
            </div>
          </header>
          {showEntities && (
            <div className="p-4 space-y-4">
              {reg.entities.map((e, i) => (
                <div key={i} className="border rounded-lg">
                  <div className="flex items-center justify-between p-3 border-b">
                    <div className="flex items-center gap-2">
                      <button aria-label={entityExpanded[i] ? 'Collapse entity' : 'Expand entity'}
                        className="p-1.5 rounded hover:bg-gray-50"
                        onClick={() => setEntityExpanded(prev => prev.map((v, idx) => idx === i ? !v : v))}>
                        <svg className={`w-5 h-5 transition-transform ${entityExpanded[i] ? '' : '-rotate-90'}`} viewBox="0 0 20 20" fill="currentColor">
                          <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 10.94l3.71-3.71a.75.75 0 111.06 1.06l-4.24 4.24a.75.75 0 01-1.06 0L5.21 8.29a.75.75 0 01.02-1.08z" clipRule="evenodd" />
                        </svg>
                      </button>
                      <div className="font-medium">{e.name || 'entity'}</div>
                    </div>
                    <div className="text-xs text-gray-500 truncate max-w-[50%]">{(e as any).description || ''}</div>
                  </div>
                  {entityExpanded[i] && (
                    <div className="p-4 space-y-3">
                      <div className="grid grid-cols-3 gap-2">
                        <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={e.name} onChange={ev => { const c = structuredClone(reg); c.entities[i].name = ev.target.value; setReg(c) }} placeholder="Entity name" />
                        <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={e.table} onChange={ev => { const c = structuredClone(reg); c.entities[i].table = ev.target.value; setReg(c) }} placeholder="Backing table" />
                        <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={e.pk || ''} onChange={ev => { const c = structuredClone(reg); c.entities[i].pk = ev.target.value; setReg(c) }} placeholder="Primary key" />
                      </div>
                      <textarea className="w-full border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" rows={2}
                        placeholder="Entity description (optional)"
                        value={(e as any).description || ''}
                        onChange={ev => { const c = structuredClone(reg); (c.entities[i] as any).description = ev.target.value; setReg(c) }} />
                      <div>
                        <div className="flex items-center justify-between">
                          <div className="font-medium">Fields</div>
                          <button className="px-3 py-1.5 border rounded hover:bg-gray-50" onClick={() => addField(i)}>+ Field</button>
                        </div>
                        <div className="space-y-2 mt-2">
                          {e.fields.map((f, j) => (
                            <div key={j} className="grid grid-cols-3 gap-2">
                              <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={f.name} onChange={ev => { const c = structuredClone(reg); c.entities[i].fields[j].name = ev.target.value; setReg(c) }} placeholder="Field name" />
                              <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={f.type} onChange={ev => { const c = structuredClone(reg); c.entities[i].fields[j].type = ev.target.value; setReg(c) }} placeholder="Type (e.g., text, int)" />
                              <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={(f as any).description || ''} onChange={ev => { const c = structuredClone(reg); (c.entities[i].fields[j] as any).description = ev.target.value; setReg(c) }} placeholder="Description (optional)" />
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Metrics Section */}
        <section className="bg-white border rounded-lg shadow-sm">
          <header className="flex items-center justify-between px-4 py-3 border-b">
            <div>
              <h3 className="text-lg font-medium">Metrics</h3>
              <p className="text-sm text-gray-500">Define metric expressions, entities, dimensions, and relations.</p>
            </div>
            <div className="flex items-center gap-2">
              <button className="px-3 py-1.5 border rounded hover:bg-gray-50" onClick={addMetric}>+ Metric</button>
              <button aria-label={showMetrics ? 'Collapse' : 'Expand'}
                className="p-1.5 rounded hover:bg-gray-50"
                onClick={() => setShowMetrics(v => !v)}>
                <svg className={`w-5 h-5 transition-transform ${showMetrics ? '' : '-rotate-90'}`} viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 10.94l3.71-3.71a.75.75 0 111.06 1.06l-4.24 4.24a.75.75 0 01-1.06 0L5.21 8.29a.75.75 0 01.02-1.08z" clipRule="evenodd" />
                </svg>
              </button>
            </div>
          </header>
          {showMetrics && (
            <div className="p-4 space-y-4">
              {reg.metrics.map((m, i) => (
                <div key={i} className="border rounded-lg">
                  <div className="flex items-center justify-between p-3 border-b">
                    <div className="flex items-center gap-2">
                      <button aria-label={metricExpanded[i] ? 'Collapse metric' : 'Expand metric'}
                        className="p-1.5 rounded hover:bg-gray-50"
                        onClick={() => setMetricExpanded(prev => prev.map((v, idx) => idx === i ? !v : v))}>
                        <svg className={`w-5 h-5 transition-transform ${metricExpanded[i] ? '' : '-rotate-90'}`} viewBox="0 0 20 20" fill="currentColor">
                          <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 10.94l3.71-3.71a.75.75 0 111.06 1.06l-4.24 4.24a.75.75 0 01-1.06 0L5.21 8.29a.75.75 0 01.02-1.08z" clipRule="evenodd" />
                        </svg>
                      </button>
                      <div className="font-medium">{m.name || 'metric'}</div>
                    </div>
                    <div className="text-xs text-gray-500 truncate max-w-[50%]">{(m.entities || []).join(', ') || m.entity || ''}</div>
                  </div>
                  {metricExpanded[i] && (
                    <div className="p-4 space-y-3">
                      <div className="grid grid-cols-2 gap-2">
                        <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={m.name} onChange={ev => { const c = structuredClone(reg); c.metrics[i].name = ev.target.value; setReg(c) }} placeholder="Metric name" />
                        <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={m.entity || ''} onChange={ev => { const c = structuredClone(reg); c.metrics[i].entity = ev.target.value; setReg(c) }} placeholder="Legacy single entity (optional)" />
                        <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={(m.entities || []).join(', ')} onChange={ev => { const c = structuredClone(reg); c.metrics[i].entities = ev.target.value.split(',').map(s => s.trim()).filter(Boolean); setReg(c) }} placeholder="Entities (comma-separated)" />
                        <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={m.timeField || ''} onChange={ev => { const c = structuredClone(reg); c.metrics[i].timeField = ev.target.value; setReg(c) }} placeholder="Time field (optional)" />
                        <input className="border p-2 rounded col-span-2 focus:outline-none focus:ring-2 focus:ring-blue-500" value={m.expression} onChange={ev => { const c = structuredClone(reg); c.metrics[i].expression = ev.target.value; setReg(c) }} placeholder="Expression (e.g., COUNT(*))" />
                        <input className="border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" value={m.dimensions.join(', ')} onChange={ev => { const c = structuredClone(reg); c.metrics[i].dimensions = ev.target.value.split(',').map(s => s.trim()).filter(Boolean); setReg(c) }} placeholder="Dimensions (comma-separated)" />
                      </div>
                      <div>
                        <label className="text-sm text-gray-600">Relations (one per line, e.g., deployments - repositories on deployments.repo_id = repositories.id)</label>
                        <textarea className="mt-1 w-full border p-2 rounded focus:outline-none focus:ring-2 focus:ring-blue-500" rows={3}
                          value={(m.relations || []).join('\n')}
                          onChange={ev => { const c = structuredClone(reg); c.metrics[i].relations = ev.target.value.split('\n').map(s => s.trim()).filter(Boolean); setReg(c) }}
                          placeholder="entityA -> entityB on a.id = b.a_id" />
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      <section>
        <h3 className="text-lg font-medium mb-2">JSON Preview</h3>
        <pre className="bg-white border rounded-lg p-4 overflow-auto text-sm shadow-sm">{JSON.stringify(reg, null, 2)}</pre>
      </section>
    </div>
  )
}
