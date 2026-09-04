import { expect, type Page } from '@playwright/test';

export interface DemoUser {
  email: string;
  password: string;
  label: string;
}

export const DEMO_USERS = {
  owner: { email: 'owner@mdl.local', password: 'Owner@123!', label: 'Owner' },
  manager: { email: 'michael@mdl.local', password: 'Manager@123!', label: 'Shop manager' },
  worker: { email: 'john@mdl.local', password: 'Worker@123!', label: 'Shop worker' },
} as const satisfies Record<string, DemoUser>;

export async function loginAs(page: Page, user: DemoUser) {
  await page.goto('/login');
  await page.getByLabel('Email or username').fill(user.email);
  await page.getByLabel('Password').fill(user.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 20000 });
}

export async function expectNavLink(page: Page, name: string, visible: boolean) {
  const link = page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', { name });
  if (visible) {
    await expect(link).toBeVisible();
  } else {
    await expect(link).toHaveCount(0);
  }
}

export async function openPageFromNav(page: Page, linkName: string, heading: string) {
  await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', { name: linkName }).click();
  await expect(page.getByRole('heading', { name: heading })).toBeVisible({ timeout: 15000 });
  await expect(page.locator('.form__error').filter({ hasText: /Failed to load|403|conflict/i })).toHaveCount(0);
}
