import { useEffect, useRef } from 'react';
import { MessageBubble } from './MessageBubble';
import { StreamingMessage } from './StreamingMessage';
import { SuggestedQueries } from './SuggestedQueries';
import type { Message } from '../../lib/types';

interface ChatThreadProps {
  messages: Message[];
  isStreaming: boolean;
  streamContent: string;
  onInspect: (message: Message) => void;
  onSuggestedQuery: (query: string) => void;
}

export function ChatThread({
  messages,
  isStreaming,
  streamContent,
  onInspect,
  onSuggestedQuery,
}: ChatThreadProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length, streamContent]);

  if (messages.length === 0 && !isStreaming) {
    return (
      <div className="flex-1 overflow-y-auto">
        <SuggestedQueries onSelect={onSuggestedQuery} />
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto px-4 py-6">
      <div className="max-w-3xl mx-auto">
        {messages.map((msg) => (
          <MessageBubble
            key={msg.id}
            message={msg}
            onInspect={msg.role === 'assistant' ? () => onInspect(msg) : undefined}
          />
        ))}
        {isStreaming && <StreamingMessage content={streamContent} />}
        <div ref={bottomRef} />
      </div>
    </div>
  );
}
