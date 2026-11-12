const markdownType = "text/markdown; charset=utf-8";

function parseAccept(accept) {
  return accept.split(",").map((part) => {
    const [range, ...params] = part.trim().toLowerCase().split(";").map((s) => s.trim());
    const q = params.find((p) => p.startsWith("q="));
    return { range, quality: q ? Number(q.slice(2)) : 1 };
  });
}

function acceptQuality(entries, type) {
  const [major] = type.split("/");
  const match = (range) => entries.find((entry) => entry.range === range);
  return (match(type) ?? match(`${major}/*`) ?? match("*/*"))?.quality ?? 0;
}

function prefersMarkdown(request) {
  const accept = request.headers.get("Accept");
  if (!accept) return false;
  const entries = parseAccept(accept);
  const markdown = entries.find((entry) => entry.range === "text/markdown")?.quality ?? 0;
  return markdown > 0 && markdown >= acceptQuality(entries, "text/html");
}

function markdownPath(pathname) {
  if (pathname === "/") return "/index.md";
  if (/\.[^/]*$/.test(pathname)) return null;
  return `${pathname.replace(/\/$/, "")}.md`;
}

function withHeaders(response, headers) {
  const result = new Response(response.body, response);
  for (const [name, value] of Object.entries(headers)) {
    if (name === "Vary") {
      const vary = result.headers.get("Vary");
      if (!vary || !/\baccept\b/i.test(vary)) result.headers.set("Vary", vary ? `${vary}, Accept` : "Accept");
    } else {
      result.headers.set(name, value);
    }
  }
  return result;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const mdPath = markdownPath(url.pathname);
    const sitemapLink = `<${url.origin}/sitemap.xml>; rel="sitemap"; type="application/xml"`;
    const llmsLink = `<${url.origin}/llms.txt>; rel="describedby"; type="text/plain"`;

    if (mdPath && prefersMarkdown(request)) {
      const markdown = await env.ASSETS.fetch(new Request(new URL(mdPath, url), request));
      if (markdown.status === 200) {
        return withHeaders(markdown, {
          "Content-Type": markdownType,
          Vary: "Accept",
          Link: `<${url.href}>; rel="canonical"; type="text/html", ${sitemapLink}`,
        });
      }
      const html = await env.ASSETS.fetch(request);
      if (html.status === 404) {
        const notFound = await env.ASSETS.fetch(new Request(new URL("/404.md", url), request));
        return new Response(request.method === "HEAD" ? null : notFound.body, {
          status: 404,
          headers: { "Content-Type": markdownType, Vary: "Accept", Link: sitemapLink },
        });
      }
      return withHeaders(html, { Vary: "Accept", Link: `${sitemapLink}, ${llmsLink}` });
    }

    const response = await env.ASSETS.fetch(request);
    if (!mdPath) return response;
    const links = [sitemapLink, llmsLink];
    if (response.status === 200 && (url.pathname === "/" || url.pathname.startsWith("/blog/"))) {
      links.unshift(`<${url.origin}${mdPath}>; rel="alternate"; type="text/markdown"`);
    }
    return withHeaders(response, { Vary: "Accept", Link: links.join(", ") });
  },
};
