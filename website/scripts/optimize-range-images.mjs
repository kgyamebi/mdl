import { readdirSync, writeFileSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const __dirname = dirname(fileURLToPath(import.meta.url));
const dir = join(__dirname, '..', 'public', 'ranges');

/** Product shots sit better contained on a warm field; lifestyle shots cover. */
const fitByFile = {
  lighting: 'cover',
  fans: 'cover',
  cables: 'contain',
  switches: 'contain',
  protection: 'contain',
  boards: 'contain',
  conduit: 'contain',
  tools: 'contain',
};

const WIDTH = 1600;
const HEIGHT = 900;
const bg = { r: 242, g: 240, b: 235, alpha: 1 };

const files = readdirSync(dir).filter((f) => /\.jpe?g$/i.test(f) && !f.includes('.tmp.'));

for (const file of files) {
  const id = file.replace(/\.jpe?g$/i, '');
  const fit = fitByFile[id] ?? 'cover';
  const input = join(dir, file);

  const source = await sharp(input).rotate().toBuffer();
  const resized = sharp(source).resize(WIDTH, HEIGHT, {
    fit,
    position: fit === 'cover' ? 'attention' : 'centre',
    background: bg,
  });

  // Write optimized derivatives (avoid overwriting OneDrive-locked originals)
  const webpOut = join(dir, `${id}.webp`);
  const optJpgOut = join(dir, `${id}.opt.jpg`);

  const jpgBuf = await resized.clone().jpeg({ quality: 84, mozjpeg: true }).toBuffer();
  const webpBuf = await resized.clone().webp({ quality: 82 }).toBuffer();

  writeFileSync(optJpgOut, jpgBuf);
  writeFileSync(webpOut, webpBuf);

  console.log(`${id}: ${fit} → ${WIDTH}x${HEIGHT} (${existsSync(webpOut) ? 'webp+opt' : 'fail'})`);
}

console.log(`optimized ${files.length} range images`);
