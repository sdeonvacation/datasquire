import { useCallback, useRef } from 'react';
import { useApp } from '../context/AppContext';
import { streamQuery } from '../lib/api';
import { generateId } from '../lib/format';
import type { InspectionData, StepInfo } from '../lib/types';

export function useStream() {
  const { state, dispatch } = useApp();
  const abortRef = useRef<AbortController | null>(null);
  const stepsRef = useRef<StepInfo[]>([]);

  const sendQuery = useCallback(async (query: string, sessionId?: string) => {
    // Abort any existing stream before starting a new one
    abortRef.current?.abort();
    abortRef.current = new AbortController();
    stepsRef.current = [];

    const msgId = generateId();
    dispatch({ type: 'SEND_QUERY', payload: { id: msgId, content: query } });

    const startTime = performance.now();
    let agentsUsed: string[] = [];
    let iterations = 0;

    try {
      for await (const event of streamQuery(query, sessionId, abortRef.current.signal)) {
        if (abortRef.current.signal.aborted) break;

        switch (event.type) {
          case 'data':
            dispatch({ type: 'STREAM_CHUNK', payload: { chunk: (event.payload as { chunk: string }).chunk } });
            break;
          case 'progress': {
            const p = event.payload as { step: StepInfo['step']; detail: string };
            stepsRef.current = updateSteps(stepsRef.current, p.step, p.detail);
            dispatch({ type: 'STREAM_PROGRESS', payload: p });
            break;
          }
          case 'done': {
            const d = event.payload as { sessionId: string; agentsUsed: string[]; iterations: number };
            agentsUsed = d.agentsUsed;
            iterations = d.iterations;
            break;
          }
          case 'error': {
            const e = event.payload as { message: string };
            dispatch({ type: 'QUERY_ERROR', payload: { message: e.message } });
            return;
          }
        }
      }

      const inspection: InspectionData = {
        chunks: [],
        sql: '',
        rawResult: '',
        steps: stepsRef.current,
        latencyMs: Math.round(performance.now() - startTime),
        agentsUsed,
        iterations,
      };
      dispatch({ type: 'QUERY_COMPLETE', payload: { inspection } });
    } catch (err) {
      // AbortError is expected when user cancels or a new query supersedes
      if (err instanceof Error && err.name === 'AbortError') {
        dispatch({ type: 'QUERY_COMPLETE', payload: { inspection: {
          chunks: [], sql: '', rawResult: '', steps: stepsRef.current,
          latencyMs: Math.round(performance.now() - startTime),
          agentsUsed, iterations,
        } } });
        return;
      }
      const message = err instanceof Error ? err.message : 'Stream failed';
      dispatch({ type: 'QUERY_ERROR', payload: { message } });
    }
  }, [dispatch]);

  const cancel = useCallback(() => {
    abortRef.current?.abort();
  }, []);

  return { sendQuery, isStreaming: state.isStreaming, cancel };
}

function updateSteps(steps: StepInfo[], step: StepInfo['step'], detail: string): StepInfo[] {
  const existing = steps.find((s) => s.step === step);
  if (existing) {
    return steps.map((s) => (s.step === step ? { ...s, status: 'active', detail } : s));
  }
  // Mark previous steps as done
  const updated = steps.map((s) => (s.status === 'active' ? { ...s, status: 'done' as const } : s));
  return [...updated, { step, status: 'active', detail }];
}
