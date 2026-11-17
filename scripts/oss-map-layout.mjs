import ELK from 'elkjs/lib/elk.bundled.js';
import { readFileSync, writeFileSync } from 'node:fs';

const source = JSON.parse(readFileSync('resources/oss-map/source.json', 'utf8'));
const output = 'resources/public/assets/oss-map.json';
const NODE_WIDTH = 208, NODE_HEIGHT = 72, GAP = 16, PADDING = 20, HEADER = 48, LABEL_HEIGHT = 18, MAX_WIDTH = 1140;

const textWidth = text => [...text].reduce((width, c) => width + (c.codePointAt(0) > 0x2e80 ? 12 : 6.8), 0);
const edges = source.edges.map(([from, to, kind, ja, en]) => ({ from, to, kind, ja, en }));
const ids = source.groups.flatMap(g => g.nodes);
for (const id of ids) if (!source.nodes[id]) throw new Error(`missing description: ${id}`);
for (const id of Object.keys(source.nodes)) if (!ids.includes(id)) throw new Error(`node without group: ${id}`);
for (const e of edges) {
  const group = id => source.groups.findIndex(g => g.nodes.includes(id));
  if (group(e.from) < 0 || group(e.from) !== group(e.to)) throw new Error(`edge must stay within one group: ${e.from} -> ${e.to}`);
}

const elk = new ELK();
const groups = [];
for (const group of source.groups) {
  const linked = group.nodes.filter(id => edges.some(e => e.from === id || e.to === id));
  const single = group.nodes.filter(id => !linked.includes(id));
  const graph = linked.length ? await elk.layout({
    id: 'group',
    layoutOptions: {
      'elk.algorithm': 'layered',
      'elk.direction': 'RIGHT',
      'elk.edgeRouting': 'ORTHOGONAL',
      'elk.padding': '[top=0,left=0,bottom=0,right=0]',
      'elk.spacing.nodeNode': String(GAP),
      'elk.layered.spacing.nodeNodeBetweenLayers': '28',
      'elk.layered.spacing.edgeNodeBetweenLayers': '12',
      'elk.spacing.edgeLabel': '2',
      'elk.edgeLabels.inline': 'true',
      'elk.separateConnectedComponents': 'true',
      'elk.aspectRatio': '0.2',
      'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
    },
    children: linked.map(id => ({ id, width: NODE_WIDTH, height: NODE_HEIGHT })),
    edges: edges.flatMap((e, i) => linked.includes(e.from) ? [{
      id: String(i), sources: [e.from], targets: [e.to],
      labels: [{ text: e.ja, width: Math.max(textWidth(e.ja), textWidth(e.en)) + 8, height: LABEL_HEIGHT }],
    }] : []),
  }) : { width: 0, height: 0, children: [], edges: [] };
  const columns = Math.floor((MAX_WIDTH - 2 * PADDING + GAP) / (NODE_WIDTH + GAP));
  const top = graph.height ? graph.height + 24 : 0;
  const nodes = graph.children.map(n => ({ id: n.id, x: n.x, y: n.y }));
  single.forEach((id, k) => nodes.push({ id, x: (k % columns) * (NODE_WIDTH + GAP), y: top + Math.floor(k / columns) * (NODE_HEIGHT + GAP) }));
  const rows = Math.ceil(single.length / columns);
  groups.push({
    ja: group.ja, en: group.en, nodes,
    width: Math.max(graph.width, Math.min(single.length, columns) * (NODE_WIDTH + GAP) - GAP) + 2 * PADDING,
    height: top + (rows ? rows * (NODE_HEIGHT + GAP) - GAP : 0) + HEADER + PADDING,
    edges: graph.edges.map(e => ({
      index: Number(e.id),
      points: e.sections.flatMap(s => [s.startPoint, ...(s.bendPoints ?? []), s.endPoint]),
      label: { x: e.labels[0].x + e.labels[0].width / 2, y: e.labels[0].y + e.labels[0].height / 2 },
    })),
  });
}

const width = Math.max(...groups.map(g => g.width));
const round = n => Math.round(n * 10) / 10;
const map = { width, height: 0, nodeWidth: NODE_WIDTH, nodeHeight: NODE_HEIGHT, groups: [], nodes: [], edges: [] };
let y = 0;
for (const group of groups) {
  const ox = PADDING, oy = y + HEADER;
  map.groups.push({ ja: group.ja, en: group.en, x: 0, y, width, height: group.height });
  for (const n of group.nodes) {
    const [ja, en] = source.nodes[n.id];
    map.nodes.push({ id: n.id, group: map.groups.length - 1, ja, en, url: `https://github.com/conao3/${n.id}`, x: round(ox + n.x), y: round(oy + n.y) });
  }
  for (const e of group.edges) {
    map.edges[e.index] = { ...edges[e.index], points: e.points.map(p => [round(ox + p.x), round(oy + p.y)]), label: [round(ox + e.label.x), round(oy + e.label.y)] };
  }
  y += group.height + GAP;
}
map.height = y - GAP;
writeFileSync(output, JSON.stringify(map, null, 1) + '\n');
console.log(`${output}: ${map.nodes.length} projects, ${map.edges.length} relationships, ${map.width}x${map.height}`);
