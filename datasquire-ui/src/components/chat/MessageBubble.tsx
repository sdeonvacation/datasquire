import { memo } from 'react';
import { Search } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { Components } from 'react-markdown';
import type { Message } from '../../lib/types';

interface MessageBubbleProps {
  message: Message;
  onInspect?: () => void;
}

function formatRelativeTime(date: Date): string {
  const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

const markdownComponents: Components = {
  table: ({ children }) => (
    <div className="overflow-x-auto my-2">
      <table className="w-full border-collapse text-sm font-mono">{children}</table>
    </div>
  ),
  th: ({ children }) => (
    <th className="border border-stone-200 bg-stone-50 px-3 py-1.5 text-left font-semibold text-stone-700">
      {children}
    </th>
  ),
  td: ({ children }) => {
    const text = String(children ?? '');
    const isNumeric = /^[$€£]?[\d,]+\.?\d*%?$/.test(text.trim());
    return (
      <td className={`border border-stone-200 px-3 py-1.5 ${isNumeric ? 'text-right tabular-nums' : 'text-left'}`}>
        {children}
      </td>
    );
  },
  code: ({ children, className }) => {
    const isBlock = className?.includes('language-');
    if (isBlock) {
      return (
        <code className={`block bg-stone-900 text-stone-100 p-3 rounded-md text-sm font-[var(--font-mono)] overflow-x-auto ${className ?? ''}`}>
          {children}
        </code>
      );
    }
    return (
      <code className="bg-stone-100 text-stone-800 px-1.5 py-0.5 rounded text-sm font-[var(--font-mono)]">
        {children}
      </code>
    );
  },
  pre: ({ children }) => <pre className="my-2">{children}</pre>,
};

export const MessageBubble = memo(function MessageBubble({ message, onInspect }: MessageBubbleProps) {
  const isUser = message.role === 'user';

  if (isUser) {
    return (
      <div className="flex justify-end mb-4">
        <div className="max-w-[80%] md:max-w-[60%]">
          <div className="bg-stone-800 text-white px-4 py-2.5 rounded-2xl rounded-br-sm">
            <p className="whitespace-pre-wrap text-sm">{message.content}</p>
          </div>
          <p className="text-xs text-stone-400 mt-1 text-right">
            {formatRelativeTime(message.timestamp)}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex justify-start mb-4">
      <div className="w-full max-w-full md:max-w-[85%]">
        <div className="bg-white border border-stone-200 px-4 py-3 rounded-2xl rounded-bl-sm">
          <div className="prose prose-sm prose-stone max-w-none">
            <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
              {message.content}
            </ReactMarkdown>
          </div>
          {onInspect && (
            <button
              onClick={onInspect}
              className="mt-2 flex items-center gap-1 text-xs text-stone-400 hover:text-green-600 transition-colors"
              aria-label="Inspect query details"
            >
              <Search className="w-3 h-3" />
              Inspect
            </button>
          )}
        </div>
        <p className="text-xs text-stone-400 mt-1">
          {formatRelativeTime(message.timestamp)}
        </p>
      </div>
    </div>
  );
});
