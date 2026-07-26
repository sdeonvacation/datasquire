import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SessionList } from '../SessionList';
import type { SessionSummary } from '../../../lib/types';

function makeSession(overrides: Partial<SessionSummary> = {}): SessionSummary {
  return {
    id: 'sess-1',
    firstQuery: 'What is the total revenue?',
    lastAccess: new Date(Date.now() - 60_000),
    messageCount: 4,
    ...overrides,
  };
}

const defaultProps = {
  sessions: [] as SessionSummary[],
  activeId: null,
  onSelect: vi.fn(),
  onDelete: vi.fn(),
  onCreate: vi.fn(),
};

describe('SessionList', () => {
  it('renders empty state when no sessions', () => {
    render(<SessionList {...defaultProps} />);
    expect(screen.getByText('No conversations yet')).toBeTruthy();
  });

  it('shows New Chat button in empty state', () => {
    const onCreate = vi.fn();
    render(<SessionList {...defaultProps} onCreate={onCreate} />);
    fireEvent.click(screen.getByText('New Chat'));
    expect(onCreate).toHaveBeenCalledOnce();
  });

  it('renders session items with truncated text', () => {
    const longQuery = 'This is a very long query that should be truncated at forty characters mark';
    render(
      <SessionList
        {...defaultProps}
        sessions={[makeSession({ firstQuery: longQuery })]}
      />
    );
    expect(screen.queryByText(longQuery)).toBeNull();
    expect(screen.getByText('This is a very long query that should be…')).toBeTruthy();
  });

  it('highlights active session', () => {
    const session = makeSession();
    render(
      <SessionList
        {...defaultProps}
        sessions={[session]}
        activeId="sess-1"
      />
    );
    const item = screen.getByText('What is the total revenue?').closest('button');
    expect(item?.className).toContain('bg-stone-700');
  });

  it('calls onSelect when session clicked', () => {
    const onSelect = vi.fn();
    render(
      <SessionList
        {...defaultProps}
        sessions={[makeSession()]}
        onSelect={onSelect}
      />
    );
    fireEvent.click(screen.getByText('What is the total revenue?'));
    expect(onSelect).toHaveBeenCalledWith('sess-1');
  });

  it('shows delete button on hover and calls onDelete', () => {
    const onDelete = vi.fn();
    const session = makeSession();
    render(
      <SessionList
        {...defaultProps}
        sessions={[session]}
        onDelete={onDelete}
      />
    );
    const deleteBtn = screen.getByLabelText(`Delete session: ${session.firstQuery}`);
    fireEvent.click(deleteBtn);
    expect(onDelete).toHaveBeenCalledWith('sess-1');
  });

  it('delete button is always in DOM for keyboard accessibility', () => {
    render(
      <SessionList
        {...defaultProps}
        sessions={[makeSession()]}
      />
    );
    const deleteBtn = screen.getByLabelText('Delete session: What is the total revenue?');
    expect(deleteBtn).toBeTruthy();
    // Visually hidden via opacity-0 but accessible
    expect(deleteBtn.className).toContain('opacity-0');
  });

  it('delete button becomes visible on focus', () => {
    render(
      <SessionList
        {...defaultProps}
        sessions={[makeSession()]}
      />
    );
    const deleteBtn = screen.getByLabelText('Delete session: What is the total revenue?');
    expect(deleteBtn.className).toContain('focus:opacity-100');
  });

  it('displays message count badge', () => {
    render(
      <SessionList
        {...defaultProps}
        sessions={[makeSession({ messageCount: 7 })]}
      />
    );
    expect(screen.getByText('7')).toBeTruthy();
  });

  it('displays relative time', () => {
    render(
      <SessionList
        {...defaultProps}
        sessions={[makeSession({ lastAccess: new Date(Date.now() - 3_600_000) })]}
      />
    );
    expect(screen.getByText('1h ago')).toBeTruthy();
  });

  it('renders multiple sessions', () => {
    const sessions = [
      makeSession({ id: '1', firstQuery: 'First query' }),
      makeSession({ id: '2', firstQuery: 'Second query' }),
    ];
    render(<SessionList {...defaultProps} sessions={sessions} />);
    expect(screen.getByText('First query')).toBeTruthy();
    expect(screen.getByText('Second query')).toBeTruthy();
  });
});
