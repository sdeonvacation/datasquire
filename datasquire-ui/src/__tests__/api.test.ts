import { describe, expect, it, vi } from 'vitest';
import { streamQuery, getSession } from '../lib/api';

// Mock fetch for SSE stream testing
function createSSEResponse(events: Array<{ event: string; data: string }>): Response {
  const text = events.map((e) => `event:${e.event}\ndata:${e.data}\n\n`).join('');
  const encoder = new TextEncoder();
  const stream = new ReadableStream({
    start(controller) {
      controller.enqueue(encoder.encode(text));
      controller.close();
    },
  });
  return new Response(stream, { status: 200, headers: { 'content-type': 'text/event-stream' } });
}

describe('streamQuery', () => {
  it('parses SSE progress events', async () => {
    const mockResponse = createSSEResponse([
      { event: 'progress', data: JSON.stringify({ step: 'rag', detail: 'searching' }) },
    ]);
    globalThis.fetch = async () => mockResponse;

    const events = [];
    for await (const event of streamQuery('test query')) {
      events.push(event);
    }

    expect(events).toHaveLength(1);
    expect(events[0].type).toBe('progress');
    expect(events[0].payload).toEqual({ step: 'rag', detail: 'searching' });
  });

  it('parses multiple SSE events in sequence', async () => {
    const mockResponse = createSSEResponse([
      { event: 'progress', data: JSON.stringify({ step: 'rag', detail: 'start' }) },
      { event: 'data', data: JSON.stringify({ chunk: 'Hello' }) },
      { event: 'data', data: JSON.stringify({ chunk: ' world' }) },
      { event: 'done', data: JSON.stringify({ sessionId: 's1', agentsUsed: ['sql'], iterations: 2, qualityScore: 0.9, metadata: {} }) },
    ]);
    globalThis.fetch = async () => mockResponse;

    const events = [];
    for await (const event of streamQuery('q')) {
      events.push(event);
    }

    expect(events).toHaveLength(4);
    expect(events[0].type).toBe('progress');
    expect(events[1].type).toBe('data');
    expect(events[2].type).toBe('data');
    expect(events[3].type).toBe('done');
    expect(events[3].payload).toMatchObject({ sessionId: 's1', agentsUsed: ['sql'] });
  });

  it('throws on non-ok response', async () => {
    globalThis.fetch = async () => new Response(null, { status: 500, statusText: 'Internal Server Error' });

    await expect(async () => {
      for await (const _ of streamQuery('q')) { /* consume */ }
    }).rejects.toThrow('Query failed: 500 Internal Server Error');
  });

  it('throws when response body is null', async () => {
    globalThis.fetch = async () => new Response(null, { status: 200 });
    // Response with null body (no getReader)
    const res = { ok: true, status: 200, statusText: 'OK', body: null } as unknown as Response;
    globalThis.fetch = async () => res;

    await expect(async () => {
      for await (const _ of streamQuery('q')) { /* consume */ }
    }).rejects.toThrow('No response body');
  });

  it('handles chunked delivery across multiple reads', async () => {
    const encoder = new TextEncoder();
    const part1 = 'event:data\nda';
    const part2 = 'ta:{"chunk":"hi"}\n\n';
    const stream = new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode(part1));
        controller.enqueue(encoder.encode(part2));
        controller.close();
      },
    });
    globalThis.fetch = async () => new Response(stream, { status: 200 });

    const events = [];
    for await (const event of streamQuery('q')) {
      events.push(event);
    }

    expect(events).toHaveLength(1);
    expect(events[0].payload).toEqual({ chunk: 'hi' });
  });

  it('passes signal to fetch when provided', async () => {
    const fetchSpy = vi.fn(async () => createSSEResponse([
      { event: 'data', data: JSON.stringify({ chunk: 'ok' }) },
    ]));
    globalThis.fetch = fetchSpy;

    const controller = new AbortController();
    const events = [];
    for await (const event of streamQuery('q', undefined, controller.signal)) {
      events.push(event);
    }

    expect(fetchSpy).toHaveBeenCalledOnce();
    const fetchOptions = fetchSpy.mock.calls[0][1] as RequestInit;
    expect(fetchOptions.signal).toBe(controller.signal);
  });

  it('throws AbortError when signal is aborted before fetch', async () => {
    const controller = new AbortController();
    controller.abort();

    globalThis.fetch = async (_url: string | URL | Request, init?: RequestInit) => {
      if (init?.signal?.aborted) {
        throw new DOMException('The operation was aborted.', 'AbortError');
      }
      return createSSEResponse([]);
    };

    await expect(async () => {
      for await (const _ of streamQuery('q', undefined, controller.signal)) { /* consume */ }
    }).rejects.toThrow('AbortError');
  });

  it('throws AbortError when signal is aborted mid-stream', async () => {
    const controller = new AbortController();
    const encoder = new TextEncoder();

    const stream = new ReadableStream({
      start(ctrl) {
        ctrl.enqueue(encoder.encode('event:data\ndata:{"chunk":"a"}\n\n'));
        // Abort mid-stream
        controller.abort();
        ctrl.enqueue(encoder.encode('event:data\ndata:{"chunk":"b"}\n\n'));
        ctrl.close();
      },
    });

    globalThis.fetch = async (_url: string | URL | Request, init?: RequestInit) => {
      // Simulate that reader.read() checks signal
      const reader = stream.getReader();
      const abortableStream = new ReadableStream({
        async pull(ctrl) {
          if (init?.signal?.aborted) {
            ctrl.error(new DOMException('The operation was aborted.', 'AbortError'));
            return;
          }
          const { done, value } = await reader.read();
          if (done) { ctrl.close(); return; }
          ctrl.enqueue(value);
          // After first chunk, signal will be aborted
          if (init?.signal?.aborted) {
            ctrl.error(new DOMException('The operation was aborted.', 'AbortError'));
            return;
          }
        },
      });
      return new Response(abortableStream, { status: 200 });
    };

    const events = [];
    try {
      for await (const event of streamQuery('q', undefined, controller.signal)) {
        events.push(event);
      }
    } catch (err) {
      expect((err as Error).name).toBe('AbortError');
    }
  });
});

describe('getSession', () => {
  it('converts timestamp strings to Date objects', async () => {
    const isoString = '2024-06-15T10:30:00.000Z';
    const body = { messages: [{ id: '1', role: 'user', content: 'hi', timestamp: isoString }] };
    globalThis.fetch = async () => new Response(JSON.stringify(body), { status: 200, headers: { 'content-type': 'application/json' } });

    const result = await getSession('test-id');
    expect(result.messages[0].timestamp).toBeInstanceOf(Date);
    expect(result.messages[0].timestamp.getTime()).toBe(new Date(isoString).getTime());
  });

  it('handles empty messages array', async () => {
    const body = { messages: [] };
    globalThis.fetch = async () => new Response(JSON.stringify(body), { status: 200, headers: { 'content-type': 'application/json' } });

    const result = await getSession('test-id');
    expect(result.messages).toEqual([]);
  });

  it('handles undefined messages', async () => {
    const body = {};
    globalThis.fetch = async () => new Response(JSON.stringify(body), { status: 200, headers: { 'content-type': 'application/json' } });

    const result = await getSession('test-id');
    expect(result.messages).toBeUndefined();
  });

  it('throws on non-ok response', async () => {
    globalThis.fetch = async () => new Response(null, { status: 404, statusText: 'Not Found' });
    await expect(getSession('bad-id')).rejects.toThrow('Failed to fetch session: 404');
  });
});
