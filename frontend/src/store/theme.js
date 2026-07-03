import { reactive } from 'vue';

/** 主题状态：深/浅色，写在 <html data-theme> 上，md3.css 按此切换令牌 */
export const theme = reactive({ mode: 'light' });

export function initTheme() {
  theme.mode = localStorage.getItem('theme')
    || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  document.documentElement.dataset.theme = theme.mode;
}

/**
 * 切换主题：以点击点为圆心向外扩散出新配色（View Transitions API 圆形揭示）。
 * 不支持的浏览器或用户关闭动画时直接切换。
 */
export function toggleTheme(event) {
  const next = theme.mode === 'dark' ? 'light' : 'dark';
  const apply = () => {
    theme.mode = next;
    localStorage.setItem('theme', next);
    document.documentElement.dataset.theme = next;
  };

  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (!document.startViewTransition || reduced) {
    apply();
    return;
  }

  const x = event?.clientX ?? window.innerWidth / 2;
  const y = event?.clientY ?? 0;
  const radius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y));

  document.startViewTransition(apply).ready.then(() => {
    document.documentElement.animate(
      { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
      { duration: 560, easing: 'ease-in-out', pseudoElement: '::view-transition-new(root)' },
    );
  });
}
