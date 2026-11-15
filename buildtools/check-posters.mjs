/**
 * Checks the composed banners without needing to look at them.
 *
 * For each banner it measures local detail (standard deviation of luma over 8x8 tiles) in two
 * regions: where the real poster was placed and where only the blurred backdrop is. A real poster
 * has noticeably more detail than a blur, so the ratio tells us the poster is present, sharp and
 * not washed out by the scrim.
 */
import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";

const dir = "C:/Projects/Orderly/buildtools/images/posters";
const W = 960;
const H = 600;
const POSTER_H = 430;
const MARGIN_RIGHT = 52;

/** Same source aspect ratios the fetch script measured, used to locate the poster. */
const RATIOS = {
  alien: 250 / 382, conjuring: 220 / 326, dc: 259 / 383, dune: 259 / 384,
  fast: 235 / 350, godzilla: 2135 / 3000, halloween: 259 / 384,
  "hunger-games": 220 / 326, "indiana-jones": 257 / 388, jurassic: 220 / 328,
  "mad-max": 264 / 378, mcu: 260 / 384, "middle-earth": 218 / 326,
  predator: 258 / 382, "star-trek": 259 / 383, "star-wars": 252 / 385,
  terminator: 260 / 385, transformers: 265 / 376, "wizarding-world": 259 / 384,
  "x-men": 248 / 368,
};

/** Mean of per-tile luma standard deviations for one crop. */
async function detail(file, left, top, width, height) {
  const { data, info } = await sharp(file)
    .extract({ left, top, width, height })
    .greyscale()
    .raw()
    .toBuffer({ resolveWithObject: true });

  const tiles = [];
  const size = 8;
  for (let y = 0; y + size <= info.height; y += size) {
    for (let x = 0; x + size <= info.width; x += size) {
      let sum = 0;
      let sumSq = 0;
      for (let j = 0; j < size; j++) {
        for (let i = 0; i < size; i++) {
          const v = data[(y + j) * info.width + (x + i)];
          sum += v;
          sumSq += v * v;
        }
      }
      const n = size * size;
      tiles.push(Math.sqrt(Math.max(0, sumSq / n - (sum / n) ** 2)));
    }
  }
  tiles.sort((a, b) => a - b);
  const mean = tiles.reduce((a, b) => a + b, 0) / tiles.length;
  return { mean: +mean.toFixed(2), p90: +tiles[Math.floor(tiles.length * 0.9)].toFixed(2) };
}

const files = fs.readdirSync(dir).filter((f) => f.endsWith(".webp")).sort();
let bad = 0;

console.log("id               size       backdrop      poster     ratio");
for (const f of files) {
  const id = f.replace(/\.webp$/, "");
  const file = path.join(dir, f);
  const meta = await sharp(file).metadata();
  const ratio = RATIOS[id];
  if (!ratio) throw new Error(`no measured ratio for ${id}`);
  const w = Math.round(POSTER_H * ratio);
  const h = POSTER_H;
  const x = W - w - MARGIN_RIGHT;

  const backdrop = await detail(file, 20, 300, 200, 200);
  const poster = await detail(file, x + 8, 56, w - 16, h - 16);
  const ratioDetail = +(poster.mean / Math.max(backdrop.mean, 0.01)).toFixed(2);

  const ok = meta.width === W && meta.height === H && ratioDetail > 2.5;
  if (!ok) bad++;
  console.log(
    `${id.padEnd(16)} ${`${meta.width}x${meta.height}`.padEnd(10)} ` +
      `${String(backdrop.mean).padStart(8)} ${String(poster.mean).padStart(12)} ${String(ratioDetail).padStart(10)}  ${ok ? "" : "  <-- CHECK"}`,
  );
}
console.log(bad === 0 ? `\nall ${files.length} banners look right` : `\n${bad} of ${files.length} need a look`);