// Deterministic springs, group attraction and rectangular collision constraints.
// Settle once so reading, panning and expanding the viewport never move the graph.
export function forceLayout(input, edges) {
  const nodes = input.map((n, i) => {
    const peers = input.filter(p => p.groupIndex === n.groupIndex);
    const index = peers.findIndex(p => p.id === n.id);
    const gx = 800 + (n.groupIndex % 3) * 1650;
    const gy = 650 + Math.floor(n.groupIndex / 3) * 1500;
    return { ...n, gx, gy, cx: gx + (index % 2 - 0.5) * 620 + Math.sin(i * 19) * 16,
      cy: gy + (Math.floor(index / 2) - (Math.ceil(peers.length / 2) - 1) / 2) * 220,
      vx: 0, vy: 0 };
  });
  const byId = new Map(nodes.map(n => [n.id, n]));
  const links = edges.map(([a, b]) => [byId.get(a), byId.get(b)]);
  for (let tick = 0; tick < 550; tick++) {
    const alpha = Math.max(0.06, 1 - tick / 550);
    for (const n of nodes) {
      n.vx += (n.gx - n.cx) * 0.003 * alpha;
      n.vy += (n.gy - n.cy) * 0.003 * alpha;
    }
    for (const [a, b] of links) {
      const dx = b.cx - a.cx, dy = b.cy - a.cy;
      const distance = Math.max(1, Math.hypot(dx, dy));
      const ux = dx / distance, uy = dy / distance;
      const radius = n => 1 / Math.max(Math.abs(ux) / (n.width / 2), Math.abs(uy) / (n.height / 2));
      const strength = a.groupIndex === b.groupIndex ? 0.018 : 0.0008;
      const force = (distance - radius(a) - radius(b) - 110) * strength * alpha;
      a.vx += ux * force; a.vy += uy * force;
      b.vx -= ux * force; b.vy -= uy * force;
    }
    for (let i = 0; i < nodes.length; i++) for (let j = i + 1; j < nodes.length; j++) {
      const a = nodes[i], b = nodes[j];
      const dx = b.cx - a.cx || 0.01, dy = b.cy - a.cy || 0.01;
      const d = Math.max(1, Math.hypot(dx, dy));
      if (d < 1200) {
        const force = Math.min(4, 50000 / (d * d)) * alpha;
        a.vx -= dx / d * force; a.vy -= dy / d * force;
        b.vx += dx / d * force; b.vy += dy / d * force;
      }
      const ox = (a.width + b.width) / 2 + 52 - Math.abs(dx);
      const oy = (a.height + b.height) / 2 + 52 - Math.abs(dy);
      if (ox > 0 && oy > 0) {
        if (ox < oy) {
          const push = Math.sign(dx) * ox * 0.25;
          a.vx -= push; b.vx += push;
        } else {
          const push = Math.sign(dy) * oy * 0.25;
          a.vy -= push; b.vy += push;
        }
      }
    }
    for (const n of nodes) {
      n.vx *= 0.65; n.vy *= 0.65;
      n.cx += Math.max(-25, Math.min(25, n.vx));
      n.cy += Math.max(-25, Math.min(25, n.vy));
    }
  }
  // Project the final positions onto non-overlapping rectangles, including padding.
  for (let pass = 0; pass < 200; pass++) {
    let overlaps = 0;
    for (let i = 0; i < nodes.length; i++) for (let j = i + 1; j < nodes.length; j++) {
      const a = nodes[i], b = nodes[j], dx = b.cx - a.cx || 0.01, dy = b.cy - a.cy || 0.01;
      const ox = (a.width + b.width) / 2 + 40 - Math.abs(dx);
      const oy = (a.height + b.height) / 2 + 40 - Math.abs(dy);
      if (ox <= 0 || oy <= 0) continue;
      overlaps++;
      if (ox < oy) { const p = Math.sign(dx) * (ox / 2 + 0.01); a.cx -= p; b.cx += p; }
      else { const p = Math.sign(dy) * (oy / 2 + 0.01); a.cy -= p; b.cy += p; }
    }
    if (!overlaps) break;
  }
  const minX = Math.min(...nodes.map(n => n.cx - n.width / 2));
  const minY = Math.min(...nodes.map(n => n.cy - n.height / 2));
  for (const n of nodes) { n.x = n.cx - n.width / 2 - minX + 24; n.y = n.cy - n.height / 2 - minY + 90; }
  const groups = Array.from({ length: 6 }, (_, i) => {
    const peers = nodes.filter(n => n.groupIndex === i);
    const x = Math.min(...peers.map(n => n.x)) - 24;
    const y = Math.min(...peers.map(n => n.y)) - 64;
    return { x, y, width: Math.max(...peers.map(n => n.x + n.width)) - x + 24,
      height: Math.max(...peers.map(n => n.y + n.height)) - y + 24 };
  });
  return { nodes, groups, width: Math.max(...groups.map(g => g.x + g.width)),
    height: Math.max(...groups.map(g => g.y + g.height)) };
}

function segmentHits(a, b, n) {
  const pad = 8, left = n.x - pad, right = n.x + n.width + pad;
  const top = n.y - pad, bottom = n.y + n.height + pad;
  if (a[0] === b[0]) return a[0] > left && a[0] < right && Math.max(a[1], b[1]) > top && Math.min(a[1], b[1]) < bottom;
  return a[1] > top && a[1] < bottom && Math.max(a[0], b[0]) > left && Math.min(a[0], b[0]) < right;
}

// Route along rectangle borders and free lanes instead of drawing through blocks.
export function routeEdge(a, b, nodes, index) {
  const fraction = 0.25 + (index % 3) * 0.25;
  const ports = n => [[n.x, n.y + n.height * fraction], [n.x + n.width, n.y + n.height * fraction],
    [n.x + n.width * fraction, n.y], [n.x + n.width * fraction, n.y + n.height]];
  const obstacles = nodes.filter(n => n !== a && n !== b);
  const offset = 18 + (index % 3) * 12;
  const xLanes = nodes.flatMap(n => [n.x - offset, n.x + n.width + offset]);
  const yLanes = nodes.flatMap(n => [n.y - offset, n.y + n.height + offset]);
  let best = null;
  function consider(points, source, target) {
    points = points.filter((p, i) => !i || p[0] !== points[i - 1][0] || p[1] !== points[i - 1][1]);
    let length = 0, hits = 0;
    for (let i = 1; i < points.length; i++) {
      const p = points[i - 1], q = points[i];
      length += Math.abs(q[0] - p[0]) + Math.abs(q[1] - p[1]);
      for (const n of obstacles) if (segmentHits(p, q, n)) hits++;
      // A path must leave its source and approach its target from outside.
      for (const n of [source, target]) if (segmentHits(p, q, { ...n, x: n.x + 9, y: n.y + 9, width: n.width - 18, height: n.height - 18 })) hits++;
    }
    const score = hits * 100000 + length + points.length * 12;
    if (!best || score < best.score) best = { points, score, hits };
  }
  for (const s of ports(a)) for (const t of ports(b)) {
    consider([s, [s[0], t[1]], t], a, b);
    consider([s, [t[0], s[1]], t], a, b);
    for (const lane of xLanes) consider([s, [lane, s[1]], [lane, t[1]], t], a, b);
    for (const lane of yLanes) consider([s, [s[0], lane], [t[0], lane], t], a, b);
  }
  let longest = 0, label = null;
  for (let i = 1; i < best.points.length; i++) {
    const p = best.points[i - 1], q = best.points[i], distance = Math.hypot(q[0] - p[0], q[1] - p[1]);
    if (distance > longest) { longest = distance; label = { x: (p[0] + q[0]) / 2, y: (p[1] + q[1]) / 2, vertical: p[0] === q[0] }; }
  }
  return { ...best, d: best.points.map((p, i) => `${i ? 'L' : 'M'}${p[0]} ${p[1]}`).join(' '), label };
}
