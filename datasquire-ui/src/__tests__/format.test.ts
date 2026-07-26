import { describe, expect, it } from 'vitest';
import { formatCurrency, formatDate, generateId } from '../lib/format';

describe('formatCurrency', () => {
  it('formats positive amounts', () => {
    expect(formatCurrency(1234.56)).toBe('$1,234.56');
  });

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });

  it('formats negative amounts', () => {
    expect(formatCurrency(-500)).toBe('-$500.00');
  });

  it('rounds to 2 decimal places', () => {
    expect(formatCurrency(99.999)).toBe('$100.00');
  });
});

describe('formatDate', () => {
  it('returns "just now" for recent timestamps', () => {
    expect(formatDate(new Date())).toBe('just now');
  });

  it('returns minutes ago', () => {
    const fiveMinAgo = new Date(Date.now() - 5 * 60 * 1000);
    expect(formatDate(fiveMinAgo)).toBe('5m ago');
  });

  it('returns hours ago', () => {
    const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000);
    expect(formatDate(twoHoursAgo)).toBe('2h ago');
  });

  it('returns "yesterday" for 1 day ago', () => {
    const yesterday = new Date(Date.now() - 25 * 60 * 60 * 1000);
    expect(formatDate(yesterday)).toBe('yesterday');
  });

  it('returns days ago for 2-6 days', () => {
    const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000);
    expect(formatDate(threeDaysAgo)).toBe('3d ago');
  });

  it('returns formatted date for older', () => {
    const old = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
    const result = formatDate(old);
    expect(result).toMatch(/\w+ \d+/); // e.g. "Jun 21"
  });
});

describe('generateId', () => {
  it('returns a valid UUID', () => {
    const id = generateId();
    expect(id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  });

  it('generates unique values', () => {
    const a = generateId();
    const b = generateId();
    expect(a).not.toBe(b);
  });
});
