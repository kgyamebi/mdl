import { useEffect, useMemo, useRef, useState, type CSSProperties, type ReactNode, type UIEvent } from 'react';

interface VirtualListProps<T> {
  items: T[];
  itemHeight: number;
  height: number;
  overscan?: number;
  className?: string;
  style?: CSSProperties;
  renderItem: (item: T, index: number, style: CSSProperties) => ReactNode;
  /** Only scroll the list to this index when set (e.g. keyboard nav). Omit during touch scroll. */
  scrollToIndex?: number | null;
  onScrollIndexChange?: (index: number) => void;
  /** Fired as soon as a finger drag/scroll is detected (before click). */
  onScrollGesture?: () => void;
}

const DRAG_THRESHOLD_PX = 8;

export function VirtualList<T>({
  items,
  itemHeight,
  height,
  overscan = 6,
  className,
  style,
  renderItem,
  scrollToIndex,
  onScrollIndexChange,
  onScrollGesture,
}: VirtualListProps<T>) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [scrollTop, setScrollTop] = useState(0);
  const prevLengthRef = useRef(items.length);
  const programmaticScrollRef = useRef(false);
  const onScrollGestureRef = useRef(onScrollGesture);
  onScrollGestureRef.current = onScrollGesture;

  useEffect(() => {
    if (items.length < prevLengthRef.current && containerRef.current) {
      const maxScroll = Math.max(0, items.length * itemHeight - height);
      if (containerRef.current.scrollTop > maxScroll) {
        containerRef.current.scrollTop = maxScroll;
        setScrollTop(maxScroll);
      }
    }
    prevLengthRef.current = items.length;
  }, [items.length, itemHeight, height]);

  // Capture-phase touch tracking: when the list scrolls, buttons never get pointermove,
  // so we must detect drag here and suppress the ghost click that follows.
  useEffect(() => {
    const el = containerRef.current;
    if (!el) {
      return;
    }

    let startX = 0;
    let startY = 0;
    let dragging = false;
    let tracking = false;

    const markDrag = () => {
      if (dragging) {
        return;
      }
      dragging = true;
      onScrollGestureRef.current?.();
    };

    const onTouchStart = (event: TouchEvent) => {
      if (event.touches.length !== 1) {
        return;
      }
      tracking = true;
      dragging = false;
      startX = event.touches[0].clientX;
      startY = event.touches[0].clientY;
    };

    const onTouchMove = (event: TouchEvent) => {
      if (!tracking || event.touches.length !== 1) {
        return;
      }
      const dx = Math.abs(event.touches[0].clientX - startX);
      const dy = Math.abs(event.touches[0].clientY - startY);
      if (dx > DRAG_THRESHOLD_PX || dy > DRAG_THRESHOLD_PX) {
        markDrag();
      }
    };

    const onTouchEnd = () => {
      tracking = false;
    };

    const onTouchCancel = () => {
      tracking = false;
      markDrag();
    };

    el.addEventListener('touchstart', onTouchStart, { passive: true, capture: true });
    el.addEventListener('touchmove', onTouchMove, { passive: true, capture: true });
    el.addEventListener('touchend', onTouchEnd, { passive: true, capture: true });
    el.addEventListener('touchcancel', onTouchCancel, { passive: true, capture: true });

    return () => {
      el.removeEventListener('touchstart', onTouchStart, true);
      el.removeEventListener('touchmove', onTouchMove, true);
      el.removeEventListener('touchend', onTouchEnd, true);
      el.removeEventListener('touchcancel', onTouchCancel, true);
    };
  }, [items.length, height]);

  const totalHeight = items.length * itemHeight;
  const maxStart = Math.max(0, items.length - 1);
  const startIndex = Math.min(maxStart, Math.max(0, Math.floor(scrollTop / itemHeight) - overscan));
  const visibleCount = Math.ceil(height / itemHeight) + overscan * 2;
  const endIndex = Math.min(items.length, startIndex + visibleCount);

  const visibleItems = useMemo(
    () => items.slice(startIndex, endIndex).map((item, offset) => ({ item, index: startIndex + offset })),
    [items, startIndex, endIndex],
  );

  useEffect(() => {
    if (scrollToIndex == null || !containerRef.current || items.length === 0) {
      return;
    }
    const clamped = Math.max(0, Math.min(scrollToIndex, items.length - 1));
    const top = clamped * itemHeight;
    const viewBottom = containerRef.current.scrollTop + height;
    if (top < containerRef.current.scrollTop || top + itemHeight > viewBottom) {
      programmaticScrollRef.current = true;
      containerRef.current.scrollTop = Math.max(0, top - itemHeight);
      setScrollTop(containerRef.current.scrollTop);
      window.setTimeout(() => {
        programmaticScrollRef.current = false;
      }, 0);
    }
  }, [scrollToIndex, itemHeight, height, items.length]);

  function handleScroll(event: UIEvent<HTMLDivElement>) {
    const nextTop = event.currentTarget.scrollTop;
    setScrollTop(nextTop);
    onScrollIndexChange?.(Math.floor(nextTop / itemHeight));
    if (!programmaticScrollRef.current) {
      onScrollGestureRef.current?.();
    }
  }

  return (
    <div
      ref={containerRef}
      className={className}
      style={{
        ...style,
        height,
        overflowY: 'auto',
        overflowX: 'hidden',
        position: 'relative',
        WebkitOverflowScrolling: 'touch',
        touchAction: 'pan-y',
        overscrollBehavior: 'contain',
      }}
      onScroll={handleScroll}
      role="listbox"
    >
      <div style={{ height: totalHeight, position: 'relative' }}>
        {visibleItems.map(({ item, index }) =>
          renderItem(item, index, {
            position: 'absolute',
            top: index * itemHeight,
            left: 0,
            right: 0,
            height: itemHeight,
          }),
        )}
      </div>
    </div>
  );
}
