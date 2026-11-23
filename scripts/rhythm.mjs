import { readFileSync } from "node:fs";

const id = process.argv[2];
if (!id) {
  console.error("usage: node scripts/rhythm.mjs <article ID> [org file]");
  process.exit(2);
}

const source = readFileSync(process.argv[3] ?? "contents.org", "utf8");
const article = source
  .split(/\n(?=\* )/)
  .find((a) => a.includes(`:export_file_name: generated/contents/blog/`) && a.includes(`/${id}\n`));
if (!article) {
  console.error(`no article with ID ${id} in contents.org`);
  process.exit(2);
}

const lines = [];
let inBlock = false;
for (const line of article.split("\n").slice(1)) {
  if (line.startsWith("#+begin")) {
    inBlock = true;
    continue;
  }
  if (line.startsWith("#+end")) {
    inBlock = false;
    continue;
  }
  const skip = inBlock || /^(:|#|\||\*|-|\[\[)/.test(line) || line.trim() === "";
  lines.push(skip ? "" : line);
}
const body = lines.join("\n");
const paragraphs = body.split(/\n\s*\n/).filter((p) => p.trim());
const sentences = body
  .replace(/\n/g, "")
  .split(/(?<=[。！？!?])/)
  .filter((s) => s.trim().length > 1);
const lengths = sentences.map((s) => s.length);
const mean = lengths.reduce((a, b) => a + b, 0) / lengths.length;
const sd = Math.sqrt(lengths.reduce((a, b) => a + (b - mean) ** 2, 0) / lengths.length);
const cv = sd / mean;
const perParagraph = sentences.length / paragraphs.length;
const commas = (sentences.join("").match(/、/g) ?? []).length;
const plain = sentences.join("").replace(/\[\[[^\]]*\]\[([^\]]*)\]\]/g, "$1").replace(/\[\[[^\]]*\]\]/g, "");
const commaRate = (commas / plain.length) * 100;
const clefts = sentences.filter((s) => /のは、.*(です|でした|だけ)/.test(s));

const failures = [];
if (perParagraph > 2.0) failures.push(`sentences per paragraph ${perParagraph.toFixed(1)} > 2.0`);
if (cv < 0.45) failures.push(`sentence length sd/mean ${cv.toFixed(2)} < 0.45`);
if (commaRate > 2.0) failures.push(`読点 per 100 characters ${commaRate.toFixed(2)} > 2.0`);
if (clefts.length > 1) failures.push(`${clefts.length} sentences of the form 〜のは、〜です > 1`);

console.log(
  `${id}: ${sentences.length} sentences, ${paragraphs.length} paragraphs, ` +
    `${perParagraph.toFixed(1)} per paragraph, mean length ${mean.toFixed(1)}, sd/mean ${cv.toFixed(2)}, ` +
    `${commaRate.toFixed(2)} 読点 per 100 characters, ${clefts.length} 〜のは、〜です`,
);
for (const s of clefts) console.log(`  のは: ${s}`);
if (failures.length) {
  for (const f of failures) console.log(`FAIL: ${f}`);
  process.exit(1);
}
