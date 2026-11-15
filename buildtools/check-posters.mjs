/**
 * Checks the composed banners without needing to look at them.
 *
 * For each banner it measures local detail (standard deviation of luma over 8x8 tiles) where the real
 * poster was placed and where only blurred backdrop is. A real poster carries far more detail than a
 * blur, so a blank, missing or washed-out poster shows up as a low ratio.
 *
 * Geometry comes from buildtools/poster-sources.json, so this stays in step with fetch-posters.mjs.
 */
import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";

const root = "C:/Projects/Orderly/buildtools";
const dir = path.join(root, "images", "posters");
const sourcesPath = path.join(root, "poster-sources.json");

const W = 960;
const H = 600;
const POSTER_H = 430;
const MARGIN_RIGHT = 52;

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

const sources = JSON.parse(fs.readFileSync(sourcesPath, "utf8"));
const byId = new Map(sources.map((s) => [s.id, s]));
const files = fs.readdirSync(dir).filter((f) => f.endsWith(".webp")).sort();
let bad = 0;

console.log("id               size       wide     backdrop      poster     ratio");
for (const f of files) {
  const id = f.replace(/\.webp$/, "");
  const file = path.join(dir, f);
  const source = byId.get(id);
  if (!source) {
    console.log(`${id.padEnd(16)} no source entry  <-- CHECK`);
    bad++;
    continue;
  }

  const meta = await sharp(file).metadata();
  if (meta.width !== W || meta.height !== H) {
    console.log(`${id.padEnd(16)} ${meta.width}x${meta.height}  <-- CHECK`);
    bad++;
    continue;
  }

  const isWide = source.width / source.height > 0.95;
  let backdropCrop;
  let posterCrop;
  if (isWide) {
    // Full-bleed: the whole frame is the poster, so sample the middle against a corner scrim.
    backdropCrop = [12, 12, 140, 140];
    posterCrop = [Math.round(W * 0.35), 120, 320, 320];
  } else {
    const w = Math.round(POSTER_H * (source.width / source.height));
    const x = W - w - MARGIN_RIGHT;
    backdropCrop = [20, 300, 200, 200];
    posterCrop = [x + 10, 58, w - 20, POSTER_H - 20];
  }

  const backdrop = await detail(file, ...backdropCrop);
  const poster = await detail(file, ...posterCrop);
  const ratio = +(poster.mean / Math.max(backdrop.mean, 0.01)).toFixed(2);
  const ok = ratio > (isWide ? 1.6 : 2.5);
  if (!ok) bad++;
  console.log(
    `${id.padEnd(16)} ${`${meta.width}x${meta.height}`.padEnd(10)} ${String(isWide ? "yes" : "no").padEnd(6)} ` +
      `${String(backdrop.mean).padStart(8)} ${String(poster.mean).padStart(12)} ${String(ratio).padStart(10)}  ${ok ? "" : "<-- CHECK"}`,
  );
}
console.log(bad === 0 ? `\nall ${files.length} banners look right` : `\n${bad} of ${files.length} need a look`);