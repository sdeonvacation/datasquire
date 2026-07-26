import { useCallback } from 'react';
import { useApp } from '../context/AppContext';
import * as api from '../lib/api';
import { generateId } from '../lib/format';
import type { SessionSummary } from '../lib/types';

export function useSession() {
  const { state, dispatch } = useApp();

  const createSession = useCallback((firstQuery: string): SessionSummary => {
    const session: SessionSummary = {
      id: generateId(),
      firstQuery,
      lastAccess: new Date(),
      messageCount: 0,
    };
    dispatch({ type: 'NEW_SESSION', payload: { session } });
    return session;
  }, [dispatch]);

  const switchSession = useCallback(async (sessionId: string) => {
    const data = await api.getSession(sessionId);
    dispatch({ type: 'SET_SESSION', payload: { sessionId, messages: data.messages } });
  }, [dispatch]);

  const deleteSession = useCallback(async (sessionId: string) => {
    await api.deleteSession(sessionId);
    if (state.activeSessionId === sessionId) {
      dispatch({ type: 'SET_SESSION', payload: { sessionId: '', messages: [] } });
    }
  }, [dispatch, state.activeSessionId]);

  return {
    sessions: state.sessions,
    activeSession: state.activeSessionId,
    createSession,
    switchSession,
    deleteSession,
  };
}
