/**
 * Fetches real cover art for the books added in 1.6, the same way fetch-posters.mjs does posters.
 *
 * Book articles lead with the first-edition cover, which is already 2:3, so each one is simply
 * cover-cropped to the 300x450 the app expects and written into assets/art/covers.
 *
 *   node fetch-covers.mjs           # every book in book-sources.json
 *   node fetch-covers.mjs bk-bd-1   # one, plus a PNG preview in buildtools/out/covers-preview
 */
import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";

const root = "C:/Projects/Orderly/buildtools";
const srcDir = path.join(root, "covers-src");
const outDir = path.join(root, "images", "covers");
const previewDir = path.join(root, "out", "covers-preview");
const assetDir = "C:/Projects/Orderly/app/src/main/assets/art/covers";
const sourcesPath = path.join(root, "book-sources.json");

const W = 300;
const H = 450;
const UA = { "User-Agent": "Sequence/1.6 (offline watch-order app; cover fetch)" };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function get(url) {
  for (let attempt = 0; attempt < 4; attempt++) {
    const res = await fetch(url, { headers: UA });
    if (res.ok) return res;
    if (res.status !== 429 && res.status < 500) throw new Error(`HTTP ${res.status} ${url}`);
    await sleep(1500 * (attempt + 1));
  }
  throw new Error(`HTTP 429 ${url}`);
}

async function summary(title) {
  const url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encodeURIComponent(title.replace(/ /g, "_"));
  const json = await (await get(url)).json();
  if (!json.originalimage?.source) throw new Error(`${title}: no lead image`);
  return json;
}

const sources = JSON.parse(fs.readFileSync(sourcesPath, "utf8"));
const only = process.argv.slice(2).filter((a) => !a.startsWith("-"));
const ids = Object.keys(sources).filter((id) => !only.length || only.includes(id));

fs.mkdirSync(srcDir, { recursive: true });
fs.mkdirSync(outDir, { recursive: true });
fs.mkdirSync(assetDir, { recursive: true });
if (only.length) fs.mkdirSync(previewDir, { recursive: true });

let done = 0;
const missing = [];
for (const id of ids) {
  const { page } = sources[id];
  try {
    const json = await summary(page);
    const url = json.originalimage.source.split("?")[0];
    const ext = path.extname(url) || ".jpg";
    const cached = fs.readdirSync(srcDir).find((f) => f.startsWith(`${id}.`));
    let file = cached ? path.join(srcDir, cached) : path.join(srcDir, `${id}${ext}`);
    if (!cached) fs.writeFileSync(file, Buffer.from(await (await get(url)).arrayBuffer()));

    const meta = await sharp(file).metadata();
    const ratio = meta.width / meta.height;
    if (ratio > 0.85) throw new Error(`not a portrait cover (${meta.width}x${meta.height})`);

    const webp = await sharp(file)
      .resize(W, H, { fit: "cover", position: "top" })
      .webp({ quality: 84, effort: 5 })
      .toBuffer();
    fs.writeFileSync(path.join(outDir, `${id}.webp`), webp);
    fs.writeFileSync(path.join(assetDir, `${id}.webp`), webp);
    if (only.length) {
      await sharp(path.join(outDir, `${id}.webp`)).png().toFile(path.join(previewDir, `${id}.png`));
    }
    done++;
    console.log(`${id.padEnd(14)} ${sources[id].title.slice(0, 38).padEnd(38)} ${(webp.length / 1024).toFixed(1)} KB`);
  } catch (err) {
    missing.push(id);
    console.warn(`${id.padEnd(14)} SKIPPED  ${err.message}`);
  }
  await sleep(600);
}

console.log(`\n${done}/${ids.length} covers written`);
if (missing.length) console.log(`no cover for: ${missing.join(", ")}`);