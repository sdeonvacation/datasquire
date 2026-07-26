import { createContext, useContext, useMemo, useReducer } from 'react';
import type { ReactNode } from 'react';
import type { AppAction, AppState } from '../lib/types';

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

function appReducer(state: AppState, action: AppAction): AppState {
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
      return state; // Progress tracked via useStream hook for UI display
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
}

interface AppContextValue {
  state: AppState;
  dispatch: React.Dispatch<AppAction>;
}

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(appReducer, initialState);
  const value = useMemo(() => ({ state, dispatch }), [state]);
  return <AppContext value={value}>{children}</AppContext>;
}

export function useApp(): AppContextValue {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within AppProvider');
  return ctx;
}
