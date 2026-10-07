// Runs only for /api/* (see run_worker_first in wrangler.jsonc); everything else is served as static assets.
// Forwarding the API through the site's own domain keeps cookies and CSRF same-origin.
export default {
  async fetch(request, env) {
    if (!env.API_ORIGIN || !env.PROXY_SHARED_SECRET) {
      return new Response('API não configurada.', { status: 503 });
    }
    const incoming = new URL(request.url);
    const target = new URL(incoming.pathname + incoming.search, env.API_ORIGIN);

    const headers = new Headers(request.headers);
    headers.set('X-Client-IP', request.headers.get('CF-Connecting-IP') ?? '');
    headers.set('X-Proxy-Secret', env.PROXY_SHARED_SECRET);

    return fetch(
      new Request(target, {
        method: request.method,
        headers,
        body: request.body,
        redirect: 'manual',
      }),
    );
  },
};
