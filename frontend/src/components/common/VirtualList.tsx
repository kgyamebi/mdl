import { useEffect, useMemo, useRef, useState, type CSSProperties, type ReactNode, type UIEvent } from 'react';

interface VirtualListProps<T> {
  items: T[];
  itemHeight: number;
  height: number;
  overscan?: number;
  className?: string;
  style?: CSSProperties;
  renderItem: (item: T, index: number, style: CSSProperties) => ReactNode;
  scrollToIndex?: number | null;
  onScrollIndexChange?: (index: number) => void;
}

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
}: VirtualListProps<T>) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [scrollTop, setScrollTop] = useState(0);
  const prevLengthRef = useRef(items.length);

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
      containerRef.current.scrollTop = Math.max(0, top - itemHeight);
      setScrollTop(containerRef.current.scrollTop);
    }
  }, [scrollToIndex, itemHeight, height, items.length]);

  function handleScroll(event: UIEvent<HTMLDivElement>) {
    const nextTop = event.currentTarget.scrollTop;
    setScrollTop(nextTop);
    onScrollIndexChange?.(Math.floor(nextTop / itemHeight));
  }

  return (
    <div
      ref={containerRef}
      className={className}
      style={{ ...style, height, overflowY: 'auto', position: 'relative' }}
      onScroll={handleScroll}
      role="presentation"
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
