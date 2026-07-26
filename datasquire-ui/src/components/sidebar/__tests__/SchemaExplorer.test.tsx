import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SchemaExplorer } from '../SchemaExplorer';

describe('SchemaExplorer', () => {
  const onTableClick = vi.fn();

  it('renders all schema tables', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    expect(screen.getByText('business_units')).toBeTruthy();
    expect(screen.getByText('chart_of_accounts')).toBeTruthy();
    expect(screen.getByText('journal_entries')).toBeTruthy();
    expect(screen.getByText('journal_lines')).toBeTruthy();
    expect(screen.getByText('budgets')).toBeTruthy();
    expect(screen.getByText('invoices')).toBeTruthy();
    expect(screen.getByText('cash_flow')).toBeTruthy();
  });

  it('shows Finance Schema header', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    expect(screen.getByText('Finance Schema')).toBeTruthy();
  });

  it('columns are hidden by default', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    // Columns exist in DOM but inside collapsed container (max-h-0 opacity-0)
    const nameCol = screen.queryByText('name');
    if (nameCol) {
      const container = nameCol.closest('[class*="max-h-0"]');
      expect(container).toBeTruthy();
    }
  });

  it('expands table on click to show columns', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    fireEvent.click(screen.getByText('business_units'));
    // After expand, columns should be in visible container
    const regionCol = screen.getByText('region');
    const container = regionCol.closest('[class*="max-h-96"]');
    expect(container).toBeTruthy();
  });

  it('collapses table on second click', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    const tableBtn = screen.getByText('business_units');
    fireEvent.click(tableBtn);
    fireEvent.click(tableBtn);
    const nameCol = screen.getByText('region');
    const container = nameCol.closest('[class*="max-h-0"]');
    expect(container).toBeTruthy();
  });

  it('shows type badges with correct styling', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    fireEvent.click(screen.getByText('invoices'));
    const badges = screen.getAllByText('text');
    expect(badges.length).toBeGreaterThan(0);
    expect(badges[0].className).toContain('text-blue-300');
  });

  it('shows numeric type badge', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    fireEvent.click(screen.getByText('invoices'));
    const numericBadges = screen.getAllByText('numeric');
    expect(numericBadges.length).toBeGreaterThan(0);
    expect(numericBadges[0].className).toContain('text-emerald-300');
  });

  it('shows date type badge', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    fireEvent.click(screen.getByText('invoices'));
    const dateBadge = screen.getByText('date');
    expect(dateBadge.className).toContain('text-amber-300');
  });

  it('dispatches query on double-click', () => {
    const handler = vi.fn();
    render(<SchemaExplorer onTableClick={handler} />);
    fireEvent.doubleClick(screen.getByText('invoices'));
    expect(handler).toHaveBeenCalledWith('Tell me about the invoices table');
  });

  it('dispatches query on Enter key press', () => {
    const handler = vi.fn();
    render(<SchemaExplorer onTableClick={handler} />);
    const tableBtn = screen.getByLabelText('Expand invoices');
    fireEvent.keyDown(tableBtn, { key: 'Enter' });
    expect(handler).toHaveBeenCalledWith('Tell me about the invoices table');
  });

  it('renders ask button for each table', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    const askBtns = screen.getAllByLabelText(/^Ask about /);
    expect(askBtns.length).toBe(7); // 7 tables in schema
  });

  it('dispatches query on ask button click', () => {
    const handler = vi.fn();
    render(<SchemaExplorer onTableClick={handler} />);
    fireEvent.click(screen.getByLabelText('Ask about budgets'));
    expect(handler).toHaveBeenCalledWith('Tell me about the budgets table');
  });

  it('has aria-expanded on table items', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    const item = screen.getByText('cash_flow').closest('[role="treeitem"]');
    expect(item?.getAttribute('aria-expanded')).toBe('false');
    fireEvent.click(screen.getByText('cash_flow'));
    expect(item?.getAttribute('aria-expanded')).toBe('true');
  });

  it('rotates chevron icon when expanded', () => {
    render(<SchemaExplorer onTableClick={onTableClick} />);
    fireEvent.click(screen.getByText('cash_flow'));
    // The chevron is the first child SVG in the button
    const btn = screen.getByText('cash_flow').closest('button')!;
    const chevron = btn.querySelector('svg');
    expect(chevron?.className.baseVal || chevron?.getAttribute('class')).toContain('rotate-90');
  });
});
