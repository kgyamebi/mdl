import { test, expect, type Page } from '@playwright/test';
import { DEMO_USERS, loginAs, openPageFromNav } from './helpers';

const BALANCES_URL = '/api/inventory/balances';

async function readRowCount(page: Page): Promise<number> {
  const text = (await page.locator('.subtitle').first().textContent()) ?? '';
  const match = text.match(/([\d,]+)\s+balance row/);
  if (!match) {
    throw new Error(`Could not read the balance row count from "${text}"`);
  }
  return Number(match[1].replace(/,/g, ''));
}

/** Waits for the balances request carrying the given query parameters. */
function waitForBalances(page: Page, ...expectedParams: string[]) {
  return page.waitForResponse(
    (response) =>
      response.url().includes(BALANCES_URL) &&
      expectedParams.every((param) => response.url().includes(param)),
    { timeout: 20000 },
  );
}

test.describe('inventory quantity filters', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, DEMO_USERS.owner);
    // Navigate in-app: a full page load drops the in-memory access token and
    // forces a session bootstrap that can outlast the default timeout.
    await openPageFromNav(page, 'Inventory', 'Inventory balances');
    // The subtitle renders "0 balance row(s)" before the first response lands,
    // so wait for a real count rather than just the label.
    await expect.poll(() => readRowCount(page), { timeout: 20000 }).toBeGreaterThan(0);
  });

  test('"at least" filter reaches the API and narrows the results', async ({ page }) => {
    const unfiltered = await readRowCount(page);
    expect(unfiltered).toBeGreaterThan(0);

    await page.getByLabel('Stock level').selectOption('atLeast');

    const responsePromise = waitForBalances(page, 'minQuantity=100');
    await page.getByLabel('Quantity', { exact: true }).fill('100');
    const body = await (await responsePromise).json();

    expect(body.data.totalElements).toBeGreaterThan(0);
    expect(body.data.totalElements).toBeLessThan(unfiltered);
    await expect
      .poll(() => readRowCount(page), { timeout: 15000 })
      .toBe(body.data.totalElements);

    // Every visible row should genuinely hold at least 100.
    const quantities = await page.locator('td[data-label="On hand"]').allTextContents();
    for (const cell of quantities) {
      expect(Number(cell.replace(/[^\d.-]/g, ''))).toBeGreaterThanOrEqual(100);
    }
  });

  test('"at most" filter finds depleted stock', async ({ page }) => {
    await page.getByLabel('Stock level').selectOption('atMost');

    const responsePromise = waitForBalances(page, 'maxQuantity=0');
    await page.getByLabel('Quantity', { exact: true }).fill('0');
    const body = await (await responsePromise).json();

    expect(body.data.totalElements).toBeGreaterThan(0);

    const quantities = await page.locator('td[data-label="On hand"]').allTextContents();
    for (const cell of quantities) {
      expect(Number(cell.replace(/[^\d.-]/g, ''))).toBeLessThanOrEqual(0);
    }
  });

  test('"between" filter applies both bounds', async ({ page }) => {
    await page.getByLabel('Stock level').selectOption('between');

    await page.getByLabel('From (inclusive)').fill('10');
    const responsePromise = waitForBalances(page, 'minQuantity=10', 'maxQuantity=50');
    await page.getByLabel('To (inclusive)').fill('50');
    const body = await (await responsePromise).json();

    expect(body.data.totalElements).toBeGreaterThan(0);

    const quantities = await page.locator('td[data-label="On hand"]').allTextContents();
    expect(quantities.length).toBeGreaterThan(0);
    for (const cell of quantities) {
      const quantity = Number(cell.replace(/[^\d.-]/g, ''));
      expect(quantity).toBeGreaterThanOrEqual(10);
      expect(quantity).toBeLessThanOrEqual(50);
    }
  });

  test('clearing the filter restores the full list', async ({ page }) => {
    const unfiltered = await readRowCount(page);

    await page.getByLabel('Stock level').selectOption('atLeast');
    const filtered = waitForBalances(page, 'minQuantity=100');
    await page.getByLabel('Quantity', { exact: true }).fill('100');
    await filtered;

    await page.getByRole('button', { name: 'Clear' }).click();
    await expect.poll(() => readRowCount(page), { timeout: 15000 }).toBe(unfiltered);
  });
});

test('POS item search finds items by typing instead of scrolling', async ({ page }) => {
  await loginAs(page, DEMO_USERS.owner);
  await openPageFromNav(page, 'Sales', 'Sales');

  await page.getByRole('button', { name: 'New sale' }).click();

  const searchField = page.locator('#pos-product-search');
  await expect(searchField).toBeVisible();

  await searchField.click();
  await searchField.fill('bajaj');

  const options = page.locator('.product-search__option');
  await expect(options.first()).toBeVisible({ timeout: 15000 });
  expect(await options.count()).toBeGreaterThan(1);
  await expect(options.first()).toContainText(/BAJAJ/i);

  // Selecting fills the field and closes the list.
  await options.first().click();
  await expect(options).toHaveCount(0);
  await expect(searchField).toHaveValue(/BAJAJ/i);
});
