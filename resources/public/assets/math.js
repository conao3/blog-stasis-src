const expandQty = (tex) => {
  let out = "";
  const closers = [];
  let depth = 0;
  for (let i = 0; i < tex.length; i++) {
    if (tex.startsWith("\\qty(", i)) {
      out += "\\left(";
      closers.push(depth);
      depth++;
      i += 4;
    } else if (tex[i] === "(") {
      out += "(";
      depth++;
    } else if (tex[i] === ")") {
      depth--;
      out += closers.at(-1) === depth ? (closers.pop(), "\\right)") : ")";
    } else {
      out += tex[i];
    }
  }
  return out;
};

document.addEventListener("DOMContentLoaded", () => {
  const elements = document.querySelectorAll(".math[data-tex]");
  if (elements.length === 0) return;
  const base = "https://cdn.jsdelivr.net/npm/katex@0.19.1/dist";
  const link = document.createElement("link");
  link.rel = "stylesheet";
  link.href = `${base}/katex.min.css`;
  document.head.append(link);
  const script = document.createElement("script");
  script.src = `${base}/katex.min.js`;
  script.onload = () => {
    for (const element of elements) {
      const bytes = Uint8Array.from(atob(element.dataset.tex), (c) => c.charCodeAt(0));
      katex.render(expandQty(new TextDecoder().decode(bytes)), element, {
        displayMode: element.classList.contains("display"),
        throwOnError: false,
      });
    }
  };
  document.head.append(script);
});
