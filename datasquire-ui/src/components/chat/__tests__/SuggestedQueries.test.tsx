import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SuggestedQueries } from '../SuggestedQueries';

describe('SuggestedQueries', () => {
  it('renders header and subtitle', () => {
    render(<SuggestedQueries onSelect={vi.fn()} />);
    expect(screen.getByText('Finance Controller Dashboard')).toBeTruthy();
    expect(screen.getByText('Ask questions about your financial data')).toBeTruthy();
  });

  it('renders all 4 suggestion cards', () => {
    render(<SuggestedQueries onSelect={vi.fn()} />);
    expect(screen.getByText("What's our total revenue YTD by business unit?")).toBeTruthy();
    expect(screen.getByText('Which departments are over budget for Q3?')).toBeTruthy();
    expect(screen.getByText('Show me all overdue invoices over $10,000')).toBeTruthy();
    expect(screen.getByText("What's our operating cash flow trend?")).toBeTruthy();
  });

  it('calls onSelect with query text when card clicked', () => {
    const onSelect = vi.fn();
    render(<SuggestedQueries onSelect={onSelect} />);
    fireEvent.click(screen.getByText('Which departments are over budget for Q3?'));
    expect(onSelect).toHaveBeenCalledWith('Which departments are over budget for Q3?');
  });

  it('each card is a button element', () => {
    render(<SuggestedQueries onSelect={vi.fn()} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(4);
  });
});
