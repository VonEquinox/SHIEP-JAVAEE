import { nextTick, onBeforeUnmount } from 'vue';

const EASING = 'cubic-bezier(0.22, 1, 0.36, 1)';
const STEP_DELAY_MS = 190;
const STEP_MOVE_RATIO = 0.68;
const EXIT_SCREEN_DURATION = 760;
const EXIT_SCREEN_MARGIN = 96;
const MOVE_BASE_MS = 260;
const MOVE_STEP_MS = 165;
const MOVE_POWER = 0.72;
const MAX_MOVE_DURATION = 1850;
const EXIT_DURATION_SCALE = 0.68;

const prefersReducedMotion = () =>
  typeof window !== 'undefined' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const idOf = (hotel) => String(hotel?.hotelId);
const keyOf = (cell) => `${cell.r}:${cell.c}`;
const sameCell = (a, b) => a.r === b.r && a.c === b.c;

function getCards(gridEl) {
  return Array.from(gridEl?.querySelectorAll('[data-expand-card]') || []);
}

function getGridCols(gridEl) {
  const columns = getComputedStyle(gridEl).gridTemplateColumns
    .split(' ')
    .map((x) => x.trim())
    .filter(Boolean);
  return Math.max(columns.length, 1);
}

function getMetrics(gridEl, oldShots, newShots, cols, count) {
  const gridRect = gridEl.getBoundingClientRect();
  const style = getComputedStyle(gridEl);
  const firstRect = oldShots[0]?.rect || newShots[0]?.rect || gridRect;
  let colPitch = firstRect.width + (parseFloat(style.columnGap) || parseFloat(style.gap) || 0);
  let rowPitch = firstRect.height + (parseFloat(style.rowGap) || parseFloat(style.gap) || 0);

  const allRects = [...oldShots, ...newShots].map((s) => s.rect);
  const sameRowNext = allRects.find((r) => Math.abs(r.top - firstRect.top) < 4 && r.left > firstRect.left + 4);
  if (sameRowNext) colPitch = sameRowNext.left - firstRect.left;

  const nextRow = allRects.find((r) => r.top > firstRect.top + 4);
  if (nextRow) rowPitch = nextRow.top - firstRect.top;

  return {
    gridRect,
    cardWidth: firstRect.width,
    cardHeight: firstRect.height,
    colPitch,
    rowPitch,
    cols,
    rows: Math.max(1, Math.ceil(count / cols)),
  };
}

function cellToPoint(metrics, cell) {
  return {
    x: metrics.gridRect.left + window.scrollX + cell.c * metrics.colPitch,
    y: metrics.gridRect.top + window.scrollY + cell.r * metrics.rowPitch,
  };
}

function cloneGhost(el, rect, className = 'grid-filter-ghost') {
  const ghost = el.cloneNode(true);
  ghost.classList.add(className);
  Object.assign(ghost.style, {
    position: 'absolute',
    left: `${rect.left + window.scrollX}px`,
    top: `${rect.top + window.scrollY}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    margin: '0',
    zIndex: 24,
    pointerEvents: 'none',
    visibility: 'visible',
    display: getComputedStyle(el).display,
    willChange: 'transform, opacity',
    contain: 'layout paint style',
  });
  document.body.appendChild(ghost);
  return ghost;
}

function snapshotCards(gridEl, hotels) {
  const cards = getCards(gridEl);
  const byId = new Map(hotels.map((hotel, index) => [idOf(hotel), { hotel, index }]));
  return cards
    .map((el) => {
      const id = el.dataset.hotelId;
      const item = byId.get(id);
      if (!item) return null;
      return {
        id,
        el,
        index: item.index,
        rect: el.getBoundingClientRect(),
      };
    })
    .filter(Boolean);
}

function positionOf(index, cols) {
  return { r: Math.floor(index / cols), c: index % cols };
}

function push(path, cell) {
  path.push({ ...cell });
}

function waitUntil(path, tick) {
  while (path.length - 1 < tick) push(path, path[path.length - 1]);
}

function boundsFor(cols, rows) {
  return { minC: -1, maxC: cols, minR: 0, maxR: rows };
}

function isOutside(cell, cols, rows) {
  return cell.c < 0 || cell.c >= cols || cell.r >= rows;
}

function inBounds(cell, bounds) {
  return (
    cell.r >= bounds.minR &&
    cell.r <= bounds.maxR &&
    cell.c >= bounds.minC &&
    cell.c <= bounds.maxC
  );
}

function neighbors(cell) {
  return [
    { r: cell.r, c: cell.c - 1 },
    { r: cell.r, c: cell.c + 1 },
    { r: cell.r + 1, c: cell.c },
    { r: cell.r - 1, c: cell.c },
  ];
}

function exitTargetFor(cell, cols, rows) {
  if (cell.r >= rows - 1) return { r: rows, c: cell.c };

  const leftSteps = cell.c + 1;
  const rightSteps = cols - cell.c;
  const downSteps = rows - cell.r;

  if (downSteps < leftSteps && downSteps < rightSteps) return { r: rows, c: cell.c };
  if (leftSteps <= rightSteps) return { r: cell.r, c: -1 };
  return { r: cell.r, c: cols };
}

function exitChoicesFor(cell, cols, rows) {
  const choices = [];
  if (cell.r >= rows - 1) choices.push({ target: { r: rows, c: cell.c }, steps: rows - cell.r, dir: 'down' });
  choices.push({ target: { r: cell.r, c: -1 }, steps: cell.c + 1, dir: 'left' });
  choices.push({ target: { r: cell.r, c: cols }, steps: cols - cell.c, dir: 'right' });
  choices.push({ target: { r: rows, c: cell.c }, steps: rows - cell.r, dir: 'down' });
  return choices;
}

function directCellsBetween(start, target) {
  const cells = [];
  let current = { ...start };
  while (current.r !== target.r) {
    current = { r: current.r + Math.sign(target.r - current.r), c: current.c };
    cells.push({ ...current });
  }
  while (current.c !== target.c) {
    current = { r: current.r, c: current.c + Math.sign(target.c - current.c) };
    cells.push({ ...current });
  }
  return cells;
}

function clearExitChoicesFor(cell, cols, rows, blockedCells) {
  return exitChoicesFor(cell, cols, rows)
    .map((choice, index) => ({
      ...choice,
      index,
      blocked: directCellsBetween(cell, choice.target).some((next) => blockedCells.has(keyOf(next))),
    }))
    .filter((choice) => !choice.blocked)
    .sort((a, b) => a.steps - b.steps || a.index - b.index);
}

function chooseExitTarget(cell, cols, rows, blockedCells) {
  return exitChoicesFor(cell, cols, rows)
    .map((choice, index) => ({
      ...choice,
      index,
      blocked: directCellsBetween(cell, choice.target).some((next) => blockedCells.has(keyOf(next))),
    }))
    .sort((a, b) => Number(a.blocked) - Number(b.blocked) || a.steps - b.steps || a.index - b.index)[0].target;
}

function isReleasedExitCell(cell, target, bounds) {
  return sameCell(cell, target) && (cell.c <= bounds.minC || cell.c >= bounds.maxC || cell.r >= bounds.maxR);
}

function findPath(start, isTarget, occupancy, bounds) {
  const startKey = keyOf(start);
  const queue = [{ cell: start, prev: -1 }];
  const seen = new Map([[startKey, 0]]);

  for (let i = 0; i < queue.length; i += 1) {
    const state = queue[i];
    if (i > 0 && isTarget(state.cell)) {
      const path = [];
      let cursor = i;
      while (cursor !== -1) {
        path.push(queue[cursor].cell);
        cursor = queue[cursor].prev;
      }
      return path.reverse();
    }

    for (const next of neighbors(state.cell)) {
      if (!inBounds(next, bounds)) continue;
      const key = keyOf(next);
      if (seen.has(key)) continue;
      if (occupancy.has(key) && key !== startKey) continue;
      seen.set(key, queue.length);
      queue.push({ cell: next, prev: i });
    }
  }

  return null;
}

function directPath(start, target) {
  const path = [{ ...start }];
  let current = { ...start };
  while (current.r !== target.r) {
    current = { r: current.r + Math.sign(target.r - current.r), c: current.c };
    push(path, current);
  }
  while (current.c !== target.c) {
    current = { r: current.r, c: current.c + Math.sign(target.c - current.c) };
    push(path, current);
  }
  return path;
}

function appendSubpath(plan, cursor, subpath) {
  waitUntil(plan.path, cursor);
  subpath.slice(1).forEach((cell) => push(plan.path, cell));
  return cursor + Math.max(1, subpath.length - 1) + 1;
}

function movePlan(plan, cursor, subpath, occupancy, currentCells) {
  const from = currentCells.get(plan.id);
  if (from) occupancy.delete(keyOf(from));
  const nextCursor = appendSubpath(plan, cursor, subpath);
  const end = subpath[subpath.length - 1];
  currentCells.set(plan.id, end);
  occupancy.set(keyOf(end), plan.id);
  return nextCursor;
}

function validatePlans(plans) {
  const maxTicks = Math.max(0, ...plans.map((p) => p.path.length - 1));
  for (let tick = 0; tick <= maxTicks; tick += 1) {
    const occupied = new Map();
    for (const plan of plans) {
      if ((plan.kind === 'exit' && tick >= plan.fadeFromTick) || (plan.kind === 'enter' && tick <= plan.fadeInTick)) {
        continue;
      }
      const cell = plan.path[Math.min(tick, plan.path.length - 1)];
      if (cell.r < 0) return false;
      const key = keyOf(cell);
      if (occupied.has(key)) return false;
      occupied.set(key, plan.id);
    }
  }
  return true;
}

function distance(a, b) {
  return Math.abs(a.r - b.r) + Math.abs(a.c - b.c);
}

function preferredSteps(cell, target, bounds) {
  return neighbors(cell)
    .filter((next) => inBounds(next, bounds))
    .sort((a, b) => distance(a, target) - distance(b, target));
}

function appendTick(allPlans, ids, nextCellsById, currentCells) {
  allPlans.forEach((plan) => {
    const next = ids.includes(plan.id)
      ? nextCellsById.get(plan.id) || currentCells.get(plan.id)
      : currentCells.get(plan.id) || plan.path[plan.path.length - 1];
    push(plan.path, next || plan.path[plan.path.length - 1]);
  });
}

function planParallelMoves({ plans, movingIds, currentCells, targets, bounds, blockedIds = new Set(), maxTicks = 80 }) {
  const ids = [...movingIds];
  let tick = 0;

  while (tick < maxTicks && ids.some((id) => !sameCell(currentCells.get(id), targets.get(id)))) {
    const desired = new Map();
    const occupied = new Map(
      [...currentCells.entries()]
        .filter(([id, cell]) => {
          const target = targets.get(id);
          return !(target && isReleasedExitCell(cell, target, bounds));
        })
        .map(([id, cell]) => [keyOf(cell), id]),
    );

    ids.forEach((id) => {
      const current = currentCells.get(id);
      const target = targets.get(id);
      if (!current || !target || sameCell(current, target)) return;

      const candidate = preferredSteps(current, target, bounds).find((next) => {
        const occupant = occupied.get(keyOf(next));
        // Conservative rule: one tick may not enter a currently occupied cell.
        // The card can move into it on the next tick after the occupant has actually left.
        return !occupant || occupant === id;
      });
      if (candidate) desired.set(id, candidate);
    });

    const targetCounts = new Map();
    desired.forEach((cell) => targetCounts.set(keyOf(cell), (targetCounts.get(keyOf(cell)) || 0) + 1));

    const accepted = new Map();
    desired.forEach((cell, id) => {
      if (targetCounts.get(keyOf(cell)) > 1) return;
      accepted.set(id, cell);
    });

    let moved = false;
    ids.forEach((id) => {
      if (!accepted.has(id)) accepted.set(id, currentCells.get(id));
      if (!sameCell(accepted.get(id), currentCells.get(id))) moved = true;
    });

    appendTick(plans, ids, accepted, currentCells);
    accepted.forEach((cell, id) => currentCells.set(id, cell));
    tick += 1;
    if (!moved) break;
  }
}

function firstPathToAnyExit(start, cols, rows, occupancy, bounds) {
  const candidates = exitChoicesFor(start, cols, rows)
    .map((choice, index) => ({ ...choice, index }))
    .sort((a, b) => a.steps - b.steps || a.index - b.index);
  for (const choice of candidates) {
    const path = findPath(start, (cell) => sameCell(cell, choice.target), occupancy, bounds);
    if (path) return path;
  }
  return null;
}

function uniqueEnterStart(target, cols, usedStarts) {
  const fromLeft = target.c < cols / 2;
  const sideC = fromLeft ? -1 : cols;
  let row = target.r;
  let start = { r: row, c: sideC };
  while (usedStarts.has(keyOf(start))) {
    row += 1;
    start = { r: row, c: sideC };
  }
  usedStarts.add(keyOf(start));
  return start;
}

function buildPlans({ oldShots, oldHotels, nextHotels, cols, metrics, oldGhosts, enterGhosts }) {
  const oldById = new Map(oldHotels.map((hotel, index) => [idOf(hotel), { hotel, index }]));
  const nextById = new Map(nextHotels.map((hotel, index) => [idOf(hotel), { hotel, index }]));
  const oldShotById = new Map(oldShots.map((shot) => [shot.id, shot]));
  const bounds = boundsFor(cols, metrics.rows + nextHotels.length);
  const plans = [];
  const currentCells = new Map();

  oldHotels.forEach((hotel, index) => {
    const id = idOf(hotel);
    const from = positionOf(index, cols);
    currentCells.set(id, from);
    plans.push({
      id,
      kind: nextById.has(id) ? 'survivor' : 'exit',
      ghost: oldGhosts.get(id),
      startRect: oldShotById.get(id)?.rect,
      path: [{ ...from }],
      target: nextById.has(id) ? positionOf(nextById.get(id).index, cols) : null,
      order: nextById.get(id)?.index ?? index,
    });
  });

  const exitPlans = plans.filter((plan) => plan.kind === 'exit');
  const survivorPlansRaw = plans.filter((plan) => plan.kind === 'survivor');
  const survivorIds = survivorPlansRaw.map((plan) => plan.id);
  const survivorCells = new Set(survivorPlansRaw.map((plan) => keyOf(currentCells.get(plan.id))));
  const claimedExitTargets = new Set();
  exitPlans.forEach((plan) => {
    const start = currentCells.get(plan.id);
    const choices = clearExitChoicesFor(start, cols, metrics.rows, survivorCells);
    const fallbackChoices = exitChoicesFor(start, cols, metrics.rows).sort((a, b) => a.steps - b.steps);
    // A clear route is more important than a unique route. Multiple exits may share
    // the same outside buffer cell; once a card reaches it, it immediately flies off-screen.
    const picked = choices.find((choice) => !claimedExitTargets.has(keyOf(choice.target)))
      || choices[0]
      || fallbackChoices.find((choice) => !claimedExitTargets.has(keyOf(choice.target)))
      || fallbackChoices[0];
    plan.target = picked.target;
    if (!sameCell(plan.target, start) && choices.includes(picked)) claimedExitTargets.add(keyOf(plan.target));
  });

  // Phase 1: exits move in parallel. Survivors are hard blockers; exits that reached
  // the one-cell outside buffer are released and immediately continue their screen-exit animation.
  planParallelMoves({
    plans,
    movingIds: exitPlans.map((plan) => plan.id),
    currentCells,
    targets: new Map(exitPlans.map((plan) => [plan.id, plan.target])),
    bounds,
    blockedIds: new Set(survivorIds),
    maxTicks: metrics.rows + cols + 10,
  });
  exitPlans.forEach((plan) => {
    plan.fadeFromTick = plan.path.findIndex((cell) => sameCell(cell, plan.target));
    if (plan.fadeFromTick < 0) plan.fadeFromTick = plan.path.length - 1;
    currentCells.delete(plan.id);
  });

  let cursor = Math.max(0, ...exitPlans.map((plan) => plan.fadeFromTick)) + 1;
  const occupancy = new Map(survivorPlansRaw.map((plan) => [keyOf(currentCells.get(plan.id)), plan.id]));
  const expanding = nextHotels.length > oldHotels.length;
  const survivorPlans = survivorPlansRaw.sort((a, b) => (expanding ? b.order - a.order : a.order - b.order));

  survivorPlans.forEach((plan) => {
    const start = currentCells.get(plan.id);
    if (sameCell(start, plan.target)) {
      waitUntil(plan.path, cursor);
      return;
    }
    const subpath = findPath(start, (cell) => sameCell(cell, plan.target), occupancy, bounds);
    if (!subpath) {
      waitUntil(plan.path, cursor + 1);
      return;
    }
    cursor = movePlan(plan, cursor, subpath, occupancy, currentCells);
  });

  const usedEnterStarts = new Set();
  const enterPlans = [];
  nextHotels.forEach((hotel, index) => {
    const id = idOf(hotel);
    if (oldById.has(id)) return;
    const target = positionOf(index, cols);
    const start = uniqueEnterStart(target, cols, usedEnterStarts);
    const plan = {
      id,
      kind: 'enter',
      ghost: enterGhosts.get(id),
      path: [{ ...start }],
      target,
      order: index,
      fadeInTick: 0,
    };
    plans.push(plan);
    enterPlans.push(plan);
    currentCells.set(id, start);
  });

  // Enters are serialized through the same occupancy table. This is deliberately
  // conservative: new cards must not pass through settled survivors or each other.
  enterPlans
    .sort((a, b) => a.order - b.order)
    .forEach((plan) => {
      waitUntil(plan.path, cursor);
      plan.fadeInTick = cursor;
      const start = currentCells.get(plan.id);
      const subpath = findPath(start, (cell) => sameCell(cell, plan.target), occupancy, bounds);
      if (!subpath) {
        waitUntil(plan.path, cursor + 1);
        return;
      }
      cursor = movePlan(plan, cursor, subpath, occupancy, currentCells);
    });

  const totalTicks = Math.max(0, ...plans.map((plan) => plan.path.length - 1));
  plans.forEach((plan) => waitUntil(plan.path, totalTicks));

  if (import.meta.env.DEV && !validatePlans(plans)) {
    console.warn('[grid-filter-animation] path validation reported overlapping cells');
  }

  return { plans, totalTicks };
}

function transformFor(plan, metrics, tick) {
  const firstPoint = cellToPoint(metrics, plan.path[0]);
  const startX = plan.startRect?.left ?? firstPoint.x;
  const startY = plan.startRect?.top ?? firstPoint.y;
  const cell = plan.path[Math.min(tick, plan.path.length - 1)];
  const point = cellToPoint(metrics, cell);
  return `translate(${point.x - startX}px, ${point.y - startY}px)`;
}

function keyframesFor(plan, metrics, totalTicks) {
  const frames = [];
  const totalDuration = Math.max(1, totalTicks * STEP_DELAY_MS);
  for (let tick = 0; tick <= totalTicks; tick += 1) {
    const holdOffset = tick === 0 ? 0 : ((tick - 1) * STEP_DELAY_MS + STEP_DELAY_MS * STEP_MOVE_RATIO) / totalDuration;
    const endOffset = tick * STEP_DELAY_MS / totalDuration;
    let opacity = 1;
    if (plan.kind === 'enter' && tick <= (plan.fadeInTick ?? 0)) opacity = 0;
    const frame = { transform: transformFor(plan, metrics, tick), opacity };
    if (tick > 0) frames.push({ ...frame, offset: holdOffset });
    frames.push({ ...frame, offset: endOffset });
  }
  return frames;
}

function exitKeyframesFor(plan, metrics, activeTicks, pathDuration, totalDuration) {
  const pathFrames = keyframesFor(plan, metrics, activeTicks).map((frame) => ({
    ...frame,
    offset: Math.min(1, (frame.offset ?? 0) * pathDuration / totalDuration),
  }));
  const firstPoint = cellToPoint(metrics, plan.path[0]);
  const startX = plan.startRect?.left ?? firstPoint.x;
  const startY = plan.startRect?.top ?? firstPoint.y;
  const last = plan.path[Math.min(activeTicks, plan.path.length - 1)];
  const point = cellToPoint(metrics, last);
  const base = `translate(${point.x - startX}px, ${point.y - startY}px)`;
  const vector = exitVector(plan, metrics);
  const exitStartOffset = pathDuration / totalDuration;

  if (pathFrames.length) pathFrames[pathFrames.length - 1].offset = exitStartOffset;

  return [
    ...pathFrames,
    { transform: base, opacity: 1, offset: exitStartOffset },
    { transform: `${base} translate(${vector.x}px, ${vector.y}px)`, opacity: 1, offset: 1 },
  ];
}

function exitVector(plan, metrics) {
  const last = plan.path[plan.path.length - 1];
  const point = cellToPoint(metrics, last);
  const rect = plan.startRect;
  if (!rect) return { x: 0, y: 0 };

  if (last.c < 0) return { x: -(point.x + rect.width + EXIT_SCREEN_MARGIN), y: 0 };
  if (last.c >= metrics.cols) {
    return { x: window.scrollX + window.innerWidth + EXIT_SCREEN_MARGIN - point.x, y: 0 };
  }
  return { x: 0, y: window.scrollY + window.innerHeight + EXIT_SCREEN_MARGIN - point.y };
}

function movingRanges(path) {
  const ranges = [];
  let start = null;
  for (let tick = 1; tick < path.length; tick += 1) {
    const moving = !sameCell(path[tick], path[tick - 1]);
    if (moving && start === null) start = tick - 1;
    if ((!moving || tick === path.length - 1) && start !== null) {
      ranges.push({ from: start, to: moving && tick === path.length - 1 ? tick : tick - 1 });
      start = null;
    }
  }
  return ranges.filter((range) => range.to > range.from);
}

function moveDuration(steps, plan) {
  const duration = Math.min(MAX_MOVE_DURATION, MOVE_BASE_MS + MOVE_STEP_MS * Math.pow(steps, MOVE_POWER));
  return plan?.kind === 'exit' ? duration * EXIT_DURATION_SCALE : duration;
}


function isDebugEnabled() {
  if (typeof window === 'undefined') return false;
  const params = new URLSearchParams(window.location.search);
  return params.has('debugGrid') || window.localStorage.getItem('debugGridAnimation') === '1';
}

function clearDebugOverlay() {
  document.querySelectorAll('.grid-filter-debug-overlay').forEach((el) => el.remove());
}

function validateAndReport(plans) {
  const maxTicks = Math.max(0, ...plans.map((p) => p.path.length - 1));
  const collisions = [];
  for (let tick = 0; tick <= maxTicks; tick += 1) {
    const occupied = new Map();
    for (const plan of plans) {
      if ((plan.kind === 'exit' && tick >= plan.fadeFromTick) || (plan.kind === 'enter' && tick <= plan.fadeInTick)) {
        continue;
      }
      const cell = plan.path[Math.min(tick, plan.path.length - 1)];
      const key = keyOf(cell);
      if (occupied.has(key)) {
        collisions.push({ tick, cell: key, a: occupied.get(key), b: plan.id });
      }
      occupied.set(key, plan.id);
    }
  }
  return collisions;
}

function drawDebugOverlay(plans, metrics) {
  if (!isDebugEnabled()) return;
  clearDebugOverlay();

  const overlay = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  overlay.classList.add('grid-filter-debug-overlay');
  Object.assign(overlay.style, {
    position: 'absolute',
    left: '0',
    top: '0',
    width: `${Math.max(document.documentElement.scrollWidth, window.innerWidth)}px`,
    height: `${Math.max(document.documentElement.scrollHeight, window.innerHeight)}px`,
    zIndex: 80,
    pointerEvents: 'none',
    overflow: 'visible',
  });

  const colors = ['#ef4444', '#f97316', '#eab308', '#22c55e', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899'];
  plans.forEach((plan, index) => {
    const color = colors[index % colors.length];
    const points = plan.path.map((cell) => {
      const p = cellToPoint(metrics, cell);
      return `${p.x + metrics.cardWidth / 2},${p.y + metrics.cardHeight / 2}`;
    });

    const polyline = document.createElementNS('http://www.w3.org/2000/svg', 'polyline');
    polyline.setAttribute('points', points.join(' '));
    polyline.setAttribute('fill', 'none');
    polyline.setAttribute('stroke', color);
    polyline.setAttribute('stroke-width', plan.kind === 'exit' ? '4' : '3');
    polyline.setAttribute('stroke-linecap', 'round');
    polyline.setAttribute('stroke-linejoin', 'round');
    polyline.setAttribute('opacity', plan.kind === 'exit' ? '0.85' : '0.7');
    overlay.appendChild(polyline);

    plan.path.forEach((cell, tick) => {
      if (tick % 2 !== 0 && tick !== plan.path.length - 1) return;
      const p = cellToPoint(metrics, cell);
      const dot = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      dot.setAttribute('cx', `${p.x + metrics.cardWidth / 2}`);
      dot.setAttribute('cy', `${p.y + metrics.cardHeight / 2}`);
      dot.setAttribute('r', tick === 0 ? '5' : '3');
      dot.setAttribute('fill', color);
      dot.setAttribute('opacity', '0.75');
      overlay.appendChild(dot);
    });

    const first = cellToPoint(metrics, plan.path[0]);
    const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    label.setAttribute('x', `${first.x + 8}`);
    label.setAttribute('y', `${first.y + 18}`);
    label.setAttribute('fill', color);
    label.setAttribute('font-size', '13');
    label.setAttribute('font-weight', '700');
    label.textContent = `${plan.kind}:${plan.id}`;
    overlay.appendChild(label);
  });

  document.body.appendChild(overlay);

  const rows = plans.map((plan) => ({
    id: plan.id,
    kind: plan.kind,
    ticks: plan.path.length - 1,
    start: keyOf(plan.path[0]),
    end: keyOf(plan.path[plan.path.length - 1]),
    path: plan.path.map(keyOf).join(' -> '),
  }));
  const collisions = validateAndReport(plans);
  window.__gridFilterDebug = { plans, rows, collisions, metrics };
  console.group('[grid-filter-animation] trajectory debug');
  console.table(rows);
  if (collisions.length) console.table(collisions);
  else console.info('No same-tick cell collisions detected.');
  console.info('Debug data is also available at window.__gridFilterDebug');
  console.info('Disable with: localStorage.removeItem("debugGridAnimation") and remove ?debugGrid=1');
  console.groupEnd();
}

export function useGridFilterAnimation({ hotels, gridRef, stageRef, filterAnimating }) {
  let cleanup = [];
  let firstPaint = true;

  function cleanupGhosts() {
    cleanup.forEach((fn) => fn());
    cleanup = [];
    document.querySelectorAll('.grid-filter-ghost').forEach((el) => el.remove());
    if (!isDebugEnabled()) clearDebugOverlay();
  }

  async function animateTo(nextHotels) {
    const gridEl = gridRef.value;
    if (!gridEl || prefersReducedMotion()) {
      hotels.value = nextHotels;
      firstPaint = false;
      return;
    }

    const oldHotels = [...hotels.value];
    if (firstPaint || oldHotels.length === 0) {
      hotels.value = nextHotels;
      firstPaint = false;
      return;
    }

    const oldShots = snapshotCards(gridEl, oldHotels);
    if (!oldShots.length) {
      hotels.value = nextHotels;
      firstPaint = false;
      return;
    }

    cleanupGhosts();
    filterAnimating.value = true;

    const oldGhosts = new Map();
    oldShots.forEach((shot) => {
      oldGhosts.set(shot.id, cloneGhost(shot.el, shot.rect));
    });

    const stageEl = stageRef.value;
    const oldStageHeight = stageEl?.getBoundingClientRect().height || 0;
    const oldMinHeight = stageEl?.style.minHeight || '';
    if (stageEl) {
      stageEl.style.minHeight = `${oldStageHeight}px`;
      cleanup.push(() => {
        stageEl.style.minHeight = oldMinHeight;
      });
    }

    try {
      hotels.value = nextHotels;
      await nextTick();

      const newShots = snapshotCards(gridEl, nextHotels);
      const newStageHeight = stageEl?.getBoundingClientRect().height || 0;
      if (stageEl) stageEl.style.minHeight = `${Math.max(oldStageHeight, newStageHeight)}px`;

      const newShotById = new Map(newShots.map((shot) => [shot.id, shot]));
      const enterGhosts = new Map();
      nextHotels.forEach((hotel) => {
        const id = idOf(hotel);
        if (oldGhosts.has(id)) return;
        const shot = newShotById.get(id);
        if (!shot) return;
        const ghost = cloneGhost(shot.el, shot.rect);
        ghost.style.opacity = '0';
        enterGhosts.set(id, ghost);
      });

      const cols = getGridCols(gridEl);
      const metrics = getMetrics(gridEl, oldShots, newShots, cols, Math.max(oldHotels.length, nextHotels.length));
      const { plans, totalTicks } = buildPlans({
        oldShots,
        oldHotels,
        nextHotels,
        cols,
        metrics,
        oldGhosts,
        enterGhosts,
      });

      drawDebugOverlay(plans, metrics);

      const animations = plans
        .filter((plan) => plan.ghost)
        .map((plan) => {
          const activeTicks = plan.kind === 'exit' ? (plan.fadeFromTick ?? totalTicks) : totalTicks;
          const pathDuration = Math.max(520, activeTicks * STEP_DELAY_MS);
          const duration = plan.kind === 'exit' ? pathDuration + EXIT_SCREEN_DURATION : pathDuration;
          const keyframes = plan.kind === 'exit'
            ? exitKeyframesFor(plan, metrics, activeTicks, pathDuration, duration)
            : keyframesFor(plan, metrics, activeTicks);

          return plan.ghost
            .animate(keyframes, {
              duration,
              easing: EASING,
              fill: 'forwards',
            })
            .finished.catch(() => {});
        });

      await Promise.all(animations);
    } finally {
      cleanupGhosts();
      filterAnimating.value = false;
      firstPaint = false;
    }
  }

  onBeforeUnmount(() => {
    cleanupGhosts();
  });

  return { animateTo };
}
