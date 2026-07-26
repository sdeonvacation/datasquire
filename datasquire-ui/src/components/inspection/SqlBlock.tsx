import { useState, useCallback } from 'react';
import { Clipboard, Check } from 'lucide-react';

interface SqlBlockProps {
  sql: string;
}

const SQL_KEYWORDS = [
  'SELECT', 'FROM', 'WHERE', 'JOIN', 'LEFT', 'RIGHT', 'INNER',
  'GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'AS', 'ON', 'AND',
  'OR', 'NOT', 'IN', 'BETWEEN', 'LIKE', 'COUNT', 'SUM', 'AVG',
  'MAX', 'MIN', 'DISTINCT', 'WITH', 'CASE', 'WHEN', 'THEN',
  'ELSE', 'END', 'NULL', 'IS', 'DESC', 'ASC', 'UNION',
];

/**
 * Tokenizes SQL and returns spans with syntax highlighting classes.
 * Keywords = blue, strings = green, numbers = orange, rest = default.
 */
export function highlightSql(sql: string): { text: string; className: string }[] {
  const keywordPattern = SQL_KEYWORDS
    .sort((a, b) => b.length - a.length)
    .map((k) => k.replace(/\s+/g, '\\s+'))
    .join('|');

  const regex = new RegExp(
    `('(?:[^'\\\\]|\\\\.)*'|"(?:[^"\\\\]|\\\\.)*")|` + // strings
    `(\\b(?:${keywordPattern})\\b)|` +                   // keywords
    `(\\b\\d+(?:\\.\\d+)?\\b)`,                          // numbers
    'gi'
  );

  const tokens: { text: string; className: string }[] = [];
  let lastIndex = 0;

  for (const match of sql.matchAll(regex)) {
    if (match.index > lastIndex) {
      tokens.push({ text: sql.slice(lastIndex, match.index), className: 'text-stone-300' });
    }

    if (match[1]) {
      tokens.push({ text: match[0], className: 'text-green-400' });
    } else if (match[2]) {
      tokens.push({ text: match[0], className: 'text-blue-400 font-semibold' });
    } else if (match[3]) {
      tokens.push({ text: match[0], className: 'text-orange-400' });
    }

    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < sql.length) {
    tokens.push({ text: sql.slice(lastIndex), className: 'text-stone-300' });
  }

  return tokens;
}

export function SqlBlock({ sql }: SqlBlockProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(async () => {
    await navigator.clipboard.writeText(sql);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [sql]);

  const tokens = highlightSql(sql);

  return (
    <div className="relative group rounded-lg bg-stone-900 p-4 overflow-x-auto">
      <button
        type="button"
        onClick={handleCopy}
        className="absolute top-2 right-2 p-1.5 rounded bg-stone-700 hover:bg-stone-600 text-stone-300 opacity-0 group-hover:opacity-100 transition-opacity"
        aria-label={copied ? 'Copied' : 'Copy SQL'}
      >
        {copied ? <Check size={14} className="text-green-400" /> : <Clipboard size={14} />}
      </button>

      {copied && (
        <span className="absolute top-2 right-12 text-xs text-green-400 font-medium">
          Copied!
        </span>
      )}

      <pre className="font-mono text-sm leading-relaxed whitespace-pre-wrap">
        <code>
          {tokens.map((token, i) => (
            <span key={i} className={token.className}>{token.text}</span>
          ))}
        </code>
      </pre>
    </div>
  );
}
