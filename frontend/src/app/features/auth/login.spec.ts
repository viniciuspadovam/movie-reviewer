import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Auth } from '../../core/auth';
import { Login } from './login';

describe('Login', () => {
  let navigateByUrl: ReturnType<typeof vi.fn>;

  async function submit(redirect: string | undefined): Promise<void> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: Auth,
          useValue: { ensureLoaded: async () => false, login: async () => ({ username: 'admin' }) },
        },
      ],
    });
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true) as never;
    const fixture = TestBed.createComponent(Login);
    if (redirect !== undefined) {
      fixture.componentRef.setInput('redirect', redirect);
    }
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLInputElement>('#username')!.value = 'admin';
    element.querySelector<HTMLInputElement>('#password')!.value = 'admin';
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('returns to the page that required login', async () => {
    await submit('/admin/new');

    expect(navigateByUrl).toHaveBeenCalledWith('/admin/new');
  });

  it('ignores redirects to other sites', async () => {
    await submit('//evil.example.com');

    expect(navigateByUrl).toHaveBeenCalledWith('/admin');
  });
});
