import { forceLayout, routeEdge } from './oss-map-layout.mjs';

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
  const measure = document.createElement('canvas').getContext('2d');
  measure.font = `20px ${getComputedStyle(svg).fontFamily}`;
  function wrapDescription(text) {
    const lines = []; let line = '';
    for (const part of (ja ? Array.from(text) : text.split(/(\s+)/))) {
      if (line && measure.measureText(line + part).width > 471) {
        lines.push(line.trim()); line = '';
      }
      line += part;
    }
    if (line.trim()) lines.push(line.trim());
    return lines;
  }
  const { nodes, groups: groupBounds, width, height } = forceLayout(data.nodes.map(n => {
    const lines = wrapDescription(ja ? n.description : n.enDescription);
    const groupIndex = data.groups.findIndex(([, ids]) => ids.includes(n.id));
    return { ...n, lines, groupIndex, width: 495, height: 56 + lines.length * 24 };
  }), data.edges);
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
  const marker = element('marker', { id: 'oss-map-arrow', markerWidth: 12, markerHeight: 12,
    refX: 11, refY: 6, orient: 'auto', markerUnits: 'userSpaceOnUse' }, defs);
  element('path', { d: 'M0 0 L12 6 L0 12', fill: 'context-stroke' }, marker);
  const world = element('g', { class: 'oss-map-world' }, svg);
  const wires = element('g', { class: 'oss-map-wires' }, world);
  data.groups.forEach(([name], i) => {
    element('text', { x: groupBounds[i].x + 24, y: groupBounds[i].y + 34,
      class: 'oss-map-group' }, world, ja ? name : data.groupNames[i]);
  });
  data.edges.forEach(([from, to, relation], i) => {
    const a = byId.get(from), b = byId.get(to);
    const route = routeEdge(a, b, nodes, i);
    const merged = relation === '統合';
    const el = element('path', { d: route.d, class: 'oss-map-edge' + (merged ? ' is-merge' : ''),
      'data-from': from, 'data-to': to, 'data-obstacle-hits': route.hits,
      'marker-end': 'url(#oss-map-arrow)', 'stroke-linejoin': 'round' }, wires);
    edgeElements.push(el);
    if (merged) {
      const { x, y, vertical } = route.label;
      element('text', { x, y: y - 7, class: 'oss-map-edge-label', 'text-anchor': 'middle',
        transform: vertical ? 'rotate(-90 ' + x + ' ' + y + ')' : '' }, wires, label('統合', 'Merged into'));
    }
  });
  nodes.forEach(n => {
    const el = element('g', { class: 'oss-map-node', 'data-node': n.id, role: 'button',
      tabindex: 0, 'aria-label': `${n.id}: ${ja ? n.description : n.enDescription}`, 'aria-pressed': 'false' }, world);
    element('rect', { x: n.x, y: n.y, width: 495, height: n.height, rx: 5 }, el);
    element('text', { x: n.x + 12, y: n.y + 28 }, el, n.id);
    const description = element('text', { class: 'oss-map-description', x: n.x + 12, y: n.y + 56 }, el);
    n.lines.forEach((line, i) => element('tspan', { x: n.x + 12, dy: i ? 24 : 0 }, description, line));
    nodeElements.set(n.id, el);
    el.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); choose(n.id); reveal(n); }
    });
    el.addEventListener('focus', () => reveal(n));
  });
  function draw() {
    x = Math.min(0, x); y = Math.min(0, y);
    world.setAttribute('transform', `translate(${x} ${y}) scale(${scale})`);
  }
  function fit() {
    scale = Math.min(svg.clientWidth / width, svg.clientHeight / height);
    x = 0; y = 0;
    area.value = 'all'; draw();
  }
  function reveal(n) {
    if (n.x * scale + x < 0 || (n.x + 495) * scale + x > svg.clientWidth ||
        n.y * scale + y < 0 || (n.y + n.height) * scale + y > svg.clientHeight) {
      scale = Math.max(0.3, Math.min(1.2, svg.clientWidth / 550));
      x = (24 - n.x) * scale;
      y = (24 - n.y) * scale;
      draw();
    }
  }
  function zoom(factor, cx = 0, cy = 0) {
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
    data.edges.forEach(([from, to, text], i) => {
      if (from !== id && to !== id) return;
      const li = document.createElement('li');
      li.append(`${from} → `);
      const a = document.createElement('a'); a.href = `https://github.com/conao3/${to}`;
      a.textContent = to; li.append(a, `: ${ja ? text : data.edgeLabels[i]}`); list.append(li);
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
    root.removeAttribute('role'); root.removeAttribute('aria-modal'); root.removeAttribute('aria-label');
    document.body.style.overflow = previousOverflow;
    fullscreenButton.textContent = label('全画面', 'Fullscreen');
    fullscreenButton.focus();
  }
  fullscreenButton.onclick = () => {
    if (root.classList.contains('is-expanded')) { closeExpanded(); return; }
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    root.classList.add('is-expanded');
    root.setAttribute('role', 'dialog'); root.setAttribute('aria-modal', 'true');
    root.setAttribute('aria-label', label('OSSの関係図', 'Open-source project map'));
    fullscreenButton.textContent = label('元の表示に戻す', 'Exit fullscreen');
  };
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && root.classList.contains('is-expanded')) closeExpanded();
    if (e.key === 'Tab' && root.classList.contains('is-expanded')) {
      const controls = [...root.querySelectorAll('button, select, a[href], [tabindex="0"]')]
        .filter(el => el.getClientRects().length);
      const first = controls[0], last = controls.at(-1);
      if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    }
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
    const bounds = groupBounds[index];
    scale = Math.min(svg.clientWidth / bounds.width, svg.clientHeight / bounds.height, 1.5);
    x = -bounds.x * scale; y = -bounds.y * scale;
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
  area.value = '0'; area.onchange();
  // Resizing the viewport preserves the camera; only explicit controls change scale.
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
