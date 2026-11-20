import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import * as icons from 'simple-icons';

const __dirname = dirname(fileURLToPath(import.meta.url));
const outDir = join(__dirname, '..', 'public', 'brand-marks');
mkdirSync(outDir, { recursive: true });

const brands = [
  { slug: 'moderndl', label: 'modernDL', color: '#9A7428' },
  { slug: 'philips', label: 'Philips', icon: 'siPhilipshue', color: '#0A5C9C' },
  { slug: 'v-tac', label: 'V-TAC', color: '#E31C23' },
  { slug: 'v-max', label: 'V-MAX', color: '#1A1A1A' },
  { slug: 'havells', label: 'Havells', icon: 'siHavells', color: '#ED1C24' },
  { slug: 'schneider-electric', label: 'Schneider', icon: 'siSchneiderelectric', color: '#3DCD58' },
  { slug: 'osram', label: 'Osram', color: '#FFCC00' },
  { slug: 'hager', label: 'Hager', color: '#009BDE' },
  { slug: 'orient', label: 'Orient', color: '#C41230' },
  { slug: 'rexton', label: 'Rexton', color: '#1B4F72' },
  { slug: 'hpl', label: 'HPL', color: '#003366' },
  { slug: 'clipsal', label: 'Clipsal', color: '#E30613' },
  { slug: 'abb', label: 'ABB', icon: 'siAbb', color: '#FF000F' },
  { slug: 'panasonic', label: 'Panasonic', icon: 'siPanasonic', color: '#0049A5' },
  { slug: 'crompton', label: 'Crompton', color: '#0057A8' },
  { slug: 'bajaj', label: 'Bajaj', color: '#ED1C24' },
  { slug: 'legrand', label: 'Legrand', color: '#C8102E' },
  { slug: 'mk', label: 'MK', color: '#E30613' },
  { slug: 'chint', label: 'Chint', color: '#E60012' },
  { slug: 'fsl', label: 'FSL', color: '#F36C00' },
  { slug: 'winluk', label: 'Winluk', color: '#0D47A1' },
  { slug: 'zenith', label: 'Zenith', color: '#111111' },
  { slug: 'emkay', label: 'Emkay', color: '#2E7D32' },
  { slug: 'modi', label: 'Modi', color: '#6A1B9A' },
  { slug: 'seera', label: 'Seera', color: '#00695C' },
  { slug: 'sollatek', label: 'Sollatek', color: '#009640' },
  { slug: 'tungsram', label: 'Tungsram', color: '#E30613' },
];

function wordmarkSvg(label, color) {
  const fontSize = label.length > 10 ? 22 : label.length > 7 ? 26 : 30;
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="320" height="120" viewBox="0 0 320 120" role="img" aria-label="${label}">
  <rect width="320" height="120" fill="#ffffff"/>
  <text x="160" y="68" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" font-size="${fontSize}" font-weight="700" fill="${color}">${label}</text>
</svg>
`;
}

function iconSvg(icon, label) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 24 24" role="img" aria-label="${label}">
  <title>${label}</title>
  <path fill="#${icon.hex}" d="${icon.path}"/>
</svg>
`;
}

for (const brand of brands) {
  const icon = brand.icon ? icons[brand.icon] : null;
  const svg = icon ? iconSvg(icon, brand.label) : wordmarkSvg(brand.label, brand.color);
  writeFileSync(join(outDir, `${brand.slug}.svg`), svg);
  console.log('wrote', brand.slug);
}
