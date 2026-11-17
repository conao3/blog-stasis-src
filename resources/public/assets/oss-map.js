(() => {
  'use strict';
  const root = document.querySelector('[data-oss-map]');
  if (!root) return;
  const data = JSON.parse(root.querySelector('.oss-map-data').textContent);
  const ja = root.dataset.lang === 'ja';
  const label = (j, e) => ja ? j : e;
  const svg = root.querySelector('svg');
  const detail = root.querySelector('.oss-map-detail');
  const area = root.querySelector('select');
  const ns = 'http://www.w3.org/2000/svg';
  const width = 1690, height = 1100;
  const nodes = data.nodes.map(n => {
    const g = data.groups.findIndex(([, ids]) => ids.includes(n.id));
    return { ...n, x: 24 + (g % 3) * 560,
      y: 100 + Math.floor(g / 3) * 550 + data.groups[g][1].indexOf(n.id) * 51 };
  });
  const byId = new Map(nodes.map(n => [n.id, n]));
  let x = 0, y = 0, scale = 1;
  const nodeElements = new Map(), edgeElements = [];
  function element(tag, attrs, parent, text) {
    const el = document.createElementNS(ns, tag);
    for (const [key, value] of Object.entries(attrs)) el.setAttribute(key, value);
    if (text !== undefined) el.textContent = text;
    parent.append(el);
    return el;
  }
  const defs = element('defs', {}, svg);
  const marker = element('marker', { id: 'oss-map-arrow', markerWidth: 8, markerHeight: 8,
    refX: 7, refY: 4, orient: 'auto', markerUnits: 'userSpaceOnUse' }, defs);
  element('path', { d: 'M0 0 L8 4 L0 8', fill: 'context-stroke' }, marker);
  const world = element('g', { class: 'oss-map-world' }, svg);
  const wires = element('g', { class: 'oss-map-wires' }, world);
  data.groups.forEach(([name], i) => {
    element('text', { x: 24 + (i % 3) * 560, y: 62 + Math.floor(i / 3) * 550,
      class: 'oss-map-group' }, world, ja ? name : data.groupNames[i]);
  });
  data.edges.forEach(([from, to], i) => {
    const a = byId.get(from), b = byId.get(to);
    const sameColumn = a.x === b.x;
    const sx = a.x + 495, sy = a.y + 20;
    const tx = sameColumn ? b.x + 495 : b.x, ty = b.y + 20;
    const lane = sameColumn ? sx + 16 + (i % 3) * 8 : (sx + tx) / 2;
    const d = `M${sx} ${sy} C${lane} ${sy} ${lane} ${ty} ${tx} ${ty}`;
    const el = element('path', { d, class: 'oss-map-edge', 'marker-end': 'url(#oss-map-arrow)' }, wires);
    edgeElements.push(el);
  });
  nodes.forEach(n => {
    const el = element('g', { class: 'oss-map-node', 'data-node': n.id, role: 'button',
      tabindex: 0, 'aria-label': n.id, 'aria-pressed': 'false' }, world);
    element('rect', { x: n.x, y: n.y, width: 495, height: 40, rx: 5 }, el);
    element('text', { x: n.x + 12, y: n.y + 28 }, el, n.id);
    nodeElements.set(n.id, el);
    el.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); choose(n.id); reveal(n); }
    });
    el.addEventListener('focus', () => reveal(n));
  });
  function draw() { world.setAttribute('transform', `translate(${x} ${y}) scale(${scale})`); }
  function fit() {
    scale = Math.min(svg.clientWidth / width, svg.clientHeight / height);
    x = (svg.clientWidth - width * scale) / 2;
    y = (svg.clientHeight - height * scale) / 2;
    area.value = 'all'; draw();
  }
  function reveal(n) {
    if (n.x * scale + x < 0 || (n.x + 495) * scale + x > svg.clientWidth ||
        n.y * scale + y < 0 || (n.y + 40) * scale + y > svg.clientHeight) {
      scale = Math.max(0.3, Math.min(1.2, svg.clientWidth / 550));
      x = svg.clientWidth / 2 - (n.x + 247.5) * scale;
      y = svg.clientHeight / 2 - (n.y + 20) * scale;
      draw();
    }
  }
  function zoom(factor, cx = svg.clientWidth / 2, cy = svg.clientHeight / 2) {
    const next = Math.max(0.12, Math.min(3, scale * factor));
    x = cx - (cx - x) * next / scale;
    y = cy - (cy - y) * next / scale;
    scale = next; draw();
  }
  function choose(id, writeUrl = true) {
    const node = byId.get(id);
    if (!node) return;
    const connected = new Set([id]);
    data.edges.forEach(([from, to], i) => {
      const active = from === id || to === id;
      edgeElements[i].classList.toggle('is-active', active);
      if (active) { connected.add(from); connected.add(to); }
    });
    nodeElements.forEach((el, key) => {
      el.setAttribute('aria-pressed', String(key === id));
      el.classList.toggle('is-connected', connected.has(key));
      el.classList.toggle('is-muted', !connected.has(key));
    });
    detail.replaceChildren();
    const link = document.createElement('a');
    link.href = node.url; link.textContent = id; detail.append(link);
    const description = document.createElement('p');
    description.textContent = ja ? node.description : node.enDescription;
    detail.append(description);
    const list = document.createElement('ul');
    data.edges.forEach(([from, to, text, source], i) => {
      if (from !== id && to !== id) return;
      const li = document.createElement('li');
      li.append(`${from} → ${to}: ${ja ? text : data.edgeLabels[i]} (`);
      const a = document.createElement('a'); a.href = `https://github.com/conao3/${source}`;
      a.textContent = label('根拠', 'Source'); li.append(a, ')'); list.append(li);
    });
    if (list.children.length) detail.append(list);
    else {
      const p = document.createElement('p');
      p.textContent = label('この図には直接の関係線を掲載していません。', 'No direct relationship is documented on this map.');
      detail.append(p);
    }
    if (writeUrl) {
      const url = new URL(location.href); url.searchParams.set('oss', id);
      history.replaceState(null, '', url);
    }
  }
  root.querySelector('[data-action="zoom-in"]').onclick = () => zoom(1.3);
  root.querySelector('[data-action="zoom-out"]').onclick = () => zoom(1 / 1.3);
  root.querySelector('[data-action="fit"]').onclick = fit;
  const fullscreenButton = root.querySelector('[data-action="fullscreen"]');
  let previousOverflow = '';
  function closeExpanded() {
    root.classList.remove('is-expanded');
    document.body.style.overflow = previousOverflow;
    fullscreenButton.textContent = label('全画面', 'Fullscreen');
    fullscreenButton.focus();
  }
  fullscreenButton.onclick = async () => {
    if (root.classList.contains('is-expanded')) { closeExpanded(); return; }
    try {
      if (document.fullscreenElement) await document.exitFullscreen();
      else await root.requestFullscreen();
    } catch (_) {
      previousOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      root.classList.add('is-expanded');
      fullscreenButton.textContent = label('元の表示に戻す', 'Exit fullscreen');
    }
  };
  document.addEventListener('fullscreenchange', () => {
    fullscreenButton.textContent = document.fullscreenElement ? label('元の表示に戻す', 'Exit fullscreen') : label('全画面', 'Fullscreen');
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && root.classList.contains('is-expanded')) closeExpanded();
  });
  area.onchange = event => {
    if (event) {
      const url = new URL(location.href);
      if (area.value === 'all') url.searchParams.delete('oss-area');
      else url.searchParams.set('oss-area', area.value);
      history.replaceState(null, '', url);
    }
    if (area.value === 'all') { fit(); return; }
    const index = Number(area.value);
    scale = Math.min(svg.clientWidth / 550, svg.clientHeight / 530, 1.5);
    x = (svg.clientWidth - 550 * scale) / 2 - (index % 3) * 560 * scale;
    y = -Math.floor(index / 3) * 550 * scale;
    draw();
  };
  svg.addEventListener('wheel', e => {
    if (!e.ctrlKey && !e.metaKey) return;
    e.preventDefault(); const b = svg.getBoundingClientRect();
    zoom(Math.exp(-e.deltaY * 0.006), e.clientX - b.left, e.clientY - b.top);
  }, { passive: false });
  const pointers = new Map(); let gesture = null, dragging = false;
  function beginGesture() {
    const ps = [...pointers.values()];
    if (ps.length === 1) gesture = { x, y, px: ps[0].x, py: ps[0].y, id: ps[0].node };
    else if (ps.length >= 2) {
      const cx = (ps[0].x + ps[1].x) / 2, cy = (ps[0].y + ps[1].y) / 2;
      gesture = { distance: Math.hypot(ps[0].x - ps[1].x, ps[0].y - ps[1].y), scale,
        wx: (cx - x) / scale, wy: (cy - y) / scale };
      dragging = true;
    }
  }
  svg.addEventListener('pointerdown', e => {
    if (e.button !== 0) return;
    const b = svg.getBoundingClientRect();
    pointers.set(e.pointerId, { x: e.clientX - b.left, y: e.clientY - b.top, node: e.target.closest('[data-node]')?.dataset.node });
    if (pointers.size === 1) dragging = false;
    svg.setPointerCapture(e.pointerId); beginGesture();
  });
  svg.addEventListener('pointermove', e => {
    if (!pointers.has(e.pointerId)) return;
    const b = svg.getBoundingClientRect(), old = pointers.get(e.pointerId);
    pointers.set(e.pointerId, { ...old, x: e.clientX - b.left, y: e.clientY - b.top });
    const ps = [...pointers.values()];
    if (ps.length >= 2 && gesture.distance) {
      scale = Math.max(0.12, Math.min(3, gesture.scale * Math.hypot(ps[0].x - ps[1].x, ps[0].y - ps[1].y) / gesture.distance));
      x = (ps[0].x + ps[1].x) / 2 - gesture.wx * scale;
      y = (ps[0].y + ps[1].y) / 2 - gesture.wy * scale;
    } else if (ps.length === 1 && gesture) {
      const dx = ps[0].x - gesture.px, dy = ps[0].y - gesture.py;
      if (Math.abs(dx) + Math.abs(dy) > 5) dragging = true;
      x = gesture.x + dx; y = gesture.y + dy;
    }
    draw();
  });
  function endPointer(e) {
    if (!pointers.has(e.pointerId)) return;
    const id = gesture?.id;
    pointers.delete(e.pointerId);
    if (e.type === 'pointerup' && !dragging && id) choose(id);
    if (pointers.size) beginGesture(); else gesture = null;
  }
  svg.addEventListener('pointerup', endPointer);
  svg.addEventListener('pointercancel', endPointer);
  svg.addEventListener('keydown', e => {
    const moves = { ArrowLeft: [70, 0], ArrowRight: [-70, 0], ArrowUp: [0, 70], ArrowDown: [0, -70] };
    if (moves[e.key]) { e.preventDefault(); x += moves[e.key][0]; y += moves[e.key][1]; draw(); }
    else if (e.key === '+' || e.key === '=') { e.preventDefault(); zoom(1.3); }
    else if (e.key === '-') { e.preventDefault(); zoom(1 / 1.3); }
    else if (e.key === '0') { e.preventDefault(); fit(); }
  });
  root.querySelector('.oss-map-interactive').hidden = false;
  if (svg.clientWidth < 600) { area.value = '0'; area.onchange(); }
  else fit();
  new ResizeObserver(() => { if (area.value === 'all') fit(); else area.onchange(); }).observe(svg);
  function restore() {
    const params = new URL(location.href).searchParams;
    const id = params.get('oss'), savedArea = params.get('oss-area');
    if (savedArea !== null && /^[0-5]$/.test(savedArea)) {
      area.value = savedArea; area.onchange();
    } else if (byId.has(id)) {
      area.value = String(data.groups.findIndex(([, ids]) => ids.includes(id)));
      area.onchange();
    }
    if (byId.has(id)) { choose(id, false); reveal(byId.get(id)); }
  }
  restore(); window.addEventListener('popstate', restore);
})();
