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

const card = ({ title, date, site, host }) =>
  element(
    "div",
    {
      backgroundColor: "rgb(29, 29, 29)",
      borderTop: "16px solid rgb(94, 170, 247)",
      boxSizing: "border-box",
      color: "rgb(235, 235, 235)",
      display: "flex",
      flexDirection: "column",
      fontFamily: "Noto Sans JP",
      height: "100%",
      justifyContent: "space-between",
      paddingBottom: 56,
      paddingLeft: 90,
      paddingRight: 90,
      paddingTop: 80,
      width: "100%",
    },
    [
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
