import { defineConfig, devices } from '@playwright/test';

const isWindows = process.platform === 'win32';
const mavenWrapper = isWindows ? '.\\mvnw.cmd' : './mvnw';
const tmdbStubUrl = 'http://localhost:8089';

// Needs the local Postgres from infra/docker-compose.dev.yml. Ports 4200 and 8080 must be free:
// the backend has to start pointing at the TMDB stub.
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? 'github' : 'list',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'retain-on-failure',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'node e2e/tmdb-stub.mjs',
      url: `${tmdbStubUrl}/search/tv`,
      reuseExistingServer: false,
    },
    {
      command: `${mavenWrapper} -q spring-boot:run -Dspring-boot.run.profiles=dev "-Dspring-boot.run.arguments=--app.tmdb.base-url=${tmdbStubUrl} --app.tmdb.access-token=e2e"`,
      cwd: '../backend',
      url: 'http://localhost:8080/actuator/health',
      timeout: 240_000,
      reuseExistingServer: false,
    },
    {
      command: 'npx ng serve --port 4200',
      url: 'http://localhost:4200',
      timeout: 180_000,
      reuseExistingServer: !process.env['CI'],
    },
  ],
});
