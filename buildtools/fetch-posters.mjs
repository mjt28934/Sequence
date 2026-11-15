/**
 * Replaces the drawn franchise artwork with the real theatrical posters.
 *
 * For every franchise the poster is the lead image of its English Wikipedia film article. It is
 * composed into the 960x600 banner the app uses: the real poster sits at full sharpness on the right
 * over a blurred, darkened copy of itself, so the 16:10 crop the layouts need still works and the
 * left half stays clear for the title text laid over it.
 *
 * Usage:
 *   node fetch-posters.mjs            # every franchise
 *   node fetch-posters.mjs alien dune # only the listed ids, plus PNG previews in buildtools/out
 *
 * Also writes buildtools/poster-sources.json and the top-level POSTERS.md, so the attribution table
 * can never drift from what was actually downloaded.
 */
import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";

const root = "C:/Projects/Orderly/buildtools";
const srcDir = path.join(root, "posters-src");
const outDir = path.join(root, "images", "posters");
const previewDir = path.join(root, "out", "posters-preview");
const assetDir = "C:/Projects/Orderly/app/src/main/assets/art/posters";
const sourcesPath = path.join(root, "poster-sources.json");
const attributionPath = "C:/Projects/Orderly/POSTERS.md";

const W = 960;
const H = 600;
const POSTER_H = 430;
const MARGIN_RIGHT = 52;
const TOP = 48;
const ROUND = 20;

/** Franchise id -> the Wikipedia article whose lead image is that franchise's poster. */
const PAGES = {
  alien: "Alien (film)",
  conjuring: "The Conjuring",
  dc: "The Dark Knight",
  dune: "Dune (2021 film)",
  fast: "The Fast and the Furious (2001 film)",
  godzilla: "Godzilla (1954 film)",
  halloween: "Halloween (1978 film)",
  "hunger-games": "The Hunger Games (film)",
  "indiana-jones": "Raiders of the Lost Ark",
  jurassic: "Jurassic Park (film)",
  "mad-max": "Mad Max: Fury Road",
  mcu: "The Avengers (2012 film)",
  "middle-earth": "The Lord of the Rings: The Fellowship of the Ring",
  predator: "Predator (film)",
  "star-trek": "Star Trek (2009 film)",
  "star-wars": "Star Wars (film)",
  terminator: "The Terminator",
  transformers: "Transformers (film)",
  "wizarding-world": "Fantastic Beasts and Where to Find Them (film)",
  "x-men": "X-Men (film)",
};

const UA = { "User-Agent": "Sequence/1.5 (offline watch-order app; poster fetch)" };

async function summary(title) {
  const url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encodeURIComponent(title.replace(/ /g, "_"));
  const res = await fetch(url, { headers: UA });
  if (!res.ok) throw new Error(`${title}: HTTP ${res.status}`);
  const json = await res.json();
  if (!json.originalimage?.source) throw new Error(`${title}: no lead image`);
  return json;
}

async function download(url, dest) {
  const res = await fetch(url.split("?")[0], { headers: UA });
  if (!res.ok) throw new Error(`HTTP ${res.status} ${url}`);
  fs.writeFileSync(dest, Buffer.from(await res.arrayBuffer()));
}

/** Cached original plus where it came from; the download is skipped when already on disk. */
async function sourcePoster(id) {
  const page = await summary(PAGES[id]);
  const clean = page.originalimage.source.split("?")[0];
  const cached = fs.readdirSync(srcDir).find((f) => f.startsWith(`${id}.`));
  const file = cached ? path.join(srcDir, cached) : path.join(srcDir, `${id}${path.extname(clean) || ".jpg"}`);
  if (!cached) await download(clean, file);
  return {
    file,
    id,
    name: page.title,
    page: page.content_urls?.desktop?.page ?? `https://en.wikipedia.org/wiki/${encodeURIComponent(PAGES[id].replace(/ /g, "_"))}`,
    url: clean,
    width: page.originalimage.width,
    height: page.originalimage.height,
  };
}

async function compose(src, id) {
  const meta = await sharp(src).metadata();
  if (!meta.width || !meta.height) throw new Error(`${id}: unreadable image`);
  const ratio = meta.width / meta.height;
  if (ratio > 0.95) throw new Error(`${id}: lead image is not a portrait poster (${meta.width}x${meta.height})`);

  const w = Math.round(POSTER_H * ratio);
  const h = POSTER_H;
  const x = W - w - MARGIN_RIGHT;

  const backdrop = await sharp(src)
    .resize(W, H, { fit: "cover" })
    .blur(30)
    .modulate({ brightness: 0.52, saturation: 1.1 })
    .toBuffer();

  const scrim = Buffer.from(
    `<svg width="${W}" height="${H}" xmlns="http://www.w3.org/2000/svg">
       <defs>
         <linearGradient id="h" x1="0" y1="0" x2="1" y2="0">
           <stop offset="0" stop-color="#05070c" stop-opacity="0.86"/>
           <stop offset="0.55" stop-color="#05070c" stop-opacity="0.45"/>
           <stop offset="1" stop-color="#05070c" stop-opacity="0.12"/>
         </linearGradient>
         <linearGradient id="v" x1="0" y1="0" x2="0" y2="1">
           <stop offset="0" stop-color="#05070c" stop-opacity="0.30"/>
           <stop offset="0.45" stop-color="#05070c" stop-opacity="0"/>
           <stop offset="1" stop-color="#05070c" stop-opacity="0.72"/>
         </linearGradient>
       </defs>
       <rect width="${W}" height="${H}" fill="url(#h)"/>
       <rect width="${W}" height="${H}" fill="url(#v)"/>
     </svg>`,
  );

  const mask = Buffer.from(
    `<svg width="${w}" height="${h}" xmlns="http://www.w3.org/2000/svg">
       <rect width="${w}" height="${h}" rx="${ROUND}" ry="${ROUND}" fill="#fff"/>
     </svg>`,
  );

  const ring = Buffer.from(
    `<svg width="${w}" height="${h}" xmlns="http://www.w3.org/2000/svg">
       <rect x="0.75" y="0.75" width="${w - 1.5}" height="${h - 1.5}" rx="${ROUND}" ry="${ROUND}"
             fill="none" stroke="#ffffff" stroke-opacity="0.16" stroke-width="1.5"/>
     </svg>`,
  );

  const rounded = await sharp(src)
    .resize(w, h, { fit: "cover" })
    .composite([
      { input: mask, blend: "dest-in" },
      { input: ring, blend: "over" },
    ])
    .png()
    .toBuffer();

  const pad = 64;
  const shadow = await sharp(
    Buffer.from(
      `<svg width="${w + pad * 2}" height="${h + pad * 2}" xmlns="http://www.w3.org/2000/svg">
         <rect x="${pad}" y="${pad}" width="${w}" height="${h}" rx="${ROUND}" ry="${ROUND}" fill="#000" fill-opacity="0.62"/>
       </svg>`,
    ),
  )
    .blur(24)
    .png()
    .toBuffer();

  const composed = sharp(backdrop)
    .composite([
      { input: scrim, left: 0, top: 0 },
      { input: shadow, left: x - pad + 8, top: TOP - pad + 12 },
      { input: rounded, left: x, top: TOP },
    ])
    .png();

  const webp = await composed.clone().webp({ quality: 82, effort: 5 }).toBuffer();
  fs.writeFileSync(path.join(outDir, `${id}.webp`), webp);
  fs.writeFileSync(path.join(assetDir, `${id}.webp`), webp);
  return { w, h, bytes: webp.length };
}

/** Attribution table + the copyright note, regenerated from what was actually downloaded. */
function writeAttribution(sources) {
  const rows = sources
    .slice()
    .sort((a, b) => a.id.localeCompare(b.id))
    .map((s) => `| ${s.franchise} | \`${s.id}\` | [${s.name}](${s.page}) | \`${s.url.replace("https://upload.wikimedia.org/", "…/")}\` |`)
    .join("\n");

  fs.writeFileSync(
    sourcesPath,
    JSON.stringify(sources.map(({ franchise, id, name, page, url }) => ({ franchise, id, name, page, url })), null, 2) + "\n",
  );
  fs.writeFileSync(
    attributionPath,
    `# Poster sources

Each franchise banner in \`app/src/main/assets/art/posters/\` is built from the real theatrical poster
of that franchise, using the lead image of the listed English Wikipedia article. \`buildtools/fetch-posters.mjs\`
composes it into a 960x600 banner and writes it into the app's assets. This table is generated by the
same script, so it always matches what was downloaded.

| Franchise | id | Wikipedia article | Poster file |
| --- | --- | --- | --- |
${rows}

## Copyright

The posters are the property of their respective studios and appear here for identification only, the
way a review or a catalogue would show them. They are **not** covered by the repository's code licence.
If you plan to publish your own build, check that your distribution channel allows this — bundling film
posters in a public repository or an app-store listing is a rights question, not a technical one.

## Regenerating

\`\`\`powershell
cd C:\\Projects\\Orderly\\buildtools
node fetch-posters.mjs            # all 20, straight into app/src/main/assets/art/posters
node fetch-posters.mjs alien dune # a few, plus PNG previews in buildtools/out/posters-preview
node check-posters.mjs            # sanity check: dimensions and per-region detail
\`\`\`

Downloaded originals are cached in \`buildtools/posters-src/\` (git-ignored); only the finished
banners are committed.
`,
  );
}

const NAMES = {
  alien: "Alien",
  conjuring: "The Conjuring Universe",
  dc: "DC Films & Shows",
  dune: "Dune",
  fast: "Fast & Furious",
  godzilla: "Godzilla",
  halloween: "Halloween",
  "hunger-games": "The Hunger Games",
  "indiana-jones": "Indiana Jones",
  jurassic: "Jurassic",
  "mad-max": "Mad Max",
  mcu: "Marvel Cinematic Universe",
  "middle-earth": "Middle-earth",
  predator: "Predator",
  "star-trek": "Star Trek",
  "star-wars": "Star Wars",
  terminator: "Terminator",
  transformers: "Transformers",
  "wizarding-world": "Wizarding World",
  "x-men": "X-Men",
};

const only = process.argv.slice(2).filter((a) => !a.startsWith("-"));
const ids = (only.length ? only : Object.keys(PAGES)).sort();

fs.mkdirSync(srcDir, { recursive: true });
fs.mkdirSync(outDir, { recursive: true });
fs.mkdirSync(assetDir, { recursive: true });
if (only.length) fs.mkdirSync(previewDir, { recursive: true });

let total = 0;
const sources = [];
for (const id of ids) {
  if (!PAGES[id]) throw new Error(`unknown franchise id: ${id}`);
  const src = await sourcePoster(id);
  const { w, h, bytes } = await compose(src.file, id);
  total += bytes;
  sources.push({ ...src, franchise: NAMES[id] });
  if (only.length) {
    await sharp(path.join(outDir, `${id}.webp`)).png().toFile(path.join(previewDir, `${id}.png`));
  }
  console.log(`${id.padEnd(16)} poster ${w}x${h}  ${(bytes / 1024).toFixed(1)} KB`);
}

if (!only.length) {
  writeAttribution(sources);
  console.log(`\nwrote ${sourcesPath} and POSTERS.md`);
}
console.log(`${ids.length} banners, ${(total / 1024 / 1024).toFixed(2)} MB total`);