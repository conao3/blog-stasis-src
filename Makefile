all:

.PHONY: dev
dev:
	clojure -X blog-clojure.core/start-server

.PHONY: build
build:
	clojure -X blog-clojure.core/export

resources/spectrum:
	mkdir -p $@

resources/spectrum/%.json: resources/spectrum
	curl -L https://raw.githubusercontent.com/adobe/spectrum-tokens/refs/heads/main/packages/tokens/src/$*.json > $@

SPECTRUM_FILE += color-aliases.json
SPECTRUM_FILE += color-component.json
SPECTRUM_FILE += color-palette.json
SPECTRUM_FILE += icons.json
SPECTRUM_FILE += layout-component.json
SPECTRUM_FILE += layout.json
SPECTRUM_FILE += semantic-color-palette.json
SPECTRUM_FILE += typography.json

.PHONY: fetch
fetch: $(SPECTRUM_FILE:%=resources/specturm/%)

.PHONY: oss-map
oss-map:
	node scripts/oss-map-layout.mjs

.PHONY: rhythm
rhythm:
	node scripts/rhythm.mjs $(ID)

.PHONY: check-images
check-images:
	node scripts/check-images.mjs
