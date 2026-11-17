# Portfolio project map

The English and Japanese portfolio pages include a map of 41 open-source projects and 15 relationships, placed after the open-source list. It adds published projects beyond the resume's selected highlights; it does not change employment or personal information.

## Data and layout

- `resources/oss-map/source.json` holds the hand-written data: five groups with their project IDs, a one-line Japanese and English description for every project, and the relationships as `[from, to, ja, en]`. Projects fully replaced by another repository (their README points to the repository that continues the work) are left out of the map and the open-source list. Every relationship stays within one group, and only relationships stated in the projects' READMEs are included.
- `make oss-map` (`scripts/oss-map-layout.mjs`, using `elkjs` from `devDependencies`) computes the layout and writes `resources/public/assets/oss-map.json`. Connected projects in each group are placed by ELK's layered algorithm from left to right with orthogonal edges and inline labels; unconnected projects follow in a grid. Groups are stacked top to bottom with a common width. Run it after editing `source.json` and commit both files.
- Blocks are 208 × 72 px. A description must fit in two lines in both languages; the browser check below catches overflow.

## Page

`portfolio.clj` renders the heading, lead paragraph, the area and zoom controls, the map frame, a hint, a complete list in `<details>` and the JSON. `core.clj` loads `oss-map.js` with `defer` only on pages containing the map. The script draws groups and arrows in one SVG and the projects as buttons on top of it, all inside one transformed layer. The frame has the width of the text column.

| Feature | Action | Expected result |
|---|---|---|
| Initial view | Open either portfolio page | 100%, the map's top-left corner at the frame's top-left; every block shows its name and description without clipping |
| Area | Click an area button | 100%, that group's top-left corner at the frame's top-left |
| Zoom | −/+ buttons, `-`/`+` keys on the focused map, Ctrl/⌘ + wheel, pinch | Scale changes around the centre or the pointer; the percentage updates |
| Fit | Click Fit or press `0` | The whole map from the top-left corner |
| Pan | Wheel or trackpad scroll over the map, drag, or arrow keys on the focused map | The map moves; it never leaves blank space before its top-left corner; a wheel scroll stops at the map's edges and then scrolls the page; a drag does not select |
| Selection | Click a block, or Tab to it and press Enter | The block and its arrows are highlighted, other blocks dim, and the panel below the map shows the description, each relationship with links to the other projects, and the GitHub link; `?oss=<id>` is written to the URL |
| Clear selection | Click the panel's ×, click empty space, or press Escape | Highlight and panel disappear; `oss` is removed from the URL |
| Restore | Open a URL with `?oss=<id>`, or go back/forward | The project is selected and brought into view at 100% |
| Full view | Click Full view; click again or press Escape | The frame fills the browser window without resizing the window; scale and position stay the same on entry and exit |
| Narrow screens | 390 px width | No horizontal page scroll; the panel follows the map |
| Without JavaScript | Open the list | All projects with descriptions and outgoing relationships |

Check these in a real browser after any change, including the browser console.
