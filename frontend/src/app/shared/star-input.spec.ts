import { TestBed } from '@angular/core/testing';

import { StarInput } from './star-input';

describe('StarInput', () => {
  it('sets the rating in half stars from the radio group', async () => {
    const fixture = TestBed.createComponent(StarInput);
    await fixture.whenStable();
    const radios = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLInputElement>('input[type=radio]');

    expect(radios.length).toBe(10);
    radios[6].click();
    await fixture.whenStable();

    expect(fixture.componentInstance.rating()).toBe(7);
    expect((fixture.nativeElement as HTMLElement).querySelector('.value')?.textContent).toBe('3,5');
  });
});
