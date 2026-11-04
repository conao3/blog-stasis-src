import { createInterface } from "node:readline";
import { bundledLanguages, createHighlighter } from "shiki";

const theme = "github-dark-default";
const highlighter = await createHighlighter({ themes: [theme], langs: [] });
const transformers = [
  {
    pre(node) {
      node.properties.style = node.properties.style.replace(/background-color:[^;]*;?/, "");
    },
  },
];

for await (const line of createInterface({ input: process.stdin })) {
  const { lang, code } = JSON.parse(line);
  const known = Object.hasOwn(bundledLanguages, lang);
  if (known) await highlighter.loadLanguage(lang);
  const html = highlighter.codeToHtml(code, { lang: known ? lang : "text", theme, transformers });
  process.stdout.write(JSON.stringify({ html }) + "\n");
}
