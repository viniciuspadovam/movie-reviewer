import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { apiInterceptor } from './api-interceptor';
import { apiUrl } from './api-url';

describe('apiInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    TestBed.inject(DOCUMENT).cookie = 'XSRF-TOKEN=abc123';
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('sends credentials and the XSRF header on API writes', () => {
    http.post(apiUrl('/admin/titles'), {}).subscribe();

    const request = controller.expectOne(apiUrl('/admin/titles'));
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('abc123');
    request.flush({});
  });

  it('does not add the XSRF header to reads', () => {
    http.get(apiUrl('/genres')).subscribe();

    const request = controller.expectOne(apiUrl('/genres'));
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush([]);
  });

  it('leaves third-party requests untouched', () => {
    http.post('https://example.org/x', {}).subscribe();

    const request = controller.expectOne('https://example.org/x');
    expect(request.request.withCredentials).toBe(false);
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush({});
  });

  it('redirects to login when an admin call returns 401', async () => {
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'url', 'get').mockReturnValue('/admin/reviews');
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    http.get(apiUrl('/admin/reviews')).subscribe({ error: () => undefined });
    controller
      .expectOne(apiUrl('/admin/reviews'))
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { redirect: '/admin/reviews' },
    });
  });
});
