import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { createElement, type ReactNode } from 'react';
import { AppProvider } from '../../context/AppContext';

// Mock child components to verify they are rendered
vi.mock('../chat', () => ({
  ChatThread: (props: any) =>
    createElement('div', { 'data-testid': 'chat-thread', 'data-streaming': props.isStreaming }),
  QueryInput: (props: any) =>
    createElement('div', { 'data-testid': 'query-input', 'data-streaming': props.isStreaming }),
}));

vi.mock('../sidebar/SessionSidebar', () => ({
  SessionSidebar: (props: any) =>
    createElement('div', { 'data-testid': 'session-sidebar', 'data-sessions': props.sessions?.length }),
}));

vi.mock('../inspection', () => ({
  InspectionPanel: () => createElement('div', { 'data-testid': 'inspection-panel' }),
}));

vi.mock('../../hooks/useStream', () => ({
  useStream: () => ({
    sendQuery: vi.fn(),
    isStreaming: false,
    cancel: vi.fn(),
  }),
}));

vi.mock('../../lib/api', () => ({
  getSession: vi.fn(),
  deleteSession: vi.fn(),
}));

vi.mock('../../lib/format', () => ({
  generateId: () => 'mock-id',
}));

import { AppShell } from '../AppShell';

function renderWithProvider(ui: React.ReactElement) {
  return render(createElement(AppProvider, null, ui));
}

describe('AppShell', () => {
  it('renders SessionSidebar component', () => {
    renderWithProvider(createElement(AppShell));
    expect(screen.getByTestId('session-sidebar')).toBeTruthy();
  });

  it('renders ChatThread component', () => {
    renderWithProvider(createElement(AppShell));
    expect(screen.getByTestId('chat-thread')).toBeTruthy();
  });

  it('renders QueryInput component', () => {
    renderWithProvider(createElement(AppShell));
    expect(screen.getByTestId('query-input')).toBeTruthy();
  });

  it('does not render InspectionPanel when inspectionOpen is false', () => {
    renderWithProvider(createElement(AppShell));
    expect(screen.queryByTestId('inspection-panel')).toBeNull();
  });

  it('renders header with Ready status when not streaming', () => {
    renderWithProvider(createElement(AppShell));
    expect(screen.getByText('Ready')).toBeTruthy();
  });

  it('passes isStreaming=false to child components by default', () => {
    renderWithProvider(createElement(AppShell));
    expect(screen.getByTestId('chat-thread').getAttribute('data-streaming')).toBe('false');
    expect(screen.getByTestId('query-input').getAttribute('data-streaming')).toBe('false');
  });

  it('renders sidebar toggle button for mobile', () => {
    renderWithProvider(createElement(AppShell));
    expect(screen.getByLabelText('Toggle sidebar')).toBeTruthy();
  });
});
