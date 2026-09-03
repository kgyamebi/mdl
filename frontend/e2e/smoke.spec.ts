import { test, expect, type Page } from '@playwright/test';

async function loginAsOwner(page: Page) {
  await page.goto('/login');
  await page.getByLabel('Email or username').fill('owner@mdl.local');
  await page.getByLabel('Password').fill('Owner@123!');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 20000 });
}

async function selectOptionByText(page: Page, selector: string, text: string) {
  const select = page.locator(selector);
  const value = await select.locator('option').filter({ hasText: text }).first().getAttribute('value');
  if (!value) {
    throw new Error(`No option matching "${text}" in ${selector}`);
  }
  await select.selectOption(value);
}

test('home page loads and shows sign in', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('img', { name: 'modern DL' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Sign in' })).toBeVisible();
});

test('login page shows demo hint', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();
  await expect(page.getByText('owner@mdl.local')).toBeVisible();
});

test('owner can open core pages from sidebar', async ({ page }) => {
  await loginAsOwner(page);

  const pages = [
    { link: 'Inventory', heading: 'Inventory balances' },
    { link: 'Products', heading: 'Products' },
    { link: 'Sales', heading: 'Sales' },
    { link: 'Returns', heading: 'Returns' },
    { link: 'Transfers', heading: 'Stock transfers' },
    { link: 'Reports', heading: 'Export downloads' },
    { link: 'Copilot', heading: 'MDL Copilot' },
  ];

  for (const entry of pages) {
    await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', { name: entry.link }).click();
    await expect(page.getByRole('heading', { name: entry.heading })).toBeVisible({ timeout: 10000 });
  }
});

test('mobile bottom nav opens sales page', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await loginAsOwner(page);
  await page.getByRole('navigation', { name: 'Mobile navigation' }).getByRole('link', { name: 'Sales' }).click();
  await expect(page).toHaveURL(/\/sales$/);
  await expect(page.getByRole('heading', { name: 'Sales' })).toBeVisible();
});

test('copilot floating button opens assistant', async ({ page }) => {
  await loginAsOwner(page);
  await page.goto('/sales');

  const fab = page.getByRole('button', { name: /Ask MDL AI Assistant/i });
  await expect(fab).toBeVisible();
  await fab.click({ force: true });

  await expect(page).toHaveURL(/\/copilot$/);
  await expect(page.getByRole('heading', { name: 'MDL Copilot' })).toBeVisible();
});

test('sales POS completes shop B sale', async ({ page }) => {
  await loginAsOwner(page);
  await page.goto('/sales');

  await page.getByRole('button', { name: 'New sale' }).click();
  await expect(page.getByRole('heading', { name: 'New sale', level: 2 })).toBeVisible();

  await selectOptionByText(page, 'select.input >> nth=0', 'SHOP-B');
  await expect(page.getByText(/Stock checked at/i)).toBeVisible({ timeout: 10000 });

  await selectOptionByText(page, '.pos-add-product select.input', 'MDL-LED-002');
  await page.getByLabel(/Quantity to sell/i).fill('1');
  await page.getByRole('button', { name: 'Add to sale' }).click();

  await expect(page.locator('tbody td[data-label="Product"]').filter({ hasText: 'MDL-LED-002' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Complete sale' })).toBeEnabled({ timeout: 10000 });

  const saleResponse = page.waitForResponse(
    (response) => response.url().includes('/api/sales') && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Complete sale' }).scrollIntoViewIfNeeded();
  await page.getByRole('button', { name: 'Complete sale' }).click({ force: true });
  const response = await saleResponse;
  if (response.status() !== 201) {
    const body = await response.text();
    throw new Error(`Sale API returned ${response.status()}: ${body}`);
  }

  await expect(page.getByRole('heading', { name: 'New sale', level: 2 })).not.toBeVisible({ timeout: 15000 });
  await expect(page.locator('.form__error').filter({ hasText: /Insufficient stock|Failed to complete/i })).toHaveCount(0);
});

test('sales POS toggles open and close', async ({ page }) => {
  await loginAsOwner(page);
  await page.goto('/sales');

  const toggle = page.getByRole('button', { name: 'New sale' });
  await toggle.click();
  await expect(page.getByRole('heading', { name: 'New sale', level: 2 })).toBeVisible();

  await page.getByRole('button', { name: 'Close POS' }).click();
  await expect(page.getByRole('heading', { name: 'New sale', level: 2 })).not.toBeVisible();
});

test('sales refresh reloads list', async ({ page }) => {
  await loginAsOwner(page);
  await page.goto('/sales');
  await page.getByRole('button', { name: 'Refresh' }).click();
  await expect(page.getByRole('heading', { name: 'Sales' })).toBeVisible();
  await expect(page.locator('.form__error').filter({ hasText: /Failed to load/i })).toHaveCount(0);
});
