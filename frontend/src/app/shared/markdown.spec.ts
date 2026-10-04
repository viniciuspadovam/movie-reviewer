import { TestBed } from '@angular/core/testing';

import { Markdown } from './markdown';

describe('Markdown', () => {
  async function render(source: string): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(Markdown);
    fixture.componentRef.setInput('source', source);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders Markdown formatting', async () => {
    const element = await render('## Título\n\nTexto com **ênfase**.');

    expect(element.querySelector('h2')?.textContent).toBe('Título');
    expect(element.querySelector('strong')?.textContent).toBe('ênfase');
  });

  it('strips scripts, event handlers and javascript links', async () => {
    const element = await render(
      '<script>alert(1)</script><img src="x" onerror="alert(1)"> [clique](javascript:alert(1))',
    );

    expect(element.querySelector('script')).toBeNull();
    expect(element.querySelector('img')?.getAttribute('onerror')).toBeNull();
    expect(element.querySelector('a')?.getAttribute('href') ?? '').not.toContain('javascript:');
  });
});
