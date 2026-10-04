import { Component, input } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { SpoilerGuard } from './spoiler-guard';

@Component({
  imports: [SpoilerGuard],
  template: `<app-spoiler-guard [hasSpoilers]="hasSpoilers()"><p class="secret">O final</p></app-spoiler-guard>`,
})
class Host {
  readonly hasSpoilers = input(true);
}

describe('SpoilerGuard', () => {
  it('hides spoilers until the reader asks', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.secret')).toBeNull();

    element.querySelector('button')!.click();
    await fixture.whenStable();

    expect(element.querySelector('.secret')?.textContent).toBe('O final');
  });

  it('shows content without spoilers right away', async () => {
    const fixture = TestBed.createComponent(Host);
    fixture.componentRef.setInput('hasSpoilers', false);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.secret')).not.toBeNull();
  });
});
