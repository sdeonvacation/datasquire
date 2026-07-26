import type { AgentInfo, Message, SSEEvent } from './types';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export async function* streamQuery(
  query: string,
  sessionId?: string,
  signal?: AbortSignal,
): AsyncGenerator<SSEEvent> {
  const response = await fetch(`${BASE_URL}/api/query`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, sessionId }),
    signal,
  });

  if (!response.ok) {
    throw new Error(`Query failed: ${response.status} ${response.statusText}`);
  }

  const reader = response.body?.getReader();
  if (!reader) throw new Error('No response body');

  const decoder = new TextDecoder();
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';

      let eventType = '';
      let data = '';

      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
          data = line.slice(5).trim();
        } else if (line === '' && eventType && data) {
          yield { type: eventType, payload: JSON.parse(data) } as SSEEvent;
          eventType = '';
          data = '';
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}

export async function getSession(id: string): Promise<{ messages: Message[] }> {
  const res = await fetch(`${BASE_URL}/api/sessions/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch session: ${res.status}`);
  const data = await res.json();
  data.messages = data.messages?.map((m: any) => ({
    ...m,
    timestamp: new Date(m.timestamp),
  }));
  return data;
}

export async function deleteSession(id: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/sessions/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`Failed to delete session: ${res.status}`);
}

export async function getAgents(): Promise<AgentInfo[]> {
  const res = await fetch(`${BASE_URL}/api/agents`);
  if (!res.ok) throw new Error(`Failed to fetch agents: ${res.status}`);
  return res.json();
}
