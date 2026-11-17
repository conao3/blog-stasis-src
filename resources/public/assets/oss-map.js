(() => {
  'use strict';
  const root = document.querySelector('[data-oss-map]');
  if (!root) return;
  const map = JSON.parse(root.querySelector('.oss-map-data').textContent);
  const ja = root.dataset.lang === 'ja';
  const t = (j, e) => ja ? j : e;
  const text = item => ja ? item.ja : item.en;
  const frame = root.querySelector('.oss-map-frame');
  const viewport = root.querySelector('.oss-map-viewport');
  const world = root.querySelector('.oss-map-world');
  const detail = root.querySelector('.oss-map-detail');
  const zoomLabel = root.querySelector('.oss-map-scale');
  const expandButton = root.querySelector('[data-action="expand"]');
  const ns = 'http://www.w3.org/2000/svg';
  const MARGIN = 16, MIN_SCALE = 0.2, MAX_SCALE = 2;
  const byId = new Map(map.nodes.map(n => [n.id, n]));

  function svgElement(tag, attrs, parent, content) {
    const el = document.createElementNS(ns, tag);
    for (const [key, value] of Object.entries(attrs)) el.setAttribute(key, value);
    if (content !== undefined) el.textContent = content;
    parent.append(el);
    return el;
  }
  function htmlElement(tag, attrs, parent, content) {
    const el = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs)) el.setAttribute(key, value);
    if (content !== undefined) el.textContent = content;
    parent?.append(el);
    return el;
  }
  function roundedPath(points, radius = 6) {
    let d = `M${points[0][0]},${points[0][1]}`;
    for (let i = 1; i < points.length - 1; i++) {
      const [px, py] = points[i - 1], [x, y] = points[i], [nx, ny] = points[i + 1];
      const r = Math.min(radius, Math.hypot(x - px, y - py) / 2, Math.hypot(nx - x, ny - y) / 2);
      const ax = x - Math.sign(x - px) * r, ay = y - Math.sign(y - py) * r;
      const bx = x + Math.sign(nx - x) * r, by = y + Math.sign(ny - y) * r;
      d += ` L${ax},${ay} Q${x},${y} ${bx},${by}`;
    }
    const [lx, ly] = points.at(-1);
    return d + ` L${lx},${ly}`;
  }

  world.style.width = `${map.width}px`;
  world.style.height = `${map.height}px`;
  const svg = svgElement('svg', { width: map.width, height: map.height, viewBox: `0 0 ${map.width} ${map.height}`, 'aria-hidden': 'true' }, world);
  const defs = svgElement('defs', {}, svg);
  for (const name of ['default', 'merge', 'active']) {
    const marker = svgElement('marker', { id: `oss-map-arrow-${name}`, class: `oss-map-arrow is-${name}`, markerWidth: 8, markerHeight: 8,
      refX: 7, refY: 4, orient: 'auto', markerUnits: 'userSpaceOnUse' }, defs);
    svgElement('path', { d: 'M0,0 L8,4 L0,8 Z' }, marker);
  }
  map.groups.forEach(group => {
    svgElement('rect', { class: 'oss-map-group', x: group.x, y: group.y, width: group.width, height: group.height, rx: 8 }, svg);
    svgElement('text', { class: 'oss-map-group-title', x: group.x + 20, y: group.y + 30 }, svg, text(group));
  });
  const edgeElements = map.edges.map(edge => {
    const marker = edge.kind === 'merge' ? 'merge' : 'default';
    const g = svgElement('g', { class: `oss-map-edge is-${edge.kind}` }, svg);
    const path = svgElement('path', { d: roundedPath(edge.points), 'marker-end': `url(#oss-map-arrow-${marker})` }, g);
    svgElement('text', { x: edge.label[0], y: edge.label[1], 'dominant-baseline': 'central' }, g, text(edge));
    return { edge, g, path, marker };
  });
  const nodeElements = new Map();
  for (const node of map.nodes) {
    const button = htmlElement('button', { type: 'button', class: 'oss-map-node', 'data-id': node.id, 'aria-pressed': 'false' }, world);
    button.style.left = `${node.x}px`;
    button.style.top = `${node.y}px`;
    button.style.width = `${map.nodeWidth}px`;
    button.style.height = `${map.nodeHeight}px`;
    htmlElement('span', { class: 'oss-map-node-name' }, button, node.id);
    htmlElement('span', { class: 'oss-map-node-description' }, button, text(node));
    button.addEventListener('click', () => {
      if (suppressClick) return;
      select(selected === node.id ? null : node.id);
    });
    button.addEventListener('focus', () => reveal(node));
    nodeElements.set(node.id, button);
  }

  let x = MARGIN, y = MARGIN, scale = 1, selected = null, suppressClick = false;
  function clamp() {
    const vw = viewport.clientWidth, vh = viewport.clientHeight;
    const w = map.width * scale, h = map.height * scale;
    x = Math.min(MARGIN, Math.max(x, Math.min(MARGIN, vw - MARGIN - w)));
    y = Math.min(MARGIN, Math.max(y, Math.min(MARGIN, vh - MARGIN - h)));
  }
  function draw() {
    clamp();
    world.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
    zoomLabel.textContent = `${Math.round(scale * 100)}%`;
  }
  function zoomTo(next, cx = viewport.clientWidth / 2, cy = viewport.clientHeight / 2) {
    next = Math.max(MIN_SCALE, Math.min(MAX_SCALE, next));
    x = cx - (cx - x) * next / scale;
    y = cy - (cy - y) * next / scale;
    scale = next;
    draw();
  }
  function fitAll() {
    scale = Math.max(MIN_SCALE, Math.min(1, (viewport.clientWidth - 2 * MARGIN) / map.width, (viewport.clientHeight - 2 * MARGIN) / map.height));
    x = MARGIN; y = MARGIN;
    draw();
  }
  function showGroup(index) {
    const group = map.groups[index];
    scale = 1;
    x = MARGIN - group.x; y = MARGIN - group.y;
    draw();
  }
  function reveal(node) {
    const vw = viewport.clientWidth, vh = viewport.clientHeight;
    const visible = () => {
      const left = x + node.x * scale, top = y + node.y * scale;
      return left >= 0 && top >= 0 && left + map.nodeWidth * scale <= vw && top + map.nodeHeight * scale <= vh;
    };
    if (visible() && scale >= 0.6) return;
    const group = map.groups[node.group];
    scale = 1;
    x = Math.min(MARGIN - group.x, vw - MARGIN - node.x - map.nodeWidth);
    y = Math.min(MARGIN - group.y, vh - MARGIN - node.y - map.nodeHeight);
    draw();
  }

  function nameButton(id) {
    const button = htmlElement('button', { type: 'button', class: 'oss-map-link' }, null, id);
    button.addEventListener('click', () => { select(id); nodeElements.get(id).focus({ preventScroll: true }); });
    return button;
  }
  function renderDetail(node) {
    detail.replaceChildren();
    const header = htmlElement('div', { class: 'oss-map-detail-header' }, detail);
    htmlElement('h4', {}, header, node.id);
    const close = htmlElement('button', { type: 'button', class: 'oss-map-close', 'aria-label': t('閉じる', 'Close') }, header, '×');
    close.addEventListener('click', () => { select(null); nodeElements.get(node.id).focus({ preventScroll: true }); });
    htmlElement('p', {}, detail, text(node));
    const related = map.edges.filter(e => e.from === node.id || e.to === node.id);
    if (related.length) {
      const list = htmlElement('ul', { class: 'oss-map-relations' }, detail);
      for (const edge of related) {
        const item = htmlElement('li', {}, list);
        const from = edge.from === node.id ? htmlElement('strong', {}, null, edge.from) : nameButton(edge.from);
        const to = edge.to === node.id ? htmlElement('strong', {}, null, edge.to) : nameButton(edge.to);
        item.append(from, ' → ', to);
        htmlElement('span', { class: 'oss-map-relation' }, item, text(edge));
      }
    }
    htmlElement('a', { href: node.url, class: 'oss-map-github' }, detail, t('GitHub で見る', 'View on GitHub'));
  }
  function select(id, { writeUrl = true } = {}) {
    selected = byId.has(id) ? id : null;
    const connected = new Set(selected ? [selected] : []);
    for (const { edge, g, path, marker } of edgeElements) {
      const active = selected !== null && (edge.from === selected || edge.to === selected);
      if (active) { connected.add(edge.from); connected.add(edge.to); }
      g.classList.toggle('is-active', active);
      g.classList.toggle('is-muted', selected !== null && !active);
      path.setAttribute('marker-end', `url(#oss-map-arrow-${active ? 'active' : marker})`);
    }
    for (const [key, button] of nodeElements) {
      button.setAttribute('aria-pressed', String(key === selected));
      button.classList.toggle('is-muted', selected !== null && !connected.has(key));
    }
    root.classList.toggle('has-selection', selected !== null);
    detail.hidden = selected === null;
    if (selected) {
      renderDetail(byId.get(selected));
      requestAnimationFrame(() => {
        reveal(byId.get(selected));
        if (writeUrl && !root.classList.contains('is-expanded')) detail.scrollIntoView({ block: 'nearest' });
      });
    }
    if (writeUrl) {
      const url = new URL(location.href);
      if (selected) url.searchParams.set('oss', selected);
      else url.searchParams.delete('oss');
      history.replaceState(history.state, '', url);
    }
  }

  root.querySelector('[data-action="zoom-in"]').addEventListener('click', () => zoomTo(scale * 1.25));
  root.querySelector('[data-action="zoom-out"]').addEventListener('click', () => zoomTo(scale / 1.25));
  root.querySelector('[data-action="fit"]').addEventListener('click', fitAll);
  root.querySelectorAll('[data-area]').forEach(button => button.addEventListener('click', () => showGroup(Number(button.dataset.area))));

  let previousOverflow = '';
  function setExpanded(expanded) {
    root.classList.toggle('is-expanded', expanded);
    expandButton.setAttribute('aria-pressed', String(expanded));
    expandButton.textContent = expanded ? t('元に戻す', 'Exit full view') : t('全画面', 'Full view');
    if (expanded) {
      previousOverflow = document.documentElement.style.overflow;
      document.documentElement.style.overflow = 'hidden';
      frame.setAttribute('role', 'dialog');
      frame.setAttribute('aria-modal', 'true');
    } else {
      document.documentElement.style.overflow = previousOverflow;
      frame.removeAttribute('role');
      frame.removeAttribute('aria-modal');
    }
    draw();
  }
  expandButton.addEventListener('click', () => setExpanded(!root.classList.contains('is-expanded')));
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
      if (selected && root.contains(document.activeElement)) { select(null); return; }
      if (root.classList.contains('is-expanded')) { setExpanded(false); expandButton.focus(); }
      return;
    }
    if (event.key === 'Tab' && root.classList.contains('is-expanded')) {
      const items = [...frame.querySelectorAll('button, a[href], [tabindex="0"]')].filter(el => el.getClientRects().length);
      const first = items[0], last = items.at(-1);
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    }
  });

  viewport.addEventListener('wheel', event => {
    if (!event.ctrlKey && !event.metaKey) return;
    event.preventDefault();
    const box = viewport.getBoundingClientRect();
    zoomTo(scale * Math.exp(-event.deltaY * 0.004), event.clientX - box.left, event.clientY - box.top);
  }, { passive: false });
  const pointers = new Map();
  let gesture = null, moved = false;
  function startGesture() {
    const points = [...pointers.values()];
    if (points.length === 1) gesture = { x, y, px: points[0].x, py: points[0].y };
    else {
      const [a, b] = points;
      const cx = (a.x + b.x) / 2, cy = (a.y + b.y) / 2;
      gesture = { distance: Math.hypot(a.x - b.x, a.y - b.y), scale, wx: (cx - x) / scale, wy: (cy - y) / scale };
      moved = true;
    }
  }
  viewport.addEventListener('pointerdown', event => {
    if (event.button !== 0 || detail.contains(event.target)) return;
    const box = viewport.getBoundingClientRect();
    pointers.set(event.pointerId, { x: event.clientX - box.left, y: event.clientY - box.top });
    if (pointers.size === 1) moved = false;
    startGesture();
  });
  viewport.addEventListener('pointermove', event => {
    if (!pointers.has(event.pointerId)) return;
    const box = viewport.getBoundingClientRect();
    pointers.set(event.pointerId, { x: event.clientX - box.left, y: event.clientY - box.top });
    const points = [...pointers.values()];
    if (points.length >= 2 && gesture.distance) {
      const [a, b] = points;
      scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, gesture.scale * Math.hypot(a.x - b.x, a.y - b.y) / gesture.distance));
      x = (a.x + b.x) / 2 - gesture.wx * scale;
      y = (a.y + b.y) / 2 - gesture.wy * scale;
    } else if (points.length === 1 && gesture) {
      const dx = points[0].x - gesture.px, dy = points[0].y - gesture.py;
      if (!moved && Math.hypot(dx, dy) > 4) {
        moved = true;
        viewport.setPointerCapture(event.pointerId);
        viewport.classList.add('is-dragging');
      }
      if (!moved) return;
      x = gesture.x + dx; y = gesture.y + dy;
    }
    draw();
  });
  function endPointer(event) {
    if (!pointers.has(event.pointerId)) return;
    pointers.delete(event.pointerId);
    if (pointers.size) { startGesture(); return; }
    gesture = null;
    viewport.classList.remove('is-dragging');
    if (moved) {
      suppressClick = true;
      setTimeout(() => { suppressClick = false; });
    } else if (event.type === 'pointerup' && selected && !event.target.closest('.oss-map-node')) {
      select(null);
    }
  }
  viewport.addEventListener('pointerup', endPointer);
  viewport.addEventListener('pointercancel', endPointer);
  viewport.addEventListener('keydown', event => {
    if (event.target !== viewport) return;
    const step = 80, moves = { ArrowLeft: [step, 0], ArrowRight: [-step, 0], ArrowUp: [0, step], ArrowDown: [0, -step] };
    if (moves[event.key]) { event.preventDefault(); x += moves[event.key][0]; y += moves[event.key][1]; draw(); }
    else if (event.key === '+' || event.key === '=') { event.preventDefault(); zoomTo(scale * 1.25); }
    else if (event.key === '-') { event.preventDefault(); zoomTo(scale / 1.25); }
    else if (event.key === '0') { event.preventDefault(); fitAll(); }
  });
  new ResizeObserver(draw).observe(viewport);

  function restore() {
    const id = new URL(location.href).searchParams.get('oss');
    select(id, { writeUrl: false });
  }
  frame.hidden = false;
  draw();
  restore();
  window.addEventListener('popstate', restore);
})();
