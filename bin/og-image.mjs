import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { createInterface } from "node:readline";
import { Resvg } from "@resvg/resvg-js";
import satori from "satori";

const require = createRequire(import.meta.url);
const font = (file) => readFile(require.resolve(`@expo-google-fonts/noto-sans-jp/${file}`));
const fonts = [
  { name: "Noto Sans JP", data: await font("400Regular/NotoSansJP_400Regular.ttf"), weight: 400 },
  { name: "Noto Sans JP", data: await font("700Bold/NotoSansJP_700Bold.ttf"), weight: 700 },
];

const icon = `data:image/jpeg;base64,${(await readFile("resources/public/assets/icon.jpg")).toString("base64")}`;

const element = (type, style, children) => ({ type, props: { style, children } });

const frameColors = ["#2BB59A", "#1C8FBF", "#3563D9"];

const frameSvg = ({ width, height, flip }) => {
  const thickness = 38;
  const radius = 40;
  const path = `M 0 0 H ${width} V ${thickness} H ${thickness + radius} A ${radius} ${radius} 0 0 0 ${thickness} ${thickness + radius} V ${height} H 0 Z`;
  const stops = [
    `<stop offset="0" stop-color="${frameColors[0]}" stop-opacity="0"/>`,
    `<stop offset="0.14" stop-color="${frameColors[0]}"/>`,
    `<stop offset="0.5" stop-color="${frameColors[1]}"/>`,
    `<stop offset="0.86" stop-color="${frameColors[2]}"/>`,
    `<stop offset="1" stop-color="${frameColors[2]}" stop-opacity="0"/>`,
  ].join("");
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}"><defs><linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="${height}" x2="${width}" y2="0">${stops}</linearGradient></defs><path d="${path}" fill="url(#g)"${flip ? ` transform="rotate(180 ${width / 2} ${height / 2})"` : ""}/></svg>`;
  return `data:image/svg+xml;base64,${Buffer.from(svg).toString("base64")}`;
};

const frame = (position, width, height, flip) => ({
  type: "img",
  props: { src: frameSvg({ width, height, flip }), width, height, style: { position: "absolute", ...position } },
});

const frames = [frame({ left: 0, top: 0 }, 760, 420, false), frame({ right: 0, bottom: 0 }, 400, 220, true)];

const card = ({ title, date, site, host }) =>
  element(
    "div",
    {
      backgroundColor: "rgb(29, 29, 29)",
      boxSizing: "border-box",
      color: "rgb(235, 235, 235)",
      display: "flex",
      flexDirection: "column",
      fontFamily: "Noto Sans JP",
      height: "100%",
      justifyContent: "space-between",
      paddingBottom: 72,
      paddingLeft: 96,
      paddingRight: 96,
      paddingTop: 88,
      position: "relative",
      width: "100%",
    },
    [
      ...frames,
      element(
        "div",
        {
          color: "rgb(255, 255, 255)",
          display: "block",
          fontSize: title.length > 40 ? 60 : 68,
          fontWeight: 700,
          lineClamp: 4,
          lineHeight: 1.45,
          wordBreak: "break-word",
        },
        title,
      ),
      element(
        "div",
        { alignItems: "flex-end", display: "flex", fontSize: 36, justifyContent: "space-between", width: "100%" },
        [
          element("div", { alignItems: "center", display: "flex", gap: 28 }, [
            { type: "img", props: { src: icon, width: 96, height: 96, style: { borderRadius: 48 } } },
            element("div", { display: "flex", flexDirection: "column" }, [
              element("div", { fontSize: 40, fontWeight: 700 }, "conao3"),
              element("div", { color: "rgb(176, 176, 176)", fontSize: 30 }, date ?? ""),
            ]),
          ]),
          element("div", { display: "flex", flexDirection: "column", alignItems: "flex-end" }, [
            element("div", { fontSize: 44, fontWeight: 700 }, site),
            element("div", { color: "rgb(176, 176, 176)", fontSize: 30 }, host),
          ]),
        ],
      ),
    ],
  );

for await (const line of createInterface({ input: process.stdin })) {
  const svg = await satori(card(JSON.parse(line)), { width: 1200, height: 630, fonts });
  const png = new Resvg(svg).render().asPng();
  process.stdout.write(JSON.stringify({ png: Buffer.from(png).toString("base64") }) + "\n");
}
