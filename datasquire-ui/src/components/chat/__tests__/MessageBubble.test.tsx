import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MessageBubble } from '../MessageBubble';
import type { Message } from '../../../lib/types';

function makeMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: '1',
    role: 'user',
    content: 'Hello',
    timestamp: new Date(Date.now() - 30_000),
    ...overrides,
  };
}

describe('MessageBubble', () => {
  it('renders user message with right alignment', () => {
    render(<MessageBubble message={makeMessage()} />);
    const bubble = screen.getByText('Hello');
    expect(bubble.closest('[class*="justify-end"]')).toBeTruthy();
  });

  it('renders assistant message with left alignment', () => {
    render(
      <MessageBubble message={makeMessage({ role: 'assistant', content: 'Response' })} />
    );
    const container = screen.getByText('Response').closest('[class*="justify-start"]');
    expect(container).toBeTruthy();
  });

  it('renders markdown tables in assistant messages', () => {
    const content = '| Col A | Col B |\n|---|---|\n| 1 | 2 |';
    render(<MessageBubble message={makeMessage({ role: 'assistant', content })} />);
    expect(screen.getByRole('table')).toBeTruthy();
  });

  it('shows inspect button only for assistant messages with onInspect', () => {
    const onInspect = vi.fn();
    render(
      <MessageBubble
        message={makeMessage({ role: 'assistant', content: 'Result' })}
        onInspect={onInspect}
      />
    );
    const btn = screen.getByLabelText('Inspect query details');
    fireEvent.click(btn);
    expect(onInspect).toHaveBeenCalledOnce();
  });

  it('does not show inspect button for user messages', () => {
    render(<MessageBubble message={makeMessage()} />);
    expect(screen.queryByLabelText('Inspect query details')).toBeNull();
  });

  it('displays relative timestamp', () => {
    render(<MessageBubble message={makeMessage({ timestamp: new Date(Date.now() - 120_000) })} />);
    expect(screen.getByText('2m ago')).toBeTruthy();
  });

  it('right-aligns numeric table cells', () => {
    const content = '| Item | Amount |\n|---|---|\n| Rent | $5,000 |';
    render(<MessageBubble message={makeMessage({ role: 'assistant', content })} />);
    const cell = screen.getByText('$5,000');
    expect(cell.className).toContain('text-right');
  });

  it('is wrapped in React.memo for performance', () => {
    // React.memo wraps the component — verify the export has $$typeof for memo
    expect(MessageBubble).toHaveProperty('type');
  });
});
