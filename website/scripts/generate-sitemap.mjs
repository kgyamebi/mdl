import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const origin = 'https://moderndreamlight.com';

const staticPaths = ['/', '/about', '/ranges', '/brands', '/contact'];

const seoSource = readFileSync(join(__dirname, '..', 'src', 'data', 'seoPages.ts'), 'utf8');
const seoSlugs = [...seoSource.matchAll(/slug:\s*'([^']+)'/g)].map((match) => match[1]);

const urls = [
  ...staticPaths.map((path) => ({ path, priority: path === '/' ? '1.0' : '0.8' })),
  ...seoSlugs.map((slug) => ({ path: `/${slug}`, priority: '0.7' })),
];

const today = new Date().toISOString().slice(0, 10);

const xml = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls
  .map(
    ({ path, priority }) => `  <url>
    <loc>${origin}${path}</loc>
    <lastmod>${today}</lastmod>
    <changefreq>weekly</changefreq>
    <priority>${priority}</priority>
  </url>`,
  )
  .join('\n')}
</urlset>
`;

writeFileSync(join(__dirname, '..', 'public', 'sitemap.xml'), xml);
console.log(`wrote sitemap with ${urls.length} urls`);
