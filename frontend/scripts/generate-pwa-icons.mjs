import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const __dirname = dirname(fileURLToPath(import.meta.url));
const publicDir = join(__dirname, '..', 'public');
const svgPath = join(publicDir, 'icon.svg');
const svg = readFileSync(svgPath);

async function writePng(size, filename) {
  const output = join(publicDir, filename);
  await sharp(svg).resize(size, size).png({ compressionLevel: 9 }).toFile(output);
  console.log(`Wrote ${filename} (${size}x${size})`);
}

/** Maskable safe zone — icon content scaled to ~80% inside the canvas. */
async function writeMaskablePng(size, filename) {
  const inner = Math.round(size * 0.72);
  const offset = Math.round((size - inner) / 2);
  const innerBuffer = await sharp(svg).resize(inner, inner).png().toBuffer();
  const output = join(publicDir, filename);
  await sharp({
    create: {
      width: size,
      height: size,
      channels: 4,
      background: { r: 15, g: 20, b: 25, alpha: 1 },
    },
  })
    .composite([{ input: innerBuffer, left: offset, top: offset }])
    .png({ compressionLevel: 9 })
    .toFile(output);
  console.log(`Wrote ${filename} (${size}x${size}, maskable)`);
}

await writePng(180, 'icon-180.png');
await writePng(192, 'icon-192.png');
await writePng(512, 'icon-512.png');
await writeMaskablePng(512, 'icon-512-maskable.png');

console.log('PWA PNG icons generated from icon.svg');
