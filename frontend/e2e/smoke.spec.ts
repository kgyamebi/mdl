import { test, expect } from '@playwright/test';

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
