# blog-stasis-src

A static blog built with [Stasis](https://github.com/magnars/stasis), a simple and flexible Clojure static site generator. Articles are written in Org-mode, exported to Markdown, and compiled into HTML.

## Requirements

- [Nix](https://nixos.org/) with flakes enabled
- Clojure
- Node.js (for Wrangler CLI)

## Getting Started

1. Enter the development environment:

   ```bash
   nix develop
   ```

2. Install Node.js dependencies:

   ```bash
   npm install
   ```

3. Start the development server:

   ```bash
   make dev
   ```

4. Open http://localhost:8080 in your browser.

## Project Structure

```
.
├── contents.org          # Blog articles (Org-mode source)
├── deps.edn              # Clojure dependencies
├── flake.nix             # Nix flake configuration
├── Makefile              # Development server and build tasks
├── package.json          # Node.js dependencies
├── src/
│   └── blog_clojure/
│       └── core.clj      # Main application logic
├── dev/
│   └── user.clj          # Development utilities
├── resources/
│   └── public/           # Static assets (images, etc.)
└── generated/
    ├── contents/         # Markdown files exported from Org-mode
    └── spectrum/         # Adobe Spectrum design tokens
```

## Writing Articles

1. Add a new top-level heading in `contents.org`
2. Set the required properties:
   - `export_title` - Article title
   - `export_file_name` - Output filename
   - `export_date` - Publication date
3. Export to Markdown from Emacs: `M-x my/stasis-export`

## Building

Generate static files in the `target/` directory:

```bash
make build
```

## Deployment

The site is the Cloudflare Worker `blog-stasis-src`, which serves `target/` as static assets on the custom domain s.conao3.com (`wrangler.toml`). Deploy by running the `deploy` workflow (`.github/workflows/deploy.yml`) on `master`; it runs `make build` and then `npx wrangler deploy`.

The Worker script (`worker/index.js`) runs before the static assets. A request whose `Accept` header names `text/markdown` at least as high as `text/html` gets the Markdown version of the page (`<path>.md`, built from `generated/contents/`) with `Vary: Accept`, and a missing page then returns `404.md` with status 404. HTML responses carry a `Link` header pointing to the Markdown version, `sitemap.xml` and `llms.txt`. The build also writes `robots.txt`, `sitemap.xml`, `llms.txt`, an Atom feed (`feed.xml`, the latest 30 posts), and JSON-LD (`WebSite` and `Person` on the top page, `BlogPosting` and `BreadcrumbList` on posts, `ProfilePage` on the portfolio). Every page has a canonical URL, OGP and Twitter card tags, and the portfolio pages link each other with `hreflang`. Headings in a post body start at `h2`, so the post title is the only `h1`.

The build reads the Markdown files committed under `generated/contents/`, so a post goes live only after its exported Markdown is committed. Posts and pages marked as drafts (`Draft: true` in the Markdown header, or `:export_draft: t` in `contents.org`) appear on the dev server but are left out of the build.

## Additional Commands

### Fetch Adobe Spectrum Tokens

Download design tokens to `resources/spectrum/`:

```bash
make fetch
```
