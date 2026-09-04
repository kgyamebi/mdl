import { test, expect } from '@playwright/test';
import { DEMO_USERS, expectNavLink, loginAs, openPageFromNav } from './helpers';

test.describe('Owner', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, DEMO_USERS.owner);
  });

  test('sees full navigation and admin tools', async ({ page }) => {
    await expectNavLink(page, 'Inventory', true);
    await expectNavLink(page, 'Sales', true);
    await expectNavLink(page, 'Transfers', true);
    await expectNavLink(page, 'Reports', true);
    await expectNavLink(page, 'Users', true);
    await expectNavLink(page, 'Locations', true);
  });

  test('can open operational pages without errors', async ({ page }) => {
    await openPageFromNav(page, 'Inventory', 'Inventory balances');
    await openPageFromNav(page, 'Sales', 'Sales');
    await openPageFromNav(page, 'Transfers', 'Stock transfers');
    await openPageFromNav(page, 'Reports', 'Export downloads');
  });

  test('can export sales PDF without conflict error', async ({ page }) => {
    await openPageFromNav(page, 'Reports', 'Export downloads');
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Download PDF' }).first().click(),
    ]);
    expect(download.suggestedFilename()).toMatch(/sales-summary.*\.pdf$/i);
    await expect(page.locator('.form__error').filter({ hasText: /data conflict/i })).toHaveCount(0);
  });

  test('can open transfer create form', async ({ page }) => {
    await openPageFromNav(page, 'Transfers', 'Stock transfers');
    await page.getByRole('button', { name: 'New request' }).click();
    await expect(page.getByRole('heading', { name: 'New stock transfer', level: 2 })).toBeVisible();
    await expect(page.locator('.form__error').filter({ hasText: /Failed to load/i })).toHaveCount(0);
    await expect(page.locator('select.input').first().locator('option')).not.toHaveCount(1, { timeout: 15000 });
  });
});

test.describe('Shop manager', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, DEMO_USERS.manager);
  });

  test('sees shop management nav but not user admin', async ({ page }) => {
    await expectNavLink(page, 'Inventory', true);
    await expectNavLink(page, 'Sales', true);
    await expectNavLink(page, 'Transfers', true);
    await expectNavLink(page, 'Reports', true);
    await expectNavLink(page, 'Users', false);
  });

  test('can open transfers and load create form', async ({ page }) => {
    await openPageFromNav(page, 'Transfers', 'Stock transfers');
    await page.getByRole('button', { name: 'New request' }).click();
    await expect(page.getByRole('heading', { name: 'New stock transfer', level: 2 })).toBeVisible();
    await expect(page.locator('.form__error').filter({ hasText: /Failed to load/i })).toHaveCount(0);
  });

  test('can export report CSV', async ({ page }) => {
    await openPageFromNav(page, 'Reports', 'Export downloads');
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Download CSV' }).first().click(),
    ]);
    expect(download.suggestedFilename()).toMatch(/sales-summary.*\.csv$/i);
  });

  test('can open sales and start POS', async ({ page }) => {
    await openPageFromNav(page, 'Sales', 'Sales');
    await page.getByRole('button', { name: 'New sale' }).click();
    await expect(page.getByRole('heading', { name: 'New sale', level: 2 })).toBeVisible();
  });
});

test.describe('Shop worker', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, DEMO_USERS.worker);
  });

  test('sees floor operations nav but not reports or admin', async ({ page }) => {
    await expectNavLink(page, 'Inventory', true);
    await expectNavLink(page, 'Sales', true);
    await expectNavLink(page, 'Transfers', true);
    await expectNavLink(page, 'Reports', false);
    await expectNavLink(page, 'Users', false);
  });

  test('can open inventory and sales without errors', async ({ page }) => {
    await openPageFromNav(page, 'Inventory', 'Inventory balances');
    await openPageFromNav(page, 'Sales', 'Sales');
  });

  test('can open transfer request form', async ({ page }) => {
    await openPageFromNav(page, 'Transfers', 'Stock transfers');
    await page.getByRole('button', { name: 'New request' }).click();
    await expect(page.getByRole('heading', { name: 'New stock transfer', level: 2 })).toBeVisible();
    await expect(page.locator('.form__error').filter({ hasText: /Failed to load/i })).toHaveCount(0);
  });

  test('can open POS panel', async ({ page }) => {
    await openPageFromNav(page, 'Sales', 'Sales');
    await page.getByRole('button', { name: 'New sale' }).click();
    await expect(page.getByRole('heading', { name: 'New sale', level: 2 })).toBeVisible();
    await expect(page.locator('.form__error').filter({ hasText: /Failed to load/i })).toHaveCount(0);
  });
});
