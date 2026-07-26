import { useCallback } from 'react';
import { useApp } from '../context/AppContext';
import { useStream } from '../hooks/useStream';
import { getSession, deleteSession } from '../lib/api';
import { generateId } from '../lib/format';
import { ChatThread } from './chat';
import { QueryInput } from './chat';
import { InspectionPanel } from './inspection';
import { SessionSidebar } from './sidebar/SessionSidebar';
import type { Message } from '../lib/types';

export function AppShell() {
  const { state, dispatch } = useApp();
  const { sendQuery, isStreaming, cancel } = useStream();

  const handleNewChat = useCallback(() => {
    dispatch({
      type: 'NEW_SESSION',
      payload: { session: { id: generateId(), firstQuery: '', lastAccess: new Date(), messageCount: 0 } },
    });
  }, [dispatch]);

  const handleSelectSession = useCallback(async (id: string) => {
    const { messages } = await getSession(id);
    dispatch({ type: 'SET_SESSION', payload: { sessionId: id, messages } });
  }, [dispatch]);

  const handleDeleteSession = useCallback(async (id: string) => {
    await deleteSession(id);
  }, []);

  const handleToggleTheme = useCallback(() => {
    dispatch({ type: 'SET_THEME', payload: { theme: state.theme === 'light' ? 'dark' : 'light' } });
  }, [dispatch, state.theme]);

  const handleInspect = useCallback((_msg: Message) => {
    dispatch({ type: 'TOGGLE_INSPECTION' });
  }, [dispatch]);

  // During streaming, the last assistant message is the partial response — show it via StreamingMessage
  const streamContent = isStreaming
    ? (state.messages[state.messages.length - 1]?.content ?? '')
    : '';

  // Exclude the in-progress assistant message from the bubbles list during streaming
  const displayMessages = isStreaming
    ? state.messages.slice(0, -1)
    : state.messages;

  return (
    <div className="flex h-screen overflow-hidden bg-[var(--color-bg-primary)]">
      {/* Sidebar */}
      <aside
        className={`
          w-[260px] shrink-0 bg-[var(--color-bg-sidebar)] text-[var(--color-text-inverse)]
          flex flex-col border-r border-[var(--color-border)]
          max-md:absolute max-md:inset-y-0 max-md:left-0 max-md:z-40
          transition-transform duration-200
          ${state.sidebarOpen ? 'translate-x-0' : 'max-md:-translate-x-full'}
        `}
      >
        <SessionSidebar
          sessions={state.sessions}
          activeSessionId={state.activeSessionId}
          sidebarOpen={state.sidebarOpen}
          theme={state.theme}
          onNewChat={handleNewChat}
          onSelectSession={handleSelectSession}
          onDeleteSession={handleDeleteSession}
          onCloseSidebar={() => dispatch({ type: 'TOGGLE_SIDEBAR' })}
          onToggleTheme={handleToggleTheme}
          onSubmitQuery={sendQuery}
        />
      </aside>

      {/* Mobile sidebar overlay */}
      {state.sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/30 md:hidden"
          onClick={() => dispatch({ type: 'TOGGLE_SIDEBAR' })}
          aria-hidden="true"
        />
      )}

      {/* Main content */}
      <main className="flex-1 flex flex-col min-w-0 relative">
        {/* Top bar with toggles */}
        <header className="h-12 flex items-center justify-between px-4 border-b border-[var(--color-border)] shrink-0">
          <button
            type="button"
            onClick={() => dispatch({ type: 'TOGGLE_SIDEBAR' })}
            className="md:hidden p-1 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
            aria-label="Toggle sidebar"
          >
            ☰
          </button>
          <span className="text-sm text-[var(--color-text-secondary)] font-[var(--font-mono)]">
            {state.isStreaming ? 'Processing...' : 'Ready'}
          </span>
          <button
            type="button"
            onClick={() => dispatch({ type: 'TOGGLE_INSPECTION' })}
            className="p-1 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] max-lg:hidden"
            aria-label="Toggle inspection panel"
          >
            ⟨⟩
          </button>
        </header>

        {/* Chat area */}
        <ChatThread
          messages={displayMessages}
          isStreaming={isStreaming}
          streamContent={streamContent}
          onInspect={handleInspect}
          onSuggestedQuery={sendQuery}
        />
        {/* Query input anchored to bottom */}
        <QueryInput
          onSend={sendQuery}
          onCancel={cancel}
          isStreaming={isStreaming}
        />
      </main>

      {/* Inspection panel */}
      {state.inspectionOpen && (
        <aside className="w-[320px] shrink-0 bg-[var(--color-bg-inspection)] border-l border-[var(--color-border)] overflow-y-auto max-lg:hidden">
          <InspectionPanel />
        </aside>
      )}
    </div>
  );
}
