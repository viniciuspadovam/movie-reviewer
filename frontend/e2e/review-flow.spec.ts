import { Page, expect, test } from '@playwright/test';

async function searchStubTitle(page: Page): Promise<void> {
  await page.goto('/admin');
  await page.getByRole('link', { name: 'Nova review' }).click();
  await page.getByLabel('Nome da obra').fill('e2e');
  await page.getByRole('button', { name: 'Buscar no TMDB' }).click();
}

async function startNewSession(
  page: Page,
  watchedOn: string,
  halfStars: number,
  text: string,
): Promise<void> {
  await searchStubTitle(page);
  await page.getByRole('button', { name: 'Escrever review' }).first().click();
  await expect(page.getByText('Nova sessão de')).toBeVisible();
  await fillEditor(page, watchedOn, halfStars, text);
}

async function publishSession(
  page: Page,
  watchedOn: string,
  halfStars: number,
  text: string,
): Promise<void> {
  await startNewSession(page, watchedOn, halfStars, text);
  await page.getByRole('button', { name: 'Publicar' }).click();
  await expect(page).toHaveURL(/\/review\/\d+$/);
}

async function fillEditor(
  page: Page,
  watchedOn: string,
  halfStars: number,
  text: string,
): Promise<void> {
  await page.getByLabel('Assistido em').fill(watchedOn);
  await page
    .locator('app-star-input label')
    .nth(halfStars - 1)
    .click();
  await page.getByLabel('Texto (Markdown)').fill(text);
}

test('owner writes and rewatches a title; visitors see only what was published', async ({
  page,
}) => {
  await page.goto('/admin');
  await expect(page).toHaveURL(/\/login/);
  await page.getByLabel('Usuário').fill('admin');
  await page.getByLabel('Senha').fill('admin');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Reviews' })).toBeVisible();

  await searchStubTitle(page);
  const titleName = (await page.locator('.results h2').first().innerText()).replace(
    /\s*\(\d{4}\)$/,
    '',
  );

  await publishSession(page, '2020-01-10', 6, 'Primeira sessão: achei frio.');
  await publishSession(page, '2024-03-05', 9, 'Segunda sessão: virou favorito.');

  await page.goto('/admin');
  await page
    .locator('.row')
    .filter({ hasText: titleName })
    .filter({ hasText: '10 de jan. de 2020' })
    .getByRole('link', { name: 'Editar' })
    .click();
  await page
    .getByLabel('Texto (Markdown)')
    .fill('Primeira sessão: achei frio, mas a trilha ficou.');
  await page.getByRole('button', { name: 'Salvar alterações' }).click();
  await expect(page.getByText(/Editada em/)).toBeVisible();

  await startNewSession(page, '2025-01-01', 2, 'Rascunho que ninguém deve ver.');
  await page.getByRole('button', { name: 'Salvar rascunho' }).click();
  await expect(page).toHaveURL(/\/admin\?status=DRAFT/);

  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page.getByRole('link', { name: 'Painel' })).toHaveCount(0);

  await page.goto(`/search?q=${encodeURIComponent(titleName)}&minRating=9`);
  await page.getByRole('link', { name: new RegExp(titleName) }).click();
  await expect(page.getByRole('heading', { name: '2 sessões' })).toBeVisible();
  await expect(page.getByText('+1,5')).toBeVisible();
  await expect(page.getByText('achei frio, mas a trilha ficou')).toBeVisible();
  await expect(page.getByText('Rascunho que ninguém deve ver')).toHaveCount(0);

  await page.goto(`/search?q=${encodeURIComponent(titleName)}&maxRating=4`);
  await expect(page.getByText('Nada encontrado com esses filtros.')).toBeVisible();
});
