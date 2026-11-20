import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import * as icons from 'simple-icons';

const __dirname = dirname(fileURLToPath(import.meta.url));
const outDir = join(__dirname, '..', 'public', 'brands');
mkdirSync(outDir, { recursive: true });

/** Domains used to pull clearer brand marks where available. */
const brands = [
  { slug: 'moderndl', label: 'modernDL', color: '#9A7428' },
  { slug: 'philips', label: 'Philips', icon: 'siPhilipshue', domain: 'philips.com', color: '#0A5C9C' },
  { slug: 'v-tac', label: 'V-TAC', domain: 'v-tac.eu', color: '#E31C23' },
  { slug: 'v-max', label: 'V-MAX', color: '#111111' },
  { slug: 'havells', label: 'Havells', icon: 'siHavells', domain: 'havells.com', color: '#ED1C24' },
  { slug: 'schneider-electric', label: 'Schneider Electric', icon: 'siSchneiderelectric', domain: 'se.com', color: '#3DCD58' },
  { slug: 'osram', label: 'Osram', domain: 'osram.com', color: '#FFCC00' },
  { slug: 'hager', label: 'Hager', domain: 'hager.com', color: '#009BDE' },
  { slug: 'orient', label: 'Orient', domain: 'orientelectric.com', color: '#C41230' },
  { slug: 'rexton', label: 'Rexton', color: '#1B4F72' },
  { slug: 'hpl', label: 'HPL', domain: 'hplindia.com', color: '#003366' },
  { slug: 'clipsal', label: 'Clipsal', domain: 'clipsal.com', color: '#E30613' },
  { slug: 'abb', label: 'ABB', icon: 'siAbb', domain: 'abb.com', color: '#FF000F' },
  { slug: 'panasonic', label: 'Panasonic', icon: 'siPanasonic', domain: 'panasonic.com', color: '#0049A5' },
  { slug: 'crompton', label: 'Crompton', domain: 'crompton.co.in', color: '#0057A8' },
  { slug: 'bajaj', label: 'Bajaj', domain: 'bajajelectricals.com', color: '#ED1C24' },
  { slug: 'legrand', label: 'Legrand', domain: 'legrand.com', color: '#C8102E' },
  { slug: 'mk', label: 'MK', domain: 'mkelectric.com', color: '#E30613' },
  { slug: 'chint', label: 'Chint', domain: 'chintglobal.com', color: '#E60012' },
  { slug: 'fsl', label: 'FSL', color: '#F36C00' },
  { slug: 'winluk', label: 'Winluk', color: '#0D47A1' },
  { slug: 'zenith', label: 'Zenith', color: '#111111' },
  { slug: 'emkay', label: 'Emkay', color: '#2E7D32' },
  { slug: 'modi', label: 'Modi', color: '#6A1B9A' },
  { slug: 'seera', label: 'Seera', color: '#00695C' },
  { slug: 'sollatek', label: 'Sollatek', domain: 'sollatek.com', color: '#009640' },
  { slug: 'tungsram', label: 'Tungsram', domain: 'tungsram.com', color: '#E30613' },
];

function iconSvg(icon, label, forceWhite = false) {
  const fill = forceWhite ? '#ffffff' : `#${icon.hex}`;
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" role="img" aria-label="${label}">
  <title>${label}</title>
  <path fill="${fill}" d="${icon.path}"/>
</svg>`;
}

function wordmarkSvg(label, color) {
  const safe = label.replace(/&/g, '&amp;');
  const size = label.length > 16 ? 34 : label.length > 10 ? 42 : 52;
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="480" height="120" viewBox="0 0 480 120" role="img" aria-label="${safe}">
  <title>${safe}</title>
  <text x="240" y="72" text-anchor="middle" font-family="Arial Black, Arial, Helvetica, sans-serif" font-size="${size}" font-weight="800" letter-spacing="1.5" fill="${color}">${safe.toUpperCase()}</text>
</svg>`;
}

async function tryFetchLogo(domain, slug) {
  const candidates = [
    `https://www.google.com/s2/favicons?domain=${domain}&sz=128`,
    `https://icons.duckduckgo.com/ip3/${domain}.ico`,
  ];
  for (const url of candidates) {
    try {
      const res = await fetch(url);
      if (!res.ok) continue;
      const type = res.headers.get('content-type') || '';
      if (!type.includes('image') && !type.includes('icon')) continue;
      const buf = Buffer.from(await res.arrayBuffer());
      if (buf.length < 200) continue;
      const ext = type.includes('png') ? 'png' : type.includes('jpeg') || type.includes('jpg') ? 'jpg' : 'png';
      const file = join(outDir, `${slug}.${ext}`);
      writeFileSync(file, buf);
      return `${slug}.${ext}`;
    } catch {
      // continue
    }
  }
  return null;
}

for (const brand of brands) {
  const icon = brand.icon ? icons[brand.icon] : null;
  // Always write a clean SVG mark for the marquee (white for dark bg).
  if (icon) {
    writeFileSync(join(outDir, `${brand.slug}.svg`), iconSvg(icon, brand.label, false));
    writeFileSync(join(outDir, `${brand.slug}-light.svg`), iconSvg(icon, brand.label, true));
  } else {
    writeFileSync(join(outDir, `${brand.slug}.svg`), wordmarkSvg(brand.label, brand.color));
    writeFileSync(join(outDir, `${brand.slug}-light.svg`), wordmarkSvg(brand.label, '#ffffff'));
  }

  if (brand.domain) {
    const fetched = await tryFetchLogo(brand.domain, brand.slug);
    console.log(brand.slug, fetched ? `fetched ${fetched}` : 'svg only');
  } else {
    console.log(brand.slug, 'svg only');
  }
}
