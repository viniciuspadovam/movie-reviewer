// Minimal TMDB stand-in for E2E runs: the backend calls TMDB server-side, so the browser cannot mock it.
import { createServer } from 'node:http';

const port = Number(process.env.TMDB_STUB_PORT ?? 8089);
const runId = Date.now() % 1_000_000_000;
const movie = {
  id: runId,
  title: `Filme E2E ${runId}`,
  original_title: `E2E Movie ${runId}`,
  release_date: '1999-10-15',
  overview: 'Obra criada pelo stub do TMDB para os testes de ponta a ponta.',
  poster_path: null,
  backdrop_path: null,
  runtime: 120,
  genres: [{ id: 18, name: 'Drama' }],
};

createServer((request, response) => {
  const url = new URL(request.url ?? '/', `http://localhost:${port}`);
  response.setHeader('Content-Type', 'application/json');
  if (url.pathname === '/search/movie') {
    response.end(JSON.stringify({ results: [movie] }));
  } else if (url.pathname === '/search/tv') {
    response.end(JSON.stringify({ results: [] }));
  } else if (url.pathname === `/movie/${runId}`) {
    response.end(JSON.stringify(movie));
  } else {
    response.statusCode = 404;
    response.end(JSON.stringify({ status_message: 'not found' }));
  }
}).listen(port, () => console.log(`TMDB stub on ${port} serving "${movie.title}"`));
