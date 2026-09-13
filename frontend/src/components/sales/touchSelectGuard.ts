/**
 * Distinguishes intentional taps from scroll/drag gestures on touch UIs.
 * Mobile browsers often fire a synthetic click after a finger-scroll; this guard
 * blocks that click and only allows selection when the gesture never dragged.
 */
export class TouchSelectGuard {
  private dragged = false;
  private suppressUntil = 0;

  constructor(
    private readonly options: {
      suppressMs?: number;
      dragThresholdPx?: number;
    } = {},
  ) {}

  get dragThresholdPx(): number {
    return this.options.dragThresholdPx ?? 8;
  }

  get suppressMs(): number {
    return this.options.suppressMs ?? 800;
  }

  /** Start of a new finger gesture. */
  beginTouchGesture(): void {
    this.dragged = false;
  }

  noteMovement(deltaX: number, deltaY: number): void {
    if (
      Math.abs(deltaX) > this.dragThresholdPx ||
      Math.abs(deltaY) > this.dragThresholdPx
    ) {
      this.markDrag();
    }
  }

  markDrag(): void {
    this.dragged = true;
    this.suppressUntil = Date.now() + this.suppressMs;
  }

  /** After a scroll/drag, ghost taps must be ignored. */
  isSuppressed(now = Date.now()): boolean {
    return this.dragged || now < this.suppressUntil;
  }

  /**
   * Touch/pen pointerup may select only if this gesture never dragged
   * and we are outside the post-scroll suppress window.
   */
  canSelectFromTouch(now = Date.now()): boolean {
    return !this.dragged && now >= this.suppressUntil;
  }

  /**
   * Mouse clicks: never on coarse pointers (phones). Fine pointers OK
   * unless still in post-scroll suppress (rare on desktop).
   */
  canSelectFromMouse(isCoarsePointer: boolean, now = Date.now()): boolean {
    if (isCoarsePointer) {
      return false;
    }
    return now >= this.suppressUntil;
  }
}
