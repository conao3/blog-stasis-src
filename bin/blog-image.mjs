import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { createInterface } from "node:readline";
import { Resvg } from "@resvg/resvg-js";
import satori from "satori";

const require = createRequire(import.meta.url);
const font = await readFile(require.resolve("@expo-google-fonts/noto-sans-jp/700Bold/NotoSansJP_700Bold.ttf"));
const colors = { background: "#0b1220", title: "#f8fafc", forward: "#38bdf8", reverse: "#34d399" };
const element = (type, style, children) => ({ type, props: { style, children } });
const arrows = `<svg xmlns="http://www.w3.org/2000/svg" width="1000" height="200" viewBox="-10 -10 1020 200"><path d="M20 42H968M918 4L972 42L918 80" fill="none" stroke="${colors.forward}" stroke-width="20" stroke-linecap="round" stroke-linejoin="round"/><path d="M980 138H32M82 100L28 138L82 176" fill="none" stroke="${colors.reverse}" stroke-width="20" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

for await (const line of createInterface({ input: process.stdin })) {
  const { titles } = JSON.parse(line);
  if (!Array.isArray(titles) || titles.length < 1 || titles.length > 2 || titles.some((title) => typeof title !== "string" || !title.trim())) {
    throw new Error("Provide one or two nonempty title lines in titles");
  }
  const tree = element("div", {
    display: "flex", flexDirection: "column", boxSizing: "border-box", position: "relative",
    backgroundColor: colors.background, color: colors.title, width: "100%", height: "100%",
    padding: "80px 100px 65px", fontFamily: "Noto Sans JP", fontSize: 88, fontWeight: 700,
  }, [
    element("div", { display: "flex", flexDirection: "column", alignItems: "center", width: "100%", lineHeight: 1.3 },
      titles.map((title) => element("div", { display: "flex", whiteSpace: "nowrap" }, title))),
    { type: "img", props: { src: `data:image/svg+xml;base64,${Buffer.from(arrows).toString("base64")}`, width: 1000, height: 200, style: { position: "absolute", left: 100, top: 385 } } },
  ]);
  const svg = await satori(tree, { width: 1200, height: 630, fonts: [{ name: "Noto Sans JP", data: font, weight: 700 }] });
  const png = new Resvg(svg).render().asPng();
  process.stdout.write(JSON.stringify({ svg, png: Buffer.from(png).toString("base64") }) + "\n");
}
