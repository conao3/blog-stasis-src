import { writeFileSync } from "node:fs";
import { mathjax } from "mathjax-full/js/mathjax.js";
import { TeX } from "mathjax-full/js/input/tex.js";
import { SVG } from "mathjax-full/js/output/svg.js";
import { liteAdaptor } from "mathjax-full/js/adaptors/liteAdaptor.js";
import { RegisterHTMLHandler } from "mathjax-full/js/handlers/html.js";
import { AllPackages } from "mathjax-full/js/input/tex/AllPackages.js";

const adaptor = liteAdaptor();
RegisterHTMLHandler(adaptor);
const doc = mathjax.document("", {
  InputJax: new TeX({ packages: AllPackages }),
  OutputJax: new SVG({ fontCache: "none" }),
});

const EX = 8.6;
const INK = "#222";
const MUTED = "#5b6570";
const LINE = "#8a96a3";
const SANS = "'Noto Sans JP', sans-serif";
const MONO = "'Noto Sans Mono CJK JP', monospace";

function tex(src, color = INK) {
  const node = doc.convert(src, { display: false });
  const svg = adaptor.firstChild(node);
  const w = parseFloat(adaptor.getAttribute(svg, "width")) * EX;
  const h = parseFloat(adaptor.getAttribute(svg, "height")) * EX;
  const style = adaptor.getAttribute(svg, "style") || "";
  const valign = parseFloat((style.match(/vertical-align:\s*(-?[\d.]+)ex/) || [0, 0])[1]) * EX;
  const viewBox = adaptor.getAttribute(svg, "viewBox");
  const inner = adaptor.innerHTML(svg).replaceAll("currentColor", color);
  return {
    w, h, ascent: h + valign,
    place(x, baseline) {
      const y = baseline - (h + valign);
      return `<svg x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${w.toFixed(1)}" height="${h.toFixed(1)}" viewBox="${viewBox}" overflow="visible">${inner}</svg>`;
    },
  };
}

function row(pieces, gap) {
  let x = 0;
  return pieces.map((p) => {
    const item = { ...p, x0: x };
    x += p.m.w + (p.gap ?? gap);
    item.x1 = item.x0 + p.m.w;
    return item;
  });
}

function width(items) {
  const last = items[items.length - 1];
  return last.x1;
}

const out = [];
const W = 1100;
const PAD = 36;

// Part 1: one table row, drawn as the post draws it.
const fn = { label: "take-while" };
const argsPieces = row([
  { id: "p", m: tex("p : x \\to \\text{bool}"), gap: 4 },
  { id: "comma", m: tex(","), gap: 12 },
  { id: "open", m: tex("("), gap: 2 },
  { id: "a0", m: tex("a_0") },
  { id: "dots1", m: tex("\\ldots") },
  { id: "aim1", m: tex("a_{i-1}") },
  { id: "ai", m: tex("a_i") },
  { id: "rest", m: tex("\\underline{a_{i+1}\\ \\ldots}"), gap: 2 },
  { id: "close", m: tex(")") },
], 9);
const resultPieces = row([
  { id: "out", m: tex("(a_0\\ \\ldots\\ a_{i-1})"), gap: 18 },
  { id: "wh", m: tex("\\text{wh.}"), gap: 8 },
  { id: "cond", m: tex("i\\text{ is the first index with }(p\\ a_i) = \\mathtt{false}") },
], 8);

const fnW = 136;
const argsW = width(argsPieces) + 34;
const resW = width(resultPieces) + 34;
const tableW = fnW + argsW + resW;
const tableX = (W - tableW) / 2;
const headY = 92;
const headH = 40;
const bodyH = 64;
const bodyBase = headY + headH + 40;

out.push(`<text x="${PAD}" y="56" font-size="22" font-weight="bold" fill="${INK}">How to read a row</text>`);

const cols = [
  { x: tableX, w: fnW, title: "Function" },
  { x: tableX + fnW, w: argsW, title: "Arguments" },
  { x: tableX + fnW + argsW, w: resW, title: "Result" },
];
for (const c of cols) {
  out.push(`<rect x="${c.x}" y="${headY}" width="${c.w}" height="${headH}" fill="#eef1f4" stroke="${LINE}"/>`);
  out.push(`<text x="${c.x + 16}" y="${headY + 27}" font-size="17" font-weight="bold" fill="${INK}">${c.title}</text>`);
  out.push(`<rect x="${c.x}" y="${headY + headH}" width="${c.w}" height="${bodyH}" fill="#fff" stroke="${LINE}"/>`);
}
out.push(`<text x="${cols[0].x + 16}" y="${bodyBase}" font-size="17" font-family="${MONO}" fill="#2f5f9e">${fn.label}</text>`);
const argsX = cols[1].x + 17;
for (const p of argsPieces) out.push(p.m.place(argsX + p.x0, bodyBase));
const resX = cols[2].x + 17;
for (const p of resultPieces) out.push(p.m.place(resX + p.x0, bodyBase));

// Callouts under the row.
const tableBottom = headY + headH + bodyH;
const mid = (id, pieces, x) => { const p = pieces.find((q) => q.id === id); return x + (p.x0 + p.x1) / 2; };
const callouts = [
  { at: argsX + argsPieces[0].x0 + 6, anchor: "end", text: ["p is a predicate:", "its result is true or false"] },
  { at: mid("ai", argsPieces, argsX), anchor: "end", text: ["the first element", "for which p is false"] },
  { at: mid("rest", argsPieces, argsX), anchor: "start", text: ["underlined elements", "are never realized"] },
  { at: mid("wh", resultPieces, resX), anchor: "start", text: ["\u201cwh.\u201d reads \u201cwhere\u201d"] },
];
const calloutY = tableBottom + 52;
for (const c of callouts) {
  out.push(`<line x1="${c.at}" y1="${tableBottom - 7}" x2="${c.at}" y2="${calloutY - 6}" stroke="${MUTED}" stroke-width="1.5"/>`);
  out.push(`<circle cx="${c.at}" cy="${tableBottom - 7}" r="3.5" fill="${MUTED}"/>`);
  const tx = c.anchor === "end" ? c.at - 8 : c.anchor === "start" ? c.at + 8 : c.at;
  const ty0 = calloutY;
  c.text.forEach((t, k) => {
    out.push(`<text x="${tx}" y="${ty0 + k * 22}" text-anchor="${c.anchor}" font-size="16" fill="${MUTED}">${t}</text>`);
  });
}
const levelY = [calloutY, calloutY + 22];

// Part 2: a concrete call.
const exTop = levelY[1] + 78;
out.push(`<line x1="${PAD}" y1="${exTop - 30}" x2="${W - PAD}" y2="${exTop - 30}" stroke="#d5dbe1"/>`);
out.push(`<text x="${PAD}" y="${exTop + 8}" font-size="22" font-weight="bold" fill="${INK}">The same row on a real call</text>`);
const codeY = exTop + 52;
out.push(`<text x="${PAD}" y="${codeY}" font-size="19" font-family="${MONO}" fill="${INK}">(take-while #(&lt; % 3) (range))  <tspan fill="${MUTED}">;=&gt;</tspan> (0 1 2)</text>`);

const kinds = {
  kept: { fill: "#e3eefb", stroke: "#3b6fb6", text: INK, dash: "" },
  tested: { fill: "#fdeedd", stroke: "#c8782c", text: INK, dash: "" },
  skipped: { fill: "#fff", stroke: "#a5adb6", text: "#9aa3ad", dash: ' stroke-dasharray="6 4"' },
};
const cells = [
  ["0", "a_0", "kept"], ["1", "a_1", "kept"], ["2", "a_2", "kept"],
  ["3", "a_3 = a_i", "tested"], ["4", "", "skipped"], ["5", "", "skipped"], ["6", "", "skipped"], ["…", "", "skipped"],
];
const cellW = 64;
const cellH = 52;
const cellGap = 10;
const tapeX = PAD;
const tapeY = codeY + 30;
cells.forEach(([v, sub, kind], k) => {
  const s = kinds[kind];
  const x = tapeX + k * (cellW + cellGap);
  out.push(`<rect x="${x}" y="${tapeY}" width="${cellW}" height="${cellH}" rx="8" fill="${s.fill}" stroke="${s.stroke}" stroke-width="2"${s.dash}/>`);
  out.push(`<text x="${x + cellW / 2}" y="${tapeY + 34}" text-anchor="middle" font-size="21" font-family="${MONO}" fill="${s.text}">${v}</text>`);
  if (sub) {
    const m = tex(sub, MUTED);
    out.push(m.place(x + cellW / 2 - m.w / 2, tapeY + cellH + 28));
  }
});

const legendX = tapeX + cells.length * (cellW + cellGap) + 40;
const legend = [
  ["kept", "in the result, (p a) is true"],
  ["tested", "realized to test p, not in the result"],
  ["skipped", "never realized"],
];
legend.forEach(([kind, label], k) => {
  const s = kinds[kind];
  const y = tapeY - 6 + k * 34;
  out.push(`<rect x="${legendX}" y="${y}" width="22" height="22" rx="5" fill="${s.fill}" stroke="${s.stroke}" stroke-width="2"${s.dash}/>`);
  out.push(`<text x="${legendX + 34}" y="${y + 17}" font-size="16" fill="${INK}">${label}</text>`);
});

const H = tapeY + cellH + 66;
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" font-family="${SANS}">
<rect x="0" y="0" width="${W}" height="${H}" rx="16" fill="#fff"/>
${out.join("\n")}
</svg>
`;
writeFileSync(process.argv[2], svg);
console.log(W, H, Math.round(tableW));
