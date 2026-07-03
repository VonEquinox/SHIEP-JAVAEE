import { nextTick, onBeforeUnmount, ref } from 'vue';

/**
 * 卡片原地展开为全屏 —— 移植自博客 TrueWebSite 的 post-expander.js。
 *
 * 交互（需求文档 7.3 “有迹可循”）：
 * - 点击卡片：页面上方的栏目（标记 data-push-away：导航、标题、搜索条）被向上“推走”，
 *   其余卡片沿“被点卡片 → 各卡片”的连线方向飞出视口；被点卡片经 FLIP 放大铺满视口。
 * - 返回：上方栏目滑回、其余卡片沿原方向飞回、卡片收回列表原位。
 * - 尊重 prefers-reduced-motion：直接切换，无动画。
 *
 * 用法：卡片容器加 data-expand-grid，每张卡片加 data-expand-card，
 *       需要被推走的页面栏目加 data-push-away。
 */
export function useCardExpand() {
  const expandedId = ref(null);
  const animating = ref(false);
  let savedScrollY = 0;

  const prefersReduced =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const DURATION = 520;
  const SIBLING_DURATION = 460;
  const EASING = 'cubic-bezier(0.22, 1, 0.36, 1)';

  const exitDistance = () => Math.hypot(window.innerWidth, window.innerHeight) * 1.1;

  /** 兄弟卡片的退场方向：被点卡片中心 → 该卡片中心 的连线方向 */
  function siblingVector(fromRect, toRect) {
    const dx = toRect.left + toRect.width / 2 - (fromRect.left + fromRect.width / 2);
    const dy = toRect.top + toRect.height / 2 - (fromRect.top + fromRect.height / 2);
    const len = Math.hypot(dx, dy) || 1;
    return { x: (dx / len) * exitDistance(), y: (dy / len) * exitDistance() };
  }

  /** 记录元素进入阅读态前的几何信息（display 必须在隐藏前取） */
  const snapshot = (els) =>
    els.map((el) => ({
      el,
      rect: el.getBoundingClientRect(),
      display: getComputedStyle(el).display,
    }));

  /**
   * 幽灵替身：原元素被阅读态样式隐藏后，用 fixed 定位的克隆体完成退场动画。
   * display/visibility 走内联样式，避免被阅读态的隐藏规则命中。
   */
  function makeGhost({ el, rect, display }) {
    const ghost = el.cloneNode(true);
    ghost.classList.add('expand-ghost');
    Object.assign(ghost.style, {
      position: 'fixed',
      left: `${rect.left}px`,
      top: `${rect.top}px`,
      width: `${rect.width}px`,
      height: `${rect.height}px`,
      margin: '0',
      zIndex: 30,
      pointerEvents: 'none',
      display,
      visibility: 'visible',
    });
    document.body.appendChild(ghost);
    return ghost;
  }

  const chromeEls = () => Array.from(document.querySelectorAll('[data-push-away]'));

  /** 上方栏目被“推走”的距离：底边完全移出视口再留一点余量 */
  const pushOutY = (rect) => -(rect.bottom + 32);

  async function expand(id, cardEl) {
    if (animating.value || expandedId.value !== null) return;
    animating.value = true;
    savedScrollY = window.scrollY;

    const grid = cardEl.closest('[data-expand-grid]');
    const siblings = Array.from(grid.querySelectorAll('[data-expand-card]')).filter(
      (el) => el !== cardEl,
    );
    const cardRect = cardEl.getBoundingClientRect();
    const siblingShots = snapshot(siblings);
    const chromeShots = snapshot(chromeEls());

    expandedId.value = id;
    document.documentElement.dataset.reading = 'true';
    await nextTick();

    if (prefersReduced) {
      window.scrollTo(0, 0);
      animating.value = false;
      return;
    }

    // 上方栏目：幽灵向上滑出（被展开的内容“推走”，而不是瞬间消失）
    chromeShots.forEach((shot) => {
      if (!shot.rect.height) return;
      const ghost = makeGhost(shot);
      ghost
        .animate(
          [
            { transform: 'translateY(0)', opacity: 1 },
            { transform: `translateY(${pushOutY(shot.rect)}px)`, opacity: 0.9 },
          ],
          { duration: SIBLING_DURATION, easing: EASING, fill: 'forwards' },
        )
        .finished.finally(() => ghost.remove());
    });

    // 兄弟卡片：幽灵沿连线方向飞出视口
    siblingShots.forEach((shot) => {
      const ghost = makeGhost(shot);
      const v = siblingVector(cardRect, shot.rect);
      ghost
        .animate(
          [
            { transform: 'translate(0, 0)', opacity: 1 },
            { transform: `translate(${v.x}px, ${v.y}px)`, opacity: 0.9 },
          ],
          { duration: SIBLING_DURATION, easing: EASING, fill: 'forwards' },
        )
        .finished.finally(() => ghost.remove());
    });

    // 被点卡片：从列表原位（FLIP Invert）连续放大到全屏
    window.scrollTo(0, 0);
    const newRect = cardEl.getBoundingClientRect();
    const anim = cardEl.animate(
      [
        {
          transform: `translate(${cardRect.left - newRect.left}px, ${cardRect.top - newRect.top}px)`,
          width: `${cardRect.width}px`,
          height: `${cardRect.height}px`,
        },
        { transform: 'translate(0, 0)', width: `${newRect.width}px`, height: `${newRect.height}px` },
      ],
      { duration: DURATION, easing: EASING },
    );
    await anim.finished.catch(() => {});
    animating.value = false;
  }

  async function collapse() {
    if (animating.value || expandedId.value === null) return;
    animating.value = true;

    const cardEl = document.querySelector('[data-expand-card][data-expanded="true"]');
    const expandedRect = cardEl.getBoundingClientRect();

    expandedId.value = null;
    delete document.documentElement.dataset.reading;
    await nextTick();
    window.scrollTo(0, savedScrollY);

    if (prefersReduced) {
      animating.value = false;
      return;
    }

    const grid = cardEl.closest('[data-expand-grid]');
    const cardRect = cardEl.getBoundingClientRect();

    // 卡片从全屏收回列表原位
    cardEl.animate(
      [
        {
          transform: `translate(${expandedRect.left - cardRect.left}px, ${expandedRect.top - cardRect.top}px)`,
          width: `${expandedRect.width}px`,
          height: `${expandedRect.height}px`,
        },
        { transform: 'translate(0, 0)', width: `${cardRect.width}px`, height: `${cardRect.height}px` },
      ],
      { duration: DURATION, easing: EASING },
    );

    // 兄弟卡片沿原连线方向飞回
    Array.from(grid.querySelectorAll('[data-expand-card]'))
      .filter((el) => el !== cardEl)
      .forEach((el) => {
        const rect = el.getBoundingClientRect();
        const v = siblingVector(cardRect, rect);
        el.animate(
          [
            { transform: `translate(${v.x}px, ${v.y}px)`, opacity: 0.9 },
            { transform: 'translate(0, 0)', opacity: 1 },
          ],
          { duration: SIBLING_DURATION, easing: EASING },
        );
      });

    // 上方栏目从视口上方滑回原位
    chromeEls().forEach((el) => {
      const rect = el.getBoundingClientRect();
      if (!rect.height) return;
      el.animate(
        [
          { transform: `translateY(${pushOutY(rect)}px)`, opacity: 0.9 },
          { transform: 'translateY(0)', opacity: 1 },
        ],
        { duration: SIBLING_DURATION, easing: EASING },
      );
    });

    await new Promise((r) => setTimeout(r, DURATION));
    animating.value = false;
  }

  onBeforeUnmount(() => {
    delete document.documentElement.dataset.reading;
    document.querySelectorAll('.expand-ghost').forEach((el) => el.remove());
  });

  return { expandedId, animating, expand, collapse };
}
