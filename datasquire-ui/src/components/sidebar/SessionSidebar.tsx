import { useState } from 'react';
import { Plus, Sun, Moon, PanelLeftClose, Database as DatabaseIcon } from 'lucide-react';
import type { SessionSummary } from '../../lib/types';
import { SessionList } from './SessionList';
import { SchemaExplorer } from './SchemaExplorer';

type Tab = 'sessions' | 'schema';

interface SessionSidebarProps {
  sessions: SessionSummary[];
  activeSessionId: string | null;
  sidebarOpen: boolean;
  theme: 'light' | 'dark';
  onNewChat: () => void;
  onSelectSession: (id: string) => void;
  onDeleteSession: (id: string) => void;
  onCloseSidebar: () => void;
  onToggleTheme: () => void;
  onSubmitQuery: (query: string) => void;
}

export function SessionSidebar({
  sessions,
  activeSessionId,
  sidebarOpen,
  theme,
  onNewChat,
  onSelectSession,
  onDeleteSession,
  onCloseSidebar,
  onToggleTheme,
  onSubmitQuery,
}: SessionSidebarProps) {
  const [activeTab, setActiveTab] = useState<Tab>('sessions');

  return (
    <>
      {/* Mobile backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={onCloseSidebar}
          aria-hidden="true"
        />
      )}

      <aside
        className={`fixed top-0 left-0 z-50 flex h-full w-[260px] flex-col bg-stone-900 transition-transform duration-200 md:relative md:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-stone-800 px-4 py-3">
          <div className="flex items-center gap-2">
            <DatabaseIcon className="h-5 w-5 text-emerald-400" />
            <h1 className="text-base font-semibold text-white">DataSquire</h1>
          </div>
          <button
            onClick={onNewChat}
            className="rounded-md bg-emerald-600 p-1.5 text-white hover:bg-emerald-500 transition-colors"
            aria-label="New chat"
          >
            <Plus className="h-4 w-4" />
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-stone-800" role="tablist">
          <button
            role="tab"
            aria-selected={activeTab === 'sessions'}
            aria-controls="sidebar-tabpanel-sessions"
            onClick={() => setActiveTab('sessions')}
            className={`flex-1 py-2 text-xs font-medium transition-colors ${
              activeTab === 'sessions'
                ? 'border-b-2 border-emerald-400 text-emerald-400'
                : 'text-stone-500 hover:text-stone-300'
            }`}
          >
            Sessions
          </button>
          <button
            role="tab"
            aria-selected={activeTab === 'schema'}
            aria-controls="sidebar-tabpanel-schema"
            onClick={() => setActiveTab('schema')}
            className={`flex-1 py-2 text-xs font-medium transition-colors ${
              activeTab === 'schema'
                ? 'border-b-2 border-emerald-400 text-emerald-400'
                : 'text-stone-500 hover:text-stone-300'
            }`}
          >
            Schema
          </button>
        </div>

        {/* Content */}
        <div
          className="flex-1 overflow-y-auto py-2"
          role="tabpanel"
          id={activeTab === 'sessions' ? 'sidebar-tabpanel-sessions' : 'sidebar-tabpanel-schema'}
          aria-label={activeTab === 'sessions' ? 'Sessions' : 'Schema'}
        >
          {activeTab === 'sessions' ? (
            <SessionList
              sessions={sessions}
              activeId={activeSessionId}
              onSelect={onSelectSession}
              onDelete={onDeleteSession}
              onCreate={onNewChat}
            />
          ) : (
            <SchemaExplorer onTableClick={onSubmitQuery} />
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-stone-800 px-4 py-2">
          <button
            onClick={onToggleTheme}
            className="rounded-md p-1.5 text-stone-400 hover:bg-stone-800 hover:text-stone-200 transition-colors"
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>
          <button
            onClick={onCloseSidebar}
            className="rounded-md p-1.5 text-stone-400 hover:bg-stone-800 hover:text-stone-200 transition-colors md:hidden"
            aria-label="Close sidebar"
          >
            <PanelLeftClose className="h-4 w-4" />
          </button>
        </div>
      </aside>
    </>
  );
}
