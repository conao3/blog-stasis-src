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
├── Makefile              # Build and deployment tasks
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

This project deploys to Cloudflare Pages.

### First-Time Setup

1. Create a [Cloudflare](https://cloudflare.com) account
2. Authenticate with Wrangler: `npx wrangler login`
3. The Pages project is created automatically on first deploy

### Deploy

```bash
make deploy
```

This runs `npx wrangler pages deploy` to upload the pre-built static files from `target/`.

## Additional Commands

### Fetch Adobe Spectrum Tokens

Download design tokens to `resources/spectrum/`:

```bash
make fetch
```
