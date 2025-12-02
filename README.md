# blog-stasis-src

A blog system using [Stasis](https://github.com/magnars/stasis), a Clojure static site generator.
Write articles in org-mode, export them to Markdown, and generate HTML.

## Requirements

- Nix (with flake support)
- Clojure
- Node.js (for wrangler)

## Setup

Enter the Nix environment:

```bash
nix develop
```

Install Node.js dependencies:

```bash
npm install
```

## Project Structure

```
.
├── contents.org          # Blog article source (org-mode)
├── deps.edn              # Clojure dependencies
├── flake.nix             # Nix flake configuration
├── Makefile              # Build and deploy tasks
├── package.json          # Node.js dependencies
├── src/
│   └── blog_clojure/
│       └── core.clj      # Main logic
├── dev/
│   └── user.clj          # Development utilities
├── resources/
│   └── public/           # Static files (images, etc.)
└── generated/
    ├── contents/         # Markdown exported from org-mode
    └── spectrum/         # Adobe Spectrum tokens
```

## Development

### Creating Articles

1. Add a new top-level heading to `contents.org`
2. Set properties: `export_title`, `export_file_name`, `export_date`
3. Export to Markdown in Emacs: `M-x my/stasis-export`

### Starting Development Server

```bash
make dev
```

Access the site at http://localhost:8080

## Build

Generate static files to the `target/` directory:

```bash
make build
```

## Deploy

Deploy to Cloudflare Pages:

```bash
make deploy
```

This command runs `npx wrangler pages deploy`.

### Initial Deploy Setup

1. Create a Cloudflare account
2. Login with wrangler: `npx wrangler login`
3. Create a Pages project (automatically created on first deploy)

### How Deployment Works

- `make build` generates static files to the `target/` directory
- `make deploy` uploads to Cloudflare Pages
- No build configuration needed (pre-built)

## Other

### Fetching Adobe Spectrum Tokens

Download design tokens:

```bash
make fetch
```

This command downloads Adobe Spectrum tokens to `resources/spectrum/`.
