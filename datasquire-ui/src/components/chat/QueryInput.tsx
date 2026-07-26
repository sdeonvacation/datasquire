import { useState, useRef, useCallback, type KeyboardEvent, type ChangeEvent } from 'react';
import { ArrowUp, X, Loader2 } from 'lucide-react';

interface QueryInputProps {
  onSend: (query: string) => void;
  onCancel: () => void;
  isStreaming: boolean;
}

export function QueryInput({ onSend, onCancel, isStreaming }: QueryInputProps) {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = useCallback(() => {
    const trimmed = value.trim();
    if (!trimmed || isStreaming) return;
    onSend(trimmed);
    setValue('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  }, [value, isStreaming, onSend]);

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    setValue(e.target.value);
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
  };

  const canSend = value.trim().length > 0 && !isStreaming;

  return (
    <div className="border-t border-stone-200 bg-white px-4 py-3">
      {isStreaming && (
        <div className="flex items-center gap-2 mb-2 text-sm text-stone-500">
          <Loader2 className="w-3.5 h-3.5 animate-spin" />
          <span>Generating...</span>
        </div>
      )}
      <div className="flex items-end gap-2 max-w-3xl mx-auto">
        <textarea
          ref={textareaRef}
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder="Ask about revenue, budgets, invoices..."
          rows={1}
          className="flex-1 resize-none rounded-xl border border-stone-200 px-4 py-2.5 text-sm text-stone-800 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-green-500/30 focus:border-green-500"
        />
        {isStreaming ? (
          <button
            onClick={onCancel}
            className="shrink-0 w-9 h-9 flex items-center justify-center rounded-full bg-red-100 text-red-600 hover:bg-red-200 transition-colors"
            aria-label="Cancel generation"
          >
            <X className="w-4 h-4" />
          </button>
        ) : (
          <button
            onClick={handleSubmit}
            disabled={!canSend}
            className="shrink-0 w-9 h-9 flex items-center justify-center rounded-full bg-green-600 text-white disabled:bg-stone-200 disabled:text-stone-400 hover:bg-green-700 transition-colors"
            aria-label="Send message"
          >
            <ArrowUp className="w-4 h-4" />
          </button>
        )}
      </div>
      <p className="text-xs text-stone-400 text-center mt-2">
        ⌘+Enter to send
      </p>
    </div>
  );
}
