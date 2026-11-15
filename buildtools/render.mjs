import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";

const root = "C:/Projects/Orderly/buildtools";
const out = path.join(root, "images");
fs.mkdirSync(path.join(out, "posters"), { recursive: true });
fs.mkdirSync(path.join(out, "covers"), { recursive: true });

async function convert(src, dest, width, height) {
  await sharp(fs.readFileSync(src), { density: 96 })
    .resize(width, height, { fit: "fill" })
    .webp({ quality: 84, effort: 5 })
    .toFile(dest);
}

for (const f of fs.readdirSync(path.join(root, "svg", "posters"))) {
  await convert(path.join(root, "svg", "posters", f), path.join(out, "posters", f.replace(/\.svg$/, ".webp")), 640, 400);
}
for (const f of fs.readdirSync(path.join(root, "svg", "covers"))) {
  await convert(path.join(root, "svg", "covers", f), path.join(out, "covers", f.replace(/\.svg$/, ".webp")), 300, 450);
}
console.log("done");
