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

The Cloudflare Pages project `blog-stasis-src` is connected to this repository. Pushing to `master` makes Cloudflare build the site and publish it at https://s.conao3.com/.

The build reads the Markdown files committed under `generated/contents/`, so a post goes live only after its exported Markdown is committed.

## Additional Commands

### Fetch Adobe Spectrum Tokens

Download design tokens to `resources/spectrum/`:

```bash
make fetch
```
