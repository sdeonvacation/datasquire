import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import type { ReactNode } from 'react';
import type { AppState, InspectionData, StepInfo } from '../../lib/types';

// Mock the context hook
const mockDispatch = vi.fn();
let mockState: AppState;

vi.mock('../../context/AppContext', () => ({
  useApp: () => ({ state: mockState, dispatch: mockDispatch }),
}));

// Must import after mock setup
const { InspectionPanel } = await import('./InspectionPanel');

function buildSteps(activeStep?: StepInfo['step']): StepInfo[] {
  const allSteps: StepInfo['step'][] = ['rag', 'sql', 'execute', 'format'];
  const activeIdx = activeStep ? allSteps.indexOf(activeStep) : -1;

  return allSteps.map((step, i) => ({
    step,
    status: i < activeIdx ? 'done' : i === activeIdx ? 'active' : 'pending',
    detail: i === activeIdx ? `Processing ${step}` : undefined,
  }));
}

function buildInspection(overrides: Partial<InspectionData> = {}): InspectionData {
  return {
    chunks: ['users table: id, name, email', 'orders table: id, user_id, amount'],
    sql: 'SELECT * FROM users',
    rawResult: '| id | name |\n| 1 | Alice |',
    steps: buildSteps('sql'),
    latencyMs: 432,
    agentsUsed: ['schema-agent', 'sql-agent'],
    iterations: 2,
    ...overrides,
  };
}

const baseState: AppState = {
  sessions: [],
  activeSessionId: null,
  messages: [],
  isStreaming: false,
  currentInspection: null,
  sidebarOpen: true,
  inspectionOpen: true,
  theme: 'light',
};

beforeEach(() => {
  mockDispatch.mockClear();
  mockState = { ...baseState };
});

describe('InspectionPanel', () => {
  it('renders empty state when no inspection data', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: null };
    render(<InspectionPanel />);

    expect(screen.getByText(/submit a query/i)).toBeDefined();
  });

  it('renders panel title and close button', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: buildInspection() };
    render(<InspectionPanel />);

    expect(screen.getByText('Query Inspection')).toBeDefined();
    expect(screen.getByLabelText('Close inspection panel')).toBeDefined();
  });

  it('dispatches TOGGLE_INSPECTION on close click', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: buildInspection() };
    render(<InspectionPanel />);

    fireEvent.click(screen.getByLabelText('Close inspection panel'));
    expect(mockDispatch).toHaveBeenCalledWith({ type: 'TOGGLE_INSPECTION' });
  });

  it('applies translate-x-full class when closed', () => {
    mockState = { ...baseState, inspectionOpen: false, currentInspection: null };
    const { container } = render(<InspectionPanel />);
    const aside = container.querySelector('aside');

    expect(aside?.className).toContain('translate-x-full');
  });

  it('does not apply translate-x-full when open', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: buildInspection() };
    const { container } = render(<InspectionPanel />);
    const aside = container.querySelector('aside');

    expect(aside?.className).not.toContain('translate-x-full');
  });

  it('renders schema chunks list', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: buildInspection() };
    render(<InspectionPanel />);

    expect(screen.getByText(/users table/)).toBeDefined();
    expect(screen.getByText(/orders table/)).toBeDefined();
  });

  it('renders metadata section with latency and iterations', () => {
    mockState = {
      ...baseState,
      inspectionOpen: true,
      currentInspection: buildInspection({ latencyMs: 750, iterations: 3 }),
    };
    render(<InspectionPanel />);

    // Metadata section is collapsed by default, click to open
    fireEvent.click(screen.getByText('Metadata'));

    expect(screen.getByText('750ms')).toBeDefined();
    expect(screen.getByText('3')).toBeDefined();
  });

  it('renders agents used in metadata', () => {
    mockState = {
      ...baseState,
      inspectionOpen: true,
      currentInspection: buildInspection({ agentsUsed: ['agent-a', 'agent-b'] }),
    };
    render(<InspectionPanel />);

    fireEvent.click(screen.getByText('Metadata'));
    expect(screen.getByText('agent-a, agent-b')).toBeDefined();
  });

  it('renders raw result content', () => {
    mockState = {
      ...baseState,
      inspectionOpen: true,
      currentInspection: buildInspection({ rawResult: 'row1\nrow2' }),
    };
    render(<InspectionPanel />);

    expect(screen.getByText(/row1/)).toBeDefined();
  });

  it('shows "No chunks retrieved" when chunks are empty', () => {
    mockState = {
      ...baseState,
      inspectionOpen: true,
      currentInspection: buildInspection({ chunks: [] }),
    };
    render(<InspectionPanel />);

    expect(screen.getByText('No chunks retrieved')).toBeDefined();
  });

  it('section toggle buttons have aria-expanded attribute', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: buildInspection() };
    render(<InspectionPanel />);

    const stepProgressBtn = screen.getByText('Step Progress').closest('button');
    expect(stepProgressBtn?.getAttribute('aria-expanded')).toBe('true');

    const metadataBtn = screen.getByText('Metadata').closest('button');
    expect(metadataBtn?.getAttribute('aria-expanded')).toBe('false');
  });

  it('section toggle buttons have aria-controls linking to region', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: buildInspection() };
    render(<InspectionPanel />);

    const stepProgressBtn = screen.getByText('Step Progress').closest('button');
    const controlsId = stepProgressBtn?.getAttribute('aria-controls');
    expect(controlsId).toBe('inspection-section-step-progress');
  });

  it('expanded section content has role="region"', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: buildInspection() };
    const { container } = render(<InspectionPanel />);

    const region = container.querySelector('#inspection-section-step-progress');
    expect(region?.getAttribute('role')).toBe('region');
  });

  it('aria-expanded toggles when section is collapsed', () => {
    mockState = { ...baseState, inspectionOpen: true, currentInspection: buildInspection() };
    render(<InspectionPanel />);

    const btn = screen.getByText('Step Progress').closest('button')!;
    expect(btn.getAttribute('aria-expanded')).toBe('true');
    fireEvent.click(btn);
    expect(btn.getAttribute('aria-expanded')).toBe('false');
  });
});
