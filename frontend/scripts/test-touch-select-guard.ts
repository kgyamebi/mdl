import assert from 'node:assert/strict';
import { TouchSelectGuard } from '../src/components/sales/touchSelectGuard';

function test(name: string, fn: () => void) {
  try {
    fn();
    console.log(`PASS ${name}`);
  } catch (error) {
    console.error(`FAIL ${name}`);
    throw error;
  }
}

test('clean tap allows touch select', () => {
  const guard = new TouchSelectGuard({ suppressMs: 800, dragThresholdPx: 8 });
  guard.beginTouchGesture();
  assert.equal(guard.canSelectFromTouch(), true);
});

test('finger drag blocks touch select', () => {
  const guard = new TouchSelectGuard({ suppressMs: 800, dragThresholdPx: 8 });
  guard.beginTouchGesture();
  guard.noteMovement(0, 20);
  assert.equal(guard.canSelectFromTouch(), false);
  assert.equal(guard.isSuppressed(), true);
});

test('small jitter under threshold still allows tap', () => {
  const guard = new TouchSelectGuard({ suppressMs: 800, dragThresholdPx: 8 });
  guard.beginTouchGesture();
  guard.noteMovement(0, 4);
  assert.equal(guard.canSelectFromTouch(), true);
});

test('ghost click after scroll is blocked on coarse pointers', () => {
  const guard = new TouchSelectGuard({ suppressMs: 800, dragThresholdPx: 8 });
  guard.beginTouchGesture();
  guard.markDrag();
  // New gesture begins (as a follow-up tap would)...
  guard.beginTouchGesture();
  // ...but suppress window from the scroll still blocks selection.
  assert.equal(guard.canSelectFromMouse(true), false);
  assert.equal(guard.canSelectFromTouch(), false);
});

test('after suppress window expires, clean tap works again', () => {
  const guard = new TouchSelectGuard({ suppressMs: 50, dragThresholdPx: 8 });
  const start = Date.now();
  guard.beginTouchGesture();
  guard.markDrag();
  guard.beginTouchGesture();
  assert.equal(guard.canSelectFromTouch(start + 10), false);
  assert.equal(guard.canSelectFromTouch(start + 60), true);
  assert.equal(guard.canSelectFromMouse(false, start + 60), true);
});

test('fine pointer mouse click allowed when not suppressed', () => {
  const guard = new TouchSelectGuard();
  assert.equal(guard.canSelectFromMouse(false), true);
});

console.log('All TouchSelectGuard tests passed.');
