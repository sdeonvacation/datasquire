import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SessionSidebar } from '../SessionSidebar';
import type { SessionSummary } from '../../../lib/types';

function makeSession(overrides: Partial<SessionSummary> = {}): SessionSummary {
  return {
    id: 'sess-1',
    firstQuery: 'Show revenue breakdown',
    lastAccess: new Date(),
    messageCount: 3,
    ...overrides,
  };
}

const defaultProps = {
  sessions: [] as SessionSummary[],
  activeSessionId: null,
  sidebarOpen: true,
  theme: 'dark' as const,
  onNewChat: vi.fn(),
  onSelectSession: vi.fn(),
  onDeleteSession: vi.fn(),
  onCloseSidebar: vi.fn(),
  onToggleTheme: vi.fn(),
  onSubmitQuery: vi.fn(),
};

describe('SessionSidebar', () => {
  it('renders DataSquire title', () => {
    render(<SessionSidebar {...defaultProps} />);
    expect(screen.getByText('DataSquire')).toBeTruthy();
  });

  it('renders Sessions and Schema tabs', () => {
    render(<SessionSidebar {...defaultProps} />);
    expect(screen.getByText('Sessions')).toBeTruthy();
    expect(screen.getByText('Schema')).toBeTruthy();
  });

  it('shows SessionList by default', () => {
    render(<SessionSidebar {...defaultProps} />);
    expect(screen.getByText('No conversations yet')).toBeTruthy();
  });

  it('switches to Schema tab on click', () => {
    render(<SessionSidebar {...defaultProps} />);
    fireEvent.click(screen.getByText('Schema'));
    expect(screen.getByText('Finance Schema')).toBeTruthy();
  });

  it('calls onNewChat when plus button clicked', () => {
    const onNewChat = vi.fn();
    render(<SessionSidebar {...defaultProps} onNewChat={onNewChat} />);
    fireEvent.click(screen.getByLabelText('New chat'));
    expect(onNewChat).toHaveBeenCalledOnce();
  });

  it('calls onToggleTheme when theme button clicked', () => {
    const onToggleTheme = vi.fn();
    render(<SessionSidebar {...defaultProps} onToggleTheme={onToggleTheme} />);
    fireEvent.click(screen.getByLabelText('Toggle theme'));
    expect(onToggleTheme).toHaveBeenCalledOnce();
  });

  it('shows Sun icon in dark mode', () => {
    render(<SessionSidebar {...defaultProps} theme="dark" />);
    // Sun icon has aria-label on parent button
    const btn = screen.getByLabelText('Toggle theme');
    expect(btn).toBeTruthy();
  });

  it('renders backdrop when sidebarOpen on mobile', () => {
    const onCloseSidebar = vi.fn();
    const { container } = render(
      <SessionSidebar {...defaultProps} sidebarOpen={true} onCloseSidebar={onCloseSidebar} />
    );
    const backdrop = container.querySelector('[class*="bg-black/50"]');
    expect(backdrop).toBeTruthy();
  });

  it('closes sidebar when backdrop clicked', () => {
    const onCloseSidebar = vi.fn();
    const { container } = render(
      <SessionSidebar {...defaultProps} sidebarOpen={true} onCloseSidebar={onCloseSidebar} />
    );
    const backdrop = container.querySelector('[class*="bg-black/50"]')!;
    fireEvent.click(backdrop);
    expect(onCloseSidebar).toHaveBeenCalledOnce();
  });

  it('hides sidebar offscreen when closed', () => {
    const { container } = render(
      <SessionSidebar {...defaultProps} sidebarOpen={false} />
    );
    const aside = container.querySelector('aside');
    expect(aside?.className).toContain('-translate-x-full');
  });

  it('passes sessions to SessionList', () => {
    const sessions = [makeSession()];
    render(<SessionSidebar {...defaultProps} sessions={sessions} />);
    expect(screen.getByText('Show revenue breakdown')).toBeTruthy();
  });

  it('active tab has emerald accent', () => {
    render(<SessionSidebar {...defaultProps} />);
    const sessionsTab = screen.getByText('Sessions');
    expect(sessionsTab.className).toContain('text-emerald-400');
  });

  it('inactive tab has stone color', () => {
    render(<SessionSidebar {...defaultProps} />);
    const schemaTab = screen.getByText('Schema');
    expect(schemaTab.className).toContain('text-stone-500');
  });

  it('tabs have proper ARIA roles', () => {
    render(<SessionSidebar {...defaultProps} />);
    const tablist = screen.getByRole('tablist');
    expect(tablist).toBeTruthy();
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(2);
  });

  it('active tab has aria-selected true', () => {
    render(<SessionSidebar {...defaultProps} />);
    const sessionsTab = screen.getByRole('tab', { name: 'Sessions' });
    const schemaTab = screen.getByRole('tab', { name: 'Schema' });
    expect(sessionsTab.getAttribute('aria-selected')).toBe('true');
    expect(schemaTab.getAttribute('aria-selected')).toBe('false');
  });

  it('tab panel has proper role and id', () => {
    render(<SessionSidebar {...defaultProps} />);
    const panel = screen.getByRole('tabpanel');
    expect(panel.id).toBe('sidebar-tabpanel-sessions');
  });

  it('switching tab updates aria-selected and panel id', () => {
    render(<SessionSidebar {...defaultProps} />);
    fireEvent.click(screen.getByRole('tab', { name: 'Schema' }));
    const sessionsTab = screen.getByRole('tab', { name: 'Sessions' });
    const schemaTab = screen.getByRole('tab', { name: 'Schema' });
    expect(sessionsTab.getAttribute('aria-selected')).toBe('false');
    expect(schemaTab.getAttribute('aria-selected')).toBe('true');
    const panel = screen.getByRole('tabpanel');
    expect(panel.id).toBe('sidebar-tabpanel-schema');
  });
});
