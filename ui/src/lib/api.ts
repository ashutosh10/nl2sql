export type Registry = {
  entities: { name: string; table: string; pk?: string; description?: string; fields: { name: string; type: string; description?: string }[] }[]
  metrics: { name: string; entity?: string; entities?: string[]; expression: string; timeField?: string; dimensions: string[]; relations?: string[] }[]
}

export async function getRegistry(): Promise<Registry> {
  const res = await fetch('/semantic-registry')
  if (!res.ok) throw new Error('Failed to load registry')
  return await res.json()
}

export async function putRegistry(reg: Registry): Promise<void> {
  const res = await fetch('/semantic-registry', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(reg),
  })
  if (!res.ok && res.status !== 204) throw new Error('Failed to save registry')
}

export async function chat(text: string): Promise<any> {
  const res = await fetch('/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  })
  if (!res.ok) throw new Error('Chat failed')
  return await res.json()
}
