import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StepIndicator } from './StepIndicator';
import type { StepInfo } from '../../lib/types';

function makeSteps(statuses: StepInfo['status'][], details?: (string | undefined)[]): StepInfo[] {
  const names: StepInfo['step'][] = ['rag', 'sql', 'execute', 'format'];
  return names.map((step, i) => ({
    step,
    status: statuses[i] ?? 'pending',
    detail: details?.[i],
  }));
}

describe('StepIndicator', () => {
  it('renders all four step labels', () => {
    const steps = makeSteps(['pending', 'pending', 'pending', 'pending']);
    render(<StepIndicator steps={steps} />);

    expect(screen.getByText('RAG')).toBeDefined();
    expect(screen.getByText('SQL')).toBeDefined();
    expect(screen.getByText('Execute')).toBeDefined();
    expect(screen.getByText('Format')).toBeDefined();
  });

  it('renders detail text for active step', () => {
    const steps = makeSteps(
      ['done', 'active', 'pending', 'pending'],
      [undefined, 'Generating query...', undefined, undefined]
    );
    render(<StepIndicator steps={steps} />);

    expect(screen.getByText('Generating query...')).toBeDefined();
  });

  it('does not render detail for non-active steps', () => {
    const steps = makeSteps(
      ['done', 'done', 'pending', 'pending'],
      ['RAG done', 'SQL done', undefined, undefined]
    );
    render(<StepIndicator steps={steps} />);

    expect(screen.queryByText('RAG done')).toBeNull();
    expect(screen.queryByText('SQL done')).toBeNull();
  });

  it('shows green check for done steps', () => {
    const steps = makeSteps(['done', 'done', 'pending', 'pending']);
    const { container } = render(<StepIndicator steps={steps} />);

    // Done circles have bg-green-600 class
    const greenCircles = container.querySelectorAll('.bg-green-600');
    expect(greenCircles.length).toBe(2);
  });

  it('shows pulsing circle for active step', () => {
    const steps = makeSteps(['done', 'active', 'pending', 'pending']);
    const { container } = render(<StepIndicator steps={steps} />);

    const pulsingCircle = container.querySelector('.animate-pulse');
    expect(pulsingCircle).not.toBeNull();
  });

  it('shows gray circle for pending steps', () => {
    const steps = makeSteps(['pending', 'pending', 'pending', 'pending']);
    const { container } = render(<StepIndicator steps={steps} />);

    const grayCircles = container.querySelectorAll('.bg-stone-300');
    expect(grayCircles.length).toBe(4);
  });

  it('renders connector lines between steps', () => {
    const steps = makeSteps(['done', 'done', 'active', 'pending']);
    const { container } = render(<StepIndicator steps={steps} />);

    // 3 connectors between 4 steps
    const greenLines = container.querySelectorAll('.bg-green-600.h-0\\.5');
    const grayLines = container.querySelectorAll('.bg-stone-300.h-0\\.5');
    expect(greenLines.length + grayLines.length).toBe(3);
  });

  it('renders all steps as done when complete', () => {
    const steps = makeSteps(['done', 'done', 'done', 'done']);
    const { container } = render(<StepIndicator steps={steps} />);

    const greenCircles = container.querySelectorAll('.bg-green-600');
    expect(greenCircles.length).toBe(4);

    const pulsingCircle = container.querySelector('.animate-pulse');
    expect(pulsingCircle).toBeNull();
  });
});
