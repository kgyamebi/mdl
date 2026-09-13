import { test, expect, devices } from '@playwright/test';

/**
 * Mobile regression: finger-scrolling a product list must NOT count as a tap/select.
 * Uses a harness that mirrors POS VirtualList touch capture + TouchSelectGuard rules.
 */
test.use({
  ...devices['Pixel 5'],
  browserName: 'chromium',
});

const harness = `<!doctype html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <style>
    #list {
      height: 280px;
      overflow-y: auto;
      -webkit-overflow-scrolling: touch;
      touch-action: pan-y;
      border: 1px solid #333;
    }
    .row {
      height: 64px;
      box-sizing: border-box;
      padding: 12px;
      border-bottom: 1px solid #444;
      touch-action: pan-y;
      user-select: none;
    }
  </style>
</head>
<body>
  <div id="status">idle</div>
  <div id="select-count">0</div>
  <div id="list" data-testid="pos-virtual-list"></div>
  <script>
    class TouchSelectGuard {
      constructor() {
        this.dragged = false;
        this.suppressUntil = 0;
        this.suppressMs = 800;
        this.dragThresholdPx = 8;
      }
      beginTouchGesture() { this.dragged = false; }
      markDrag() {
        this.dragged = true;
        this.suppressUntil = Date.now() + this.suppressMs;
      }
      canSelectFromTouch() {
        return !this.dragged && Date.now() >= this.suppressUntil;
      }
      canSelectFromMouse(isCoarse) {
        if (isCoarse) return false;
        return Date.now() >= this.suppressUntil;
      }
    }

    const guard = new TouchSelectGuard();
    const list = document.getElementById('list');
    const status = document.getElementById('status');
    const countEl = document.getElementById('select-count');
    let selectCount = 0;
    let ignoreNextClick = false;
    const isCoarse = () => window.matchMedia('(pointer: coarse)').matches;

    for (let i = 0; i < 40; i++) {
      const row = document.createElement('div');
      row.className = 'row';
      row.dataset.testid = 'pos-product-card';
      row.textContent = 'Product ' + (i + 1);
      row.addEventListener('pointerup', (e) => {
        if (e.pointerType !== 'touch' && e.pointerType !== 'pen') return;
        ignoreNextClick = true;
        setTimeout(() => { ignoreNextClick = false; }, 500);
        if (!guard.canSelectFromTouch()) {
          status.textContent = 'scroll-ignored';
          return;
        }
        selectCount += 1;
        countEl.textContent = String(selectCount);
        status.textContent = 'selected';
      });
      row.addEventListener('click', (e) => {
        if (ignoreNextClick || isCoarse()) {
          e.preventDefault();
          status.textContent = status.textContent === 'selected' ? 'selected' : 'click-ignored';
          return;
        }
        if (!guard.canSelectFromMouse(false)) return;
        selectCount += 1;
        countEl.textContent = String(selectCount);
        status.textContent = 'selected';
      });
      list.appendChild(row);
    }

    let startX = 0, startY = 0, tracking = false, dragging = false;
    list.addEventListener('touchstart', (e) => {
      if (e.touches.length !== 1) return;
      tracking = true; dragging = false;
      startX = e.touches[0].clientX; startY = e.touches[0].clientY;
      guard.beginTouchGesture();
    }, { capture: true, passive: true });
    list.addEventListener('touchmove', (e) => {
      if (!tracking || e.touches.length !== 1) return;
      const dx = e.touches[0].clientX - startX;
      const dy = e.touches[0].clientY - startY;
      if (!dragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) {
        dragging = true;
        guard.markDrag();
        status.textContent = 'dragging';
      }
    }, { capture: true, passive: true });
    list.addEventListener('scroll', () => {
      guard.markDrag();
      status.textContent = 'scrolling';
    }, { passive: true });
  </script>
</body>
</html>`;

test.describe('POS mobile list scroll vs tap', () => {
  test('finger scroll does not register as a product tap', async ({ page }) => {
    await page.setContent(harness);
    const list = page.getByTestId('pos-virtual-list');
    await expect(list).toBeVisible();

    const box = await list.boundingBox();
    if (!box) {
      throw new Error('list not measurable');
    }

    const startX = box.x + box.width / 2;
    const startY = box.y + box.height / 2;

    await page.evaluate(({ x, y }) => {
      const target = document.elementFromPoint(x, y);
      const listEl = document.getElementById('list');
      if (!target || !listEl) {
        throw new Error('missing target');
      }
      const fire = (type, clientY, touches) => {
        const touch = new Touch({
          identifier: 1,
          target,
          clientX: x,
          clientY,
          pageX: x,
          pageY: clientY,
          radiusX: 1,
          radiusY: 1,
          rotationAngle: 0,
          force: 1,
        });
        const init = {
          bubbles: true,
          cancelable: true,
          touches: touches ? [touch] : [],
          targetTouches: touches ? [touch] : [],
          changedTouches: [touch],
        };
        listEl.dispatchEvent(new TouchEvent(type, init));
        target.dispatchEvent(new TouchEvent(type, init));
      };

      fire('touchstart', y, true);
      for (let i = 1; i <= 12; i++) {
        fire('touchmove', y - i * 12, true);
      }
      listEl.scrollTop = 180;
      listEl.dispatchEvent(new Event('scroll', { bubbles: true }));
      fire('touchend', y - 144, false);

      target.dispatchEvent(
        new PointerEvent('pointerup', {
          bubbles: true,
          cancelable: true,
          pointerType: 'touch',
          clientX: x,
          clientY: y - 144,
        }),
      );
      target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    }, { x: startX, y: startY });

    await expect(page.locator('#select-count')).toHaveText('0');
    const status = await page.locator('#status').textContent();
    expect(['scrolling', 'dragging', 'scroll-ignored', 'click-ignored']).toContain(status);
  });

  test('a stationary finger tap still selects', async ({ page }) => {
    await page.setContent(harness);
    const list = page.getByTestId('pos-virtual-list');
    const box = await list.boundingBox();
    if (!box) {
      throw new Error('list not measurable');
    }
    const x = box.x + box.width / 2;
    const y = box.y + 40;

    await page.evaluate(({ x, y }) => {
      const target = document.elementFromPoint(x, y);
      const listEl = document.getElementById('list');
      if (!target || !listEl) {
        throw new Error('missing target');
      }
      const touch = new Touch({
        identifier: 1,
        target,
        clientX: x,
        clientY: y,
        pageX: x,
        pageY: y,
        radiusX: 1,
        radiusY: 1,
        rotationAngle: 0,
        force: 1,
      });
      const startInit = {
        bubbles: true,
        cancelable: true,
        touches: [touch],
        targetTouches: [touch],
        changedTouches: [touch],
      };
      listEl.dispatchEvent(new TouchEvent('touchstart', startInit));
      target.dispatchEvent(
        new PointerEvent('pointerup', {
          bubbles: true,
          cancelable: true,
          pointerType: 'touch',
          clientX: x,
          clientY: y,
        }),
      );
    }, { x, y });

    await expect(page.locator('#select-count')).toHaveText('1');
    await expect(page.locator('#status')).toHaveText('selected');
  });
});
