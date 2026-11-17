# Portfolio project map

The English and Japanese portfolio pages include an interactive map after the existing open-source list. The map adds published projects beyond the resume's selected highlights; it does not change employment or personal information.

`resources/public/assets/oss-map.json` contains 44 projects and 15 documented relationships, collected on 2026-11-17. It was adapted from the relationship inventory in the private idea repository. Only public project names, descriptions, GitHub URLs and README-backed relationships are included. Groups are editorial categories, not dependency claims. Edge labels distinguish code generation, extensions, development tooling and validation.

`portfolio.clj` renders the headings, controls, JSON payload and a complete text fallback. `core.clj` includes the deferred `oss-map.js` only on pages containing the map. The script creates SVG nodes and connections using the site's Spectrum colors. It does not depend on a chat visualization renderer or external packages.

| Feature | Action | Expected result |
|---|---|---|
| Initial rendering | Open either portfolio page | 44 nodes, 15 edges; start with the Lean area; every node shows its name and a wrapping description, with height derived from its line count |
| Selection | Click/tap a project, or focus it and press Enter/Space | Highlight its connections and show descriptions, relationship labels and linked project names |
| Share selection | Select a project, then reload the URL containing `oss` | Restore the selection and bring it into view |
| Area | Select an area | Align that group at the top left; store the chosen area in `oss-area` |
| Zoom | Use +/−, Ctrl/Command-wheel or a two-pointer pinch | Change scale around the zoom center |
| Pan | Drag the map, or focus it and press arrow keys | Change translation; dragging must not select a project |
| Alignment | Initialize, fit, zoom out or change area | Anchor the map or selected area at the top left; panning cannot introduce blank space before the map origin |
| Overview | Click Fit all | Fit the complete map from the top-left corner of the canvas |
| Fullscreen | Click Fullscreen, then click again or press Escape | Fill the existing browser viewport with the map and controls; keep the map transform and block size unchanged on entry and exit; never call the native fullscreen API or resize the browser window |
| Viewport resize | Expand the map or resize the browser | Preserve the current pan and zoom, including after manually zooming or fitting all; only explicit zoom, fit, area or project navigation changes the camera |
| Script unavailable | Read the expandable text list | All 44 projects and outgoing relationships remain available without JavaScript |

Build with `nix develop -c make build`. Validate changes in a real browser, including node selection, zoom, drag and narrow layouts; JavaScript syntax checks alone do not establish that the map works in its delivery environment.
