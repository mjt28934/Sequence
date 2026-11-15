import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";

const root = "C:/Projects/Orderly/buildtools";
const srcPath = "C:/Users/MJT/AppData/Local/Temp/opencode/seq_apk/unzipped/assets/www/app.js";
const raw = fs.readFileSync(srcPath, "utf8");
const marker = "/* Sequence for Android: the app shell.";
const cut = raw.indexOf(marker);
if (cut < 0) throw new Error("app shell marker not found");
const core = raw.slice(0, cut) + "\nglobalThis.__SEQ = __m;\n";
const context = { globalThis: null, console, Math, Date, JSON, Object, Array, String, Number, RegExp, Map, Set, encodeURIComponent };
context.globalThis = context;
vm.createContext(context);
vm.runInContext(core, context);
const m = context.__SEQ;
const F = m.franchises;
const W = m.works;
const A = m.art;

const franchises = F.franchises.map((f) => ({ ...f, entries: f.entries.map((e) => ({ ...e })) }));
const works = W.WORKS;
const accents = {};
for (const f of franchises) accents[f.id] = A.accentTokens(f.accent);

const data = {
  version: "1.3",
  dataAsOf: F.DATA_AS_OF,
  genres: F.GENRES,
  franchises,
  works,
  accents,
};
fs.mkdirSync(path.join(root, "out"), { recursive: true });
fs.writeFileSync(path.join(root, "out", "sequence.json"), JSON.stringify(data, null, 2));

// Art
const svgDir = path.join(root, "svg");
fs.mkdirSync(path.join(svgDir, "posters"), { recursive: true });
fs.mkdirSync(path.join(svgDir, "covers"), { recursive: true });
for (const f of franchises) {
  fs.writeFileSync(path.join(svgDir, "posters", `${f.id}.svg`), A.posterSvg(f));
  for (const w of W.worksFor(f.id)) {
    fs.writeFileSync(path.join(svgDir, "covers", `${w.id}.svg`), A.coverSvg(w, f.accent));
  }
}

let entries = 0;
let workCount = 0;
for (const f of franchises) { entries += f.entries.length; workCount += W.worksFor(f.id).length; }
console.log(JSON.stringify({
  franchises: franchises.length,
  entries,
  works: workCount,
  posters: franchises.length,
  genres: F.GENRES,
  sampleAccent: accents[franchises[0].id],
}, null, 2));
