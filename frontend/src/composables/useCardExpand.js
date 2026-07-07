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
  function makeGhost({ el, rect, display }, extraStyles = {}) {
    const ghost = el.cloneNode(true);
    ghost.classList.add('expand-ghost');
    Object.assign(
      ghost.style,
      {
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
      },
      extraStyles,
    );
    document.body.appendChild(ghost);
    return ghost;
  }

  function tempStyles(el, styles) {
    const previous = {};
    Object.keys(styles).forEach((key) => {
      previous[key] = el.style[key];
      el.style[key] = styles[key];
    });
    return () => {
      Object.entries(previous).forEach(([key, value]) => {
        el.style[key] = value;
      });
    };
  }

  const chromeEls = () => Array.from(document.querySelectorAll('[data-push-away]'));

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

    // “上方区块” = 页面栏目 + 完全位于被点卡片上方的卡片（如点第二排时的第一排）。
    // 它们必须作为刚性整体、用同一位移同步平移：布局中标题本就在卡片上方，
    // 整体移动相对位置不变，卡片才不会中途追上并压过标题。
    const aboveShots = siblingShots.filter((s) => s.rect.bottom <= cardRect.top + 1);
    const radialShots = siblingShots.filter((s) => s.rect.bottom > cardRect.top + 1);
    const blockShots = chromeShots.filter((s) => s.rect.height).concat(aboveShots);
    const blockDy = blockShots.length
      ? -(Math.max(...blockShots.map((s) => s.rect.bottom)) + 32)
      : 0;

    expandedId.value = id;
    document.documentElement.dataset.reading = 'true';
    await nextTick();

    if (prefersReduced) {
      window.scrollTo(0, 0);
      animating.value = false;
      return;
    }

    // 上方区块（栏目 + 上方整排卡片）：作为整体向上滑出，保持相对位置不变
    blockShots.forEach((shot) => {
      const ghost = makeGhost(shot);
      ghost
        .animate(
          [
            { transform: 'translateY(0)', opacity: 1 },
            { transform: `translateY(${blockDy}px)`, opacity: 0.9 },
          ],
          { duration: SIBLING_DURATION, easing: EASING, fill: 'forwards' },
        )
        .finished.finally(() => ghost.remove());
    });

    // 其余兄弟卡片：幽灵沿连线方向飞出视口
    radialShots.forEach((shot) => {
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
    // 阅读态的 min-height:100svh 会钳住关键帧里的 height，放大过程中先临时放开
    const restoreMinHeight = tempStyles(cardEl, { minHeight: '0' });
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
    restoreMinHeight();
    animating.value = false;
  }

  async function collapse() {
    if (animating.value || expandedId.value === null) return;
    animating.value = true;

    const cardEl = document.querySelector('[data-expand-card][data-expanded="true"]');
    if (!cardEl) {
      expandedId.value = null;
      delete document.documentElement.dataset.reading;
      animating.value = false;
      return;
    }

    const expandedRect = cardEl.getBoundingClientRect();

    if (prefersReduced) {
      expandedId.value = null;
      delete document.documentElement.dataset.reading;
      await nextTick();
      window.scrollTo(0, savedScrollY);
      animating.value = false;
      return;
    }

    const expandedStyle = getComputedStyle(cardEl);

    // 幽灵只取展开卡片与视口相交的“切片”（整卡内容往往远高于视口，整卡克隆会全程压住页面）：
    // 外层 fixed 容器负责裁剪与缩放，内层克隆按当前滚动偏移对位，保证幽灵画面 = 用户眼前画面
    const sliceTop = Math.max(expandedRect.top, 0);
    const sliceHeight = Math.min(expandedRect.bottom, window.innerHeight) - sliceTop;
    const cardGhost = document.createElement('div');
    cardGhost.className = 'expand-ghost';
    Object.assign(cardGhost.style, {
      position: 'fixed',
      left: `${expandedRect.left}px`,
      top: `${sliceTop}px`,
      width: `${expandedRect.width}px`,
      height: `${sliceHeight}px`,
      overflow: 'hidden',
      margin: '0',
      zIndex: 20,
      pointerEvents: 'none',
      transformOrigin: 'top left',
      willChange: 'transform, opacity',
      borderRadius: expandedStyle.borderRadius,
      background: expandedStyle.backgroundColor,
    });
    const cloneInner = cardEl.cloneNode(true);
    Object.assign(cloneInner.style, {
      position: 'absolute',
      left: '0',
      top: `${expandedRect.top - sliceTop}px`,
      width: `${expandedRect.width}px`,
      margin: '0',
      minHeight: '0', // 克隆带着 data-expanded 的 min-height:100svh，必须放开
      display: expandedStyle.display,
      visibility: 'visible',
    });
    cardGhost.appendChild(cloneInner);
    document.body.appendChild(cardGhost);

    expandedId.value = null;
    delete document.documentElement.dataset.reading;
    await nextTick();
    window.scrollTo(0, savedScrollY);
    await nextTick();

    const grid = cardEl.closest('[data-expand-grid]');
    const cardRect = cardEl.getBoundingClientRect();

    // 幽灵切片用 transform 缩放（不重排、字号随整体等比缩小）飞回列表原位，
    // 同时淡出，与其下方的真实列表卡片交叉淡化 —— 避免动画结束时文字尺寸跳变。
    const cardAnim = cardGhost.animate(
      [
        { transform: 'translate(0, 0) scale(1, 1)', opacity: 1 },
        {
          transform:
            `translate(${cardRect.left - expandedRect.left}px, ${cardRect.top - sliceTop}px) ` +
            `scale(${cardRect.width / expandedRect.width}, ${cardRect.height / sliceHeight})`,
          opacity: 0,
        },
      ],
      { duration: DURATION, easing: EASING, fill: 'forwards' },
    );

    // 回程同样分组：标题/搜索条 + 完全位于被点卡片上方的卡片（第一排）作为
    // 刚性整体、用同一位移从视口上方一起滑回 —— 相对位置不变，卡片永远压不到标题。
    const siblings = grid
      ? Array.from(grid.querySelectorAll('[data-expand-card]')).filter((el) => el !== cardEl)
      : [];
    const siblingRects = siblings.map((el) => ({ el, rect: el.getBoundingClientRect() }));
    const aboveBack = siblingRects.filter((s) => s.rect.bottom <= cardRect.top + 1);
    const radialBack = siblingRects.filter((s) => s.rect.bottom > cardRect.top + 1);
    const chromeBack = chromeEls()
      .map((el) => ({ el, rect: el.getBoundingClientRect() }))
      .filter((s) => s.rect.height);
    const blockBack = chromeBack.concat(aboveBack);
    const blockDy = blockBack.length
      ? -(Math.max(...blockBack.map((s) => s.rect.bottom)) + 32)
      : 0;

    // 滑回期间临时提升到收回幽灵（zIndex 20）之上，避免被幽灵盖住
    const blockRestores = [];
    const blockAnims = blockBack.map(({ el }) => {
      const styles = { zIndex: 40 };
      if (getComputedStyle(el).position === 'static') styles.position = 'relative';
      blockRestores.push(tempStyles(el, styles));
      return el
        .animate(
          [
            { transform: `translateY(${blockDy}px)`, opacity: 0.9 },
            { transform: 'translateY(0)', opacity: 1 },
          ],
          { duration: SIBLING_DURATION, easing: EASING },
        )
        .finished.catch(() => {});
    });

    // 其余兄弟卡片沿原连线方向飞回
    const siblingAnims = radialBack.map(({ el, rect }) => {
      const v = siblingVector(cardRect, rect);
      return el
        .animate(
          [
            { transform: `translate(${v.x}px, ${v.y}px)`, opacity: 0.9 },
            { transform: 'translate(0, 0)', opacity: 1 },
          ],
          { duration: SIBLING_DURATION, easing: EASING },
        )
        .finished.catch(() => {});
    });

    try {
      await Promise.all([cardAnim.finished.catch(() => {}), ...blockAnims, ...siblingAnims]);
    } finally {
      cardGhost.remove();
      blockRestores.forEach((restore) => restore());
      animating.value = false;
    }
  }

  onBeforeUnmount(() => {
    delete document.documentElement.dataset.reading;
    document.querySelectorAll('.expand-ghost').forEach((el) => el.remove());
  });

  return { expandedId, animating, expand, collapse };
}
