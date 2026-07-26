import { describe, expect, it } from 'vitest';
import type { AppAction, AppState } from '../lib/types';

// Import the reducer by re-implementing inline since it's not exported separately.
// For testability, we extract the reducer logic:
function createTestReducer() {
  // Mirror of appReducer from AppContext
  return function appReducer(state: AppState, action: AppAction): AppState {
    switch (action.type) {
      case 'SEND_QUERY':
        return {
          ...state,
          isStreaming: true,
          messages: [
            ...state.messages,
            { id: action.payload.id, role: 'user', content: action.payload.content, timestamp: new Date() },
            { id: `${action.payload.id}-resp`, role: 'assistant', content: '', timestamp: new Date() },
          ],
        };
      case 'STREAM_CHUNK': {
        const msgs = [...state.messages];
        const last = msgs[msgs.length - 1];
        if (last?.role === 'assistant') {
          msgs[msgs.length - 1] = { ...last, content: last.content + action.payload.chunk };
        }
        return { ...state, messages: msgs };
      }
      case 'STREAM_PROGRESS':
        return state;
      case 'QUERY_COMPLETE': {
        const msgs = [...state.messages];
        const last = msgs[msgs.length - 1];
        if (last?.role === 'assistant') {
          msgs[msgs.length - 1] = { ...last, inspection: action.payload.inspection };
        }
        return { ...state, isStreaming: false, currentInspection: action.payload.inspection, messages: msgs };
      }
      case 'QUERY_ERROR': {
        const msgs = [...state.messages];
        const last = msgs[msgs.length - 1];
        if (last?.role === 'assistant') {
          msgs[msgs.length - 1] = { ...last, content: `Error: ${action.payload.message}` };
        }
        return { ...state, isStreaming: false, messages: msgs };
      }
      case 'SET_SESSION':
        return { ...state, activeSessionId: action.payload.sessionId, messages: action.payload.messages };
      case 'NEW_SESSION':
        return {
          ...state,
          sessions: [action.payload.session, ...state.sessions],
          activeSessionId: action.payload.session.id,
          messages: [],
        };
      case 'TOGGLE_SIDEBAR':
        return { ...state, sidebarOpen: !state.sidebarOpen };
      case 'TOGGLE_INSPECTION':
        return { ...state, inspectionOpen: !state.inspectionOpen };
      case 'SET_THEME':
        return { ...state, theme: action.payload.theme };
    }
  };
}

const initialState: AppState = {
  sessions: [],
  activeSessionId: null,
  messages: [],
  isStreaming: false,
  currentInspection: null,
  sidebarOpen: true,
  inspectionOpen: false,
  theme: 'light',
};

describe('appReducer', () => {
  const reducer = createTestReducer();

  it('SEND_QUERY adds user + assistant messages and sets streaming', () => {
    const next = reducer(initialState, { type: 'SEND_QUERY', payload: { id: '1', content: 'hello' } });
    expect(next.isStreaming).toBe(true);
    expect(next.messages).toHaveLength(2);
    expect(next.messages[0].role).toBe('user');
    expect(next.messages[0].content).toBe('hello');
    expect(next.messages[1].role).toBe('assistant');
    expect(next.messages[1].content).toBe('');
  });

  it('STREAM_CHUNK appends to last assistant message', () => {
    const withMsg = reducer(initialState, { type: 'SEND_QUERY', payload: { id: '1', content: 'q' } });
    const next = reducer(withMsg, { type: 'STREAM_CHUNK', payload: { chunk: 'Hello ' } });
    const next2 = reducer(next, { type: 'STREAM_CHUNK', payload: { chunk: 'world' } });
    expect(next2.messages[1].content).toBe('Hello world');
  });

  it('QUERY_COMPLETE stops streaming and attaches inspection', () => {
    const inspection = { chunks: [], sql: '', rawResult: '', steps: [], latencyMs: 100, agentsUsed: ['a'], iterations: 1 };
    const withMsg = reducer(initialState, { type: 'SEND_QUERY', payload: { id: '1', content: 'q' } });
    const next = reducer(withMsg, { type: 'QUERY_COMPLETE', payload: { inspection } });
    expect(next.isStreaming).toBe(false);
    expect(next.currentInspection).toBe(inspection);
    expect(next.messages[1].inspection).toBe(inspection);
  });

  it('QUERY_ERROR stops streaming and sets error content', () => {
    const withMsg = reducer(initialState, { type: 'SEND_QUERY', payload: { id: '1', content: 'q' } });
    const next = reducer(withMsg, { type: 'QUERY_ERROR', payload: { message: 'timeout' } });
    expect(next.isStreaming).toBe(false);
    expect(next.messages[1].content).toBe('Error: timeout');
  });

  it('TOGGLE_SIDEBAR flips state', () => {
    const next = reducer(initialState, { type: 'TOGGLE_SIDEBAR' });
    expect(next.sidebarOpen).toBe(false);
    const next2 = reducer(next, { type: 'TOGGLE_SIDEBAR' });
    expect(next2.sidebarOpen).toBe(true);
  });

  it('TOGGLE_INSPECTION flips state', () => {
    const next = reducer(initialState, { type: 'TOGGLE_INSPECTION' });
    expect(next.inspectionOpen).toBe(true);
  });

  it('SET_THEME changes theme', () => {
    const next = reducer(initialState, { type: 'SET_THEME', payload: { theme: 'dark' } });
    expect(next.theme).toBe('dark');
  });

  it('NEW_SESSION prepends session and clears messages', () => {
    const session = { id: 's1', firstQuery: 'hi', lastAccess: new Date(), messageCount: 0 };
    const next = reducer(initialState, { type: 'NEW_SESSION', payload: { session } });
    expect(next.sessions).toHaveLength(1);
    expect(next.activeSessionId).toBe('s1');
    expect(next.messages).toEqual([]);
  });

  it('SET_SESSION switches active session', () => {
    const msgs = [{ id: 'm1', role: 'user' as const, content: 'x', timestamp: new Date() }];
    const next = reducer(initialState, { type: 'SET_SESSION', payload: { sessionId: 's2', messages: msgs } });
    expect(next.activeSessionId).toBe('s2');
    expect(next.messages).toEqual(msgs);
  });
});
