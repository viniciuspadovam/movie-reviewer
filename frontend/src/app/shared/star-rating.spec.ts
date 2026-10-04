import { TestBed } from '@angular/core/testing';

import { StarRating } from './star-rating';

describe('StarRating', () => {
  async function render(rating: number): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(StarRating);
    fixture.componentRef.setInput('rating', rating);
    fixture.componentRef.setInput('showNumber', true);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders full and half stars for odd ratings', async () => {
    const element = await render(7);

    expect(element.querySelectorAll('svg.full').length).toBe(3);
    expect(element.querySelectorAll('svg.half').length).toBe(1);
    expect(element.querySelectorAll('svg.empty').length).toBe(1);
  });

  it('exposes the rating to screen readers', async () => {
    const element = await render(9);

    expect(element.querySelector('[role="img"]')?.getAttribute('aria-label')).toBe('Nota 4,5 de 5');
    expect(element.textContent).toContain('4,5');
  });
});
