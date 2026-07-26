import { describe, it, expect } from 'vitest';
import { highlightSql } from './SqlBlock';

describe('highlightSql', () => {
  it('highlights SQL keywords as blue', () => {
    const tokens = highlightSql('SELECT id FROM users');
    const selectToken = tokens.find((t) => t.text === 'SELECT');
    const fromToken = tokens.find((t) => t.text === 'FROM');

    expect(selectToken?.className).toContain('text-blue-400');
    expect(fromToken?.className).toContain('text-blue-400');
  });

  it('highlights string literals as green', () => {
    const tokens = highlightSql("WHERE name = 'Alice'");
    const stringToken = tokens.find((t) => t.text === "'Alice'");

    expect(stringToken?.className).toContain('text-green-400');
  });

  it('highlights numbers as orange', () => {
    const tokens = highlightSql('LIMIT 100');
    const numToken = tokens.find((t) => t.text === '100');

    expect(numToken?.className).toContain('text-orange-400');
  });

  it('handles decimal numbers', () => {
    const tokens = highlightSql('WHERE price > 9.99');
    const numToken = tokens.find((t) => t.text === '9.99');

    expect(numToken?.className).toContain('text-orange-400');
  });

  it('preserves non-keyword text with default color', () => {
    const tokens = highlightSql('SELECT username');
    const identToken = tokens.find((t) => t.text.includes('username'));

    expect(identToken?.className).toContain('text-stone-300');
  });

  it('handles multi-word keywords like GROUP BY', () => {
    const tokens = highlightSql('GROUP BY department');
    const gbToken = tokens.find((t) => /GROUP\s+BY/i.test(t.text));

    expect(gbToken?.className).toContain('text-blue-400');
  });

  it('is case-insensitive for keywords', () => {
    const tokens = highlightSql('select id from users');
    const selectToken = tokens.find((t) => t.text === 'select');

    expect(selectToken?.className).toContain('text-blue-400');
  });

  it('handles escaped quotes in strings', () => {
    const tokens = highlightSql("WHERE name = 'O\\'Brien'");
    const stringToken = tokens.find((t) => t.className.includes('text-green-400'));

    expect(stringToken).toBeDefined();
    expect(stringToken?.text).toContain('O');
  });

  it('returns empty array for empty string', () => {
    const tokens = highlightSql('');
    expect(tokens).toEqual([]);
  });

  it('handles double-quoted strings', () => {
    const tokens = highlightSql('SELECT "column name" FROM t');
    const stringToken = tokens.find((t) => t.text === '"column name"');

    expect(stringToken?.className).toContain('text-green-400');
  });

  it('handles complex query with multiple token types', () => {
    const sql = "SELECT COUNT(*) AS total FROM orders WHERE amount > 500 AND status = 'active'";
    const tokens = highlightSql(sql);

    expect(tokens.length).toBeGreaterThan(5);

    const countToken = tokens.find((t) => t.text === 'COUNT');
    expect(countToken?.className).toContain('text-blue-400');

    const numToken = tokens.find((t) => t.text === '500');
    expect(numToken?.className).toContain('text-orange-400');

    const strToken = tokens.find((t) => t.text === "'active'");
    expect(strToken?.className).toContain('text-green-400');
  });
});
