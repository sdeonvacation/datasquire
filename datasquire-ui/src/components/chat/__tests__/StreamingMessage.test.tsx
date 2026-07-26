import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StreamingMessage } from '../StreamingMessage';

describe('StreamingMessage', () => {
  it('shows "Thinking..." when content is empty', () => {
    render(<StreamingMessage content="" />);
    expect(screen.getByText('Thinking...')).toBeTruthy();
  });

  it('renders partial markdown content', () => {
    render(<StreamingMessage content="**Bold** text" />);
    const bold = screen.getByText('Bold');
    expect(bold.tagName).toBe('STRONG');
  });

  it('renders blinking cursor element', () => {
    const { container } = render(<StreamingMessage content="test" />);
    const cursor = container.querySelector('[class*="animate-"]');
    expect(cursor).toBeTruthy();
  });

  it('applies fade-in animation class', () => {
    const { container } = render(<StreamingMessage content="test" />);
    const wrapper = container.firstElementChild;
    expect(wrapper?.className).toContain('animate-');
  });
});
