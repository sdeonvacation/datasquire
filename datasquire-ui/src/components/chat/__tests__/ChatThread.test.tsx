import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ChatThread } from '../ChatThread';
import type { Message } from '../../../lib/types';

function makeMessages(count: number): Message[] {
  return Array.from({ length: count }, (_, i) => ({
    id: String(i),
    role: i % 2 === 0 ? 'user' : 'assistant',
    content: `Message ${i}`,
    timestamp: new Date(),
  })) as Message[];
}

describe('ChatThread', () => {
  const onInspect = vi.fn();
  const onSuggestedQuery = vi.fn();

  it('shows suggested queries when messages array is empty', () => {
    render(
      <ChatThread
        messages={[]}
        isStreaming={false}
        streamContent=""
        onInspect={onInspect}
        onSuggestedQuery={onSuggestedQuery}
      />
    );
    expect(screen.getByText('Finance Controller Dashboard')).toBeTruthy();
  });

  it('renders message bubbles when messages exist', () => {
    render(
      <ChatThread
        messages={makeMessages(3)}
        isStreaming={false}
        streamContent=""
        onInspect={onInspect}
        onSuggestedQuery={onSuggestedQuery}
      />
    );
    expect(screen.getByText('Message 0')).toBeTruthy();
    expect(screen.getByText('Message 1')).toBeTruthy();
    expect(screen.getByText('Message 2')).toBeTruthy();
  });

  it('shows streaming message when isStreaming is true', () => {
    render(
      <ChatThread
        messages={makeMessages(1)}
        isStreaming={true}
        streamContent="Partial response"
        onInspect={onInspect}
        onSuggestedQuery={onSuggestedQuery}
      />
    );
    expect(screen.getByText('Partial response')).toBeTruthy();
  });

  it('does not show streaming message when not streaming', () => {
    render(
      <ChatThread
        messages={makeMessages(1)}
        isStreaming={false}
        streamContent=""
        onInspect={onInspect}
        onSuggestedQuery={onSuggestedQuery}
      />
    );
    expect(screen.queryByText('Thinking...')).toBeNull();
  });
});

describe('ChatThread - SuggestedQueries interaction', () => {
  it('calls onSuggestedQuery when suggestion card clicked', () => {
    const onSuggestedQuery = vi.fn();
    render(
      <ChatThread
        messages={[]}
        isStreaming={false}
        streamContent=""
        onInspect={vi.fn()}
        onSuggestedQuery={onSuggestedQuery}
      />
    );
    fireEvent.click(screen.getByText("What's our total revenue YTD by business unit?"));
    expect(onSuggestedQuery).toHaveBeenCalledWith(
      "What's our total revenue YTD by business unit?"
    );
  });
});
