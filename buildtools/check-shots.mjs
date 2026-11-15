/**
 * Numeric sanity check for a rendered screenshot: is there actual artwork on it?
 *
 * Reports size, mean luma, luma spread and the share of pixels that are neither near-black nor
 * near-white, which is what a screen full of real posters looks like. A blank or placeholder-only
 * render comes out with a very low spread and almost no mid-tones.
 *
 *   node buildtools/check-shots.mjs app/build/reports/paparazzi/debug/images/*.png
 *   node buildtools/check-shots.mjs screenshots/01-home.png
 */
import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";

const files = process.argv.slice(2).filter((a) => !a.startsWith("-"));
if (!files.length) {
  console.error("pass one or more image paths");
  process.exit(1);
}

console.log("file                                   size          mean    spread   midtones");
let bad = 0;
for (const file of files) {
  const { data, info } = await sharp(file).greyscale().raw().toBuffer({ resolveWithObject: true });
  let sum = 0;
  let sumSq = 0;
  let mid = 0;
  const n = info.width * info.height;
  for (let i = 0; i < n; i++) {
    const v = data[i];
    sum += v;
    sumSq += v * v;
    if (v > 26 && v < 232) mid++;
  }
  const mean = sum / n;
  const spread = Math.sqrt(Math.max(0, sumSq / n - mean ** 2));
  const midShare = mid / n;
  const ok = spread > 18 && midShare > 0.12;
  if (!ok) bad++;
  console.log(
    `${path.basename(file).padEnd(38)} ${`${info.width}x${info.height}`.padEnd(12)} ` +
      `${mean.toFixed(1).padStart(6)} ${spread.toFixed(1).padStart(8)} ${(midShare * 100).toFixed(1).padStart(9)}%  ${ok ? "" : "<-- CHECK"}`,
  );
}
console.log(bad === 0 ? `\n${files.length} renders carry real artwork` : `\n${bad} of ${files.length} look blank`);
