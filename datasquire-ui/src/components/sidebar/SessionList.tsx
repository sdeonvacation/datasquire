import { memo } from 'react';
import { Plus, Trash2, MessageSquare } from 'lucide-react';
import type { SessionSummary } from '../../lib/types';

interface SessionListProps {
  sessions: SessionSummary[];
  activeId: string | null;
  onSelect: (id: string) => void;
  onDelete: (id: string) => void;
  onCreate: () => void;
}

function formatRelativeTime(date: Date): string {
  const now = Date.now();
  const diff = now - new Date(date).getTime();
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function truncate(text: string, max: number): string {
  return text.length > max ? text.slice(0, max) + '…' : text;
}

export const SessionList = memo(function SessionList({ sessions, activeId, onSelect, onDelete, onCreate }: SessionListProps) {
  if (sessions.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 px-3 pt-8">
        <MessageSquare className="h-8 w-8 text-stone-600" />
        <p className="text-sm text-stone-500">No conversations yet</p>
        <button
          onClick={onCreate}
          className="flex items-center gap-1.5 rounded-md bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-500 transition-colors"
        >
          <Plus className="h-3.5 w-3.5" />
          New Chat
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-0.5 overflow-y-auto px-2">
      <button
        onClick={onCreate}
        className="mb-2 flex items-center gap-1.5 rounded-md bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-500 transition-colors"
      >
        <Plus className="h-3.5 w-3.5" />
        New Chat
      </button>
      {sessions.map((session) => (
        <button
          key={session.id}
          onClick={() => onSelect(session.id)}
          className={`group relative flex w-full flex-col items-start rounded-md px-3 py-2 text-left transition-colors ${
            session.id === activeId
              ? 'bg-stone-700 text-stone-100'
              : 'text-stone-300 hover:bg-stone-800'
          }`}
        >
          <span className="w-full truncate text-sm font-medium">
            {truncate(session.firstQuery, 40)}
          </span>
          <span className="flex w-full items-center justify-between text-xs text-stone-500 mt-0.5">
            <span>{formatRelativeTime(session.lastAccess)}</span>
            <span className="rounded-full bg-stone-800 px-1.5 py-0.5 text-[10px] text-stone-400">
              {session.messageCount}
            </span>
          </span>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDelete(session.id);
            }}
            className="absolute right-2 top-2 rounded p-1 text-stone-500 hover:bg-red-900/30 hover:text-red-400 transition-colors opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 focus:opacity-100"
            aria-label={`Delete session: ${session.firstQuery}`}
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        </button>
      ))}
    </div>
  );
});
