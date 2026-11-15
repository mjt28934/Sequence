/**
 * Compares a fresh render with the previous release screenshot, tile by tile.
 *
 * Reports, for a grid of tiles, how much local detail each has. That answers the question a
 * screenshot alone cannot: did the artwork actually make it onto the screen, and where did the
 * layout change.
 *
 *   node buildtools/diff-shots.mjs old.png new.png
 */
import sharp from "sharp";

const [a, b] = process.argv.slice(2);
if (!a || !b) {
  console.error("usage: node diff-shots.mjs before.png after.png");
  process.exit(1);
}

async function luma(file) {
  const { data, info } = await sharp(file).greyscale().raw().toBuffer({ resolveWithObject: true });
  return { data, w: info.width, h: info.height };
}

const A = await luma(a);
const B = await luma(b);
if (A.w !== B.w || A.h !== B.h) console.log(`size differs: ${A.w}x${A.h} vs ${B.w}x${B.h}`);

function detail(img, x0, y0, x1, y1) {
  let sum = 0;
  let sumSq = 0;
  let n = 0;
  for (let y = y0; y < y1; y++) {
    for (let x = x0; x < x1; x++) {
      const v = img.data[y * img.w + x];
      sum += v;
      sumSq += v * v;
      n++;
    }
  }
  const mean = sum / n;
  return { mean: +mean.toFixed(1), spread: +Math.sqrt(Math.max(0, sumSq / n - mean ** 2)).toFixed(1) };
}

const rows = 10;
const cols = 4;
const tw = Math.floor(A.w / cols);
const th = Math.floor(A.h / rows);
console.log("row  " + Array.from({ length: cols }, (_, c) => `tile ${c}`.padEnd(22)).join(""));
for (let r = 0; r < rows; r++) {
  const cells = [];
  for (let c = 0; c < cols; c++) {
    const x0 = c * tw;
    const y0 = r * th;
    const da = detail(A, x0, y0, x0 + tw, y0 + th);
    const db = detail(B, x0, y0, x0 + tw, y0 + th);
    cells.push(`${db.mean}/${db.spread}`.padEnd(9) + `(was ${da.mean}/${da.spread})`.padEnd(13));
  }
  console.log(`${String(r).padEnd(5)}${cells.join("")}`);
}
