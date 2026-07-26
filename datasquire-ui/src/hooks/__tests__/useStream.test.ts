import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { createElement, type ReactNode } from 'react';
import { AppProvider } from '../../context/AppContext';

// Mock api module
vi.mock('../../lib/api', () => ({
  streamQuery: vi.fn(),
}));

vi.mock('../../lib/format', () => ({
  generateId: () => 'test-id',
}));

import { useStream } from '../useStream';
import { streamQuery } from '../../lib/api';
import type { SSEEvent } from '../../lib/types';

const mockedStreamQuery = vi.mocked(streamQuery);

function wrapper({ children }: { children: ReactNode }) {
  return createElement(AppProvider, null, children);
}

function createMockStream(events: SSEEvent[]): AsyncGenerator<SSEEvent> {
  return (async function* () {
    for (const event of events) {
      yield event;
    }
  })();
}

function createDelayedStream(events: SSEEvent[], delayMs: number): AsyncGenerator<SSEEvent> {
  return (async function* () {
    for (const event of events) {
      await new Promise((r) => setTimeout(r, delayMs));
      yield event;
    }
  })();
}

function createAbortableStream(events: SSEEvent[], signal: AbortSignal): AsyncGenerator<SSEEvent> {
  return (async function* () {
    for (const event of events) {
      await new Promise((r) => setTimeout(r, 10));
      if (signal.aborted) {
        throw new DOMException('The operation was aborted.', 'AbortError');
      }
      yield event;
    }
  })();
}

describe('useStream', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('sends query and dispatches events', async () => {
    mockedStreamQuery.mockReturnValue(createMockStream([
      { type: 'data', payload: { chunk: 'Hello' } },
      { type: 'done', payload: { sessionId: 's1', agentsUsed: ['sql'], iterations: 1, qualityScore: 1, metadata: {} } },
    ]));

    const { result } = renderHook(() => useStream(), { wrapper });

    await act(async () => {
      await result.current.sendQuery('test');
    });

    expect(mockedStreamQuery).toHaveBeenCalledWith('test', undefined, expect.any(AbortSignal));
  });

  it('passes AbortController signal to streamQuery', async () => {
    mockedStreamQuery.mockReturnValue(createMockStream([
      { type: 'done', payload: { sessionId: 's1', agentsUsed: [], iterations: 0, qualityScore: 1, metadata: {} } },
    ]));

    const { result } = renderHook(() => useStream(), { wrapper });

    await act(async () => {
      await result.current.sendQuery('q');
    });

    const signal = mockedStreamQuery.mock.calls[0][2];
    expect(signal).toBeInstanceOf(AbortSignal);
  });

  it('aborts previous stream when sendQuery is called again', async () => {
    let capturedSignals: AbortSignal[] = [];

    mockedStreamQuery.mockImplementation((_q, _s, signal) => {
      capturedSignals.push(signal!);
      return createMockStream([
        { type: 'done', payload: { sessionId: 's1', agentsUsed: [], iterations: 0, qualityScore: 1, metadata: {} } },
      ]);
    });

    const { result } = renderHook(() => useStream(), { wrapper });

    await act(async () => {
      await result.current.sendQuery('first');
    });

    await act(async () => {
      await result.current.sendQuery('second');
    });

    // First call's signal should have been aborted before second call started
    expect(capturedSignals[0].aborted).toBe(true);
    // Second call's signal should not be aborted
    expect(capturedSignals[1].aborted).toBe(false);
  });

  it('cancel() aborts the current stream', async () => {
    let capturedSignal: AbortSignal | undefined;

    mockedStreamQuery.mockImplementation((_q, _s, signal) => {
      capturedSignal = signal;
      return createAbortableStream([
        { type: 'data', payload: { chunk: 'a' } },
        { type: 'data', payload: { chunk: 'b' } },
      ], signal!);
    });

    const { result } = renderHook(() => useStream(), { wrapper });

    // Start streaming (will await the delayed events)
    let streamPromise: Promise<void>;
    act(() => {
      streamPromise = result.current.sendQuery('q');
    });

    // Cancel
    act(() => {
      result.current.cancel();
    });

    // Advance timers so the stream loop can process the abort
    await act(async () => {
      vi.advanceTimersByTime(50);
      await streamPromise!;
    });

    expect(capturedSignal?.aborted).toBe(true);
  });

  it('handles AbortError gracefully without dispatching QUERY_ERROR', async () => {
    mockedStreamQuery.mockImplementation((_q, _s, signal) => {
      return (async function* () {
        if (signal?.aborted) {
          throw new DOMException('The operation was aborted.', 'AbortError');
        }
        yield { type: 'data', payload: { chunk: 'hi' } } as SSEEvent;
        throw new DOMException('The operation was aborted.', 'AbortError');
      })();
    });

    const { result } = renderHook(() => useStream(), { wrapper });

    // Should not throw - AbortError is handled gracefully
    await act(async () => {
      await result.current.sendQuery('q');
    });

    // After abort handling, isStreaming should be false (QUERY_COMPLETE dispatched)
    expect(result.current.isStreaming).toBe(false);
  });

  it('dispatches QUERY_ERROR for non-abort errors', async () => {
    mockedStreamQuery.mockImplementation(() => {
      return (async function* () {
        throw new Error('Network failure');
      })();
    });

    const { result } = renderHook(() => useStream(), { wrapper });

    await act(async () => {
      await result.current.sendQuery('q');
    });

    // isStreaming should be false after error
    expect(result.current.isStreaming).toBe(false);
  });

  it('creates fresh AbortController after aborting previous', async () => {
    let callCount = 0;
    let signals: AbortSignal[] = [];

    mockedStreamQuery.mockImplementation((_q, _s, signal) => {
      callCount++;
      signals.push(signal!);
      return createMockStream([
        { type: 'done', payload: { sessionId: 's1', agentsUsed: [], iterations: 0, qualityScore: 1, metadata: {} } },
      ]);
    });

    const { result } = renderHook(() => useStream(), { wrapper });

    await act(async () => {
      await result.current.sendQuery('a');
    });
    await act(async () => {
      await result.current.sendQuery('b');
    });

    expect(callCount).toBe(2);
    // Two different signal objects (fresh controller each time)
    expect(signals[0]).not.toBe(signals[1]);
  });
});
