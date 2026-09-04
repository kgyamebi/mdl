import { useCallback, useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

const STORAGE_KEY = 'mdl-copilot-fab-position';
const DRAG_THRESHOLD_PX = 6;

interface FabPosition {
  x: number;
  y: number;
}

function readStoredPosition(): FabPosition | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as FabPosition;
    if (typeof parsed.x !== 'number' || typeof parsed.y !== 'number') {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function writeStoredPosition(position: FabPosition): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(position));
  } catch {
    // Ignore quota / private mode errors.
  }
}

function getSafeAreaBottom(): number {
  if (typeof document === 'undefined') {
    return 0;
  }
  const probe = document.createElement('div');
  probe.style.position = 'fixed';
  probe.style.bottom = '0';
  probe.style.paddingBottom = 'env(safe-area-inset-bottom, 0px)';
  probe.style.visibility = 'hidden';
  probe.style.pointerEvents = 'none';
  document.body.appendChild(probe);
  const inset = probe.getBoundingClientRect().height;
  probe.remove();
  return inset;
}

function getBottomNavHeight(): number {
  if (typeof document === 'undefined') {
    return 0;
  }
  const nav = document.querySelector('.layout__bottom-nav');
  if (!nav || getComputedStyle(nav).display === 'none') {
    return 0;
  }
  return nav.getBoundingClientRect().height;
}

function getStickyFooterHeight(): number {
  if (typeof document === 'undefined') {
    return 0;
  }
  const posSubmit = document.querySelector('.pos-form__submit');
  if (!posSubmit) {
    return 0;
  }
  return posSubmit.getBoundingClientRect().height;
}

function getBottomInset(): number {
  const margin = 12;
  const navHeight = getBottomNavHeight();
  if (navHeight > 0) {
    return navHeight + margin;
  }
  const stickyFooter = getStickyFooterHeight();
  if (stickyFooter > 0) {
    return stickyFooter + margin;
  }
  return margin + getSafeAreaBottom();
}

function getWorkspaceDetailInset(): number {
  if (typeof document === 'undefined') {
    return 0;
  }
  const detail = document.querySelector('.workspace-split--open .workspace-split__detail');
  if (!detail) {
    return 0;
  }
  const rect = detail.getBoundingClientRect();
  if (rect.width <= 0 || rect.left >= window.innerWidth - 40) {
    return 0;
  }
  return Math.min(rect.width + 24, window.innerWidth * 0.45);
}

function getDefaultPosition(width: number, height: number): FabPosition {
  const margin = 16;
  const bottomInset = getBottomInset();
  const detailInset = getWorkspaceDetailInset();
  return {
    x: Math.max(margin, window.innerWidth - width - margin - detailInset),
    y: Math.max(margin, window.innerHeight - height - bottomInset),
  };
}

function clampPosition(position: FabPosition, width: number, height: number): FabPosition {
  const margin = 8;
  const bottomInset = getBottomInset();
  const detailInset = getWorkspaceDetailInset();
  const maxX = Math.max(margin, window.innerWidth - width - margin - detailInset);
  const maxY = Math.max(margin, window.innerHeight - height - bottomInset);
  return {
    x: Math.min(Math.max(margin, position.x), maxX),
    y: Math.min(Math.max(margin, position.y), maxY),
  };
}

export function CopilotFloatingButton() {
  const { hasPermission } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const fabRef = useRef<HTMLButtonElement>(null);
  const dragRef = useRef({
    active: false,
    moved: false,
    pointerId: -1,
    startX: 0,
    startY: 0,
    originX: 0,
    originY: 0,
  });

  const [position, setPosition] = useState<FabPosition | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [hasCustomPosition, setHasCustomPosition] = useState(false);

  const measureAndPlace = useCallback(() => {
    const element = fabRef.current;
    if (!element) {
      return;
    }
    const { width, height } = element.getBoundingClientRect();
    const stored = readStoredPosition();
    if (stored) {
      setHasCustomPosition(true);
      setPosition(clampPosition(stored, width, height));
      return;
    }
    setHasCustomPosition(false);
    setPosition(getDefaultPosition(width, height));
  }, []);

  useEffect(() => {
    measureAndPlace();
  }, [measureAndPlace, location.pathname]);

  useEffect(() => {
    function handleResize() {
      const element = fabRef.current;
      if (!element || position == null) {
        return;
      }
      const { width, height } = element.getBoundingClientRect();
      setPosition((current) => (current ? clampPosition(current, width, height) : current));
    }

    window.addEventListener('resize', handleResize);

    const observed = [
      document.querySelector('.layout__bottom-nav'),
      document.querySelector('.pos-form__submit'),
      document.querySelector('.workspace-split--open'),
    ].filter((node): node is Element => node != null);

    const observer =
      observed.length > 0
        ? new ResizeObserver(() => {
            measureAndPlace();
          })
        : null;

    observed.forEach((node) => observer?.observe(node));

    return () => {
      window.removeEventListener('resize', handleResize);
      observer?.disconnect();
    };
  }, [measureAndPlace, location.pathname]);

  if (!hasPermission('copilot:use') || location.pathname === '/copilot') {
    return null;
  }

  function persistPosition(next: FabPosition) {
    writeStoredPosition(next);
    setHasCustomPosition(true);
  }

  function handlePointerDown(event: ReactPointerEvent<HTMLButtonElement>) {
    if (event.button !== 0 || position == null) {
      return;
    }

    dragRef.current = {
      active: true,
      moved: false,
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      originX: position.x,
      originY: position.y,
    };
    setIsDragging(true);
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLButtonElement>) {
    const drag = dragRef.current;
    if (!drag.active || drag.pointerId !== event.pointerId || position == null) {
      return;
    }

    const deltaX = event.clientX - drag.startX;
    const deltaY = event.clientY - drag.startY;
    if (!drag.moved && (Math.abs(deltaX) > DRAG_THRESHOLD_PX || Math.abs(deltaY) > DRAG_THRESHOLD_PX)) {
      drag.moved = true;
    }
    if (!drag.moved) {
      return;
    }

    const element = fabRef.current;
    if (!element) {
      return;
    }
    const { width, height } = element.getBoundingClientRect();
    const next = clampPosition(
      { x: drag.originX + deltaX, y: drag.originY + deltaY },
      width,
      height,
    );
    setPosition(next);
  }

  function finishDrag(event: ReactPointerEvent<HTMLButtonElement>) {
    const drag = dragRef.current;
    if (!drag.active || drag.pointerId !== event.pointerId) {
      return;
    }

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }

    if (drag.moved && position != null) {
      persistPosition(position);
    }

    dragRef.current.active = false;
    setIsDragging(false);

    window.setTimeout(() => {
      dragRef.current.moved = false;
    }, 0);
  }

  function handleClick() {
    if (dragRef.current.moved) {
      return;
    }
    navigate('/copilot');
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLButtonElement>) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      navigate('/copilot');
    }
  }

  return (
    <button
      ref={fabRef}
      type="button"
      className={`copilot-fab${position ? ' copilot-fab--ready' : ''}${isDragging ? ' copilot-fab--dragging' : ''}${
        hasCustomPosition ? ' copilot-fab--custom' : ''
      }`}
      style={
        position
          ? { left: `${position.x}px`, top: `${position.y}px`, right: 'auto', bottom: 'auto' }
          : undefined
      }
      aria-label="Ask MDL AI Assistant. Drag to reposition."
      aria-grabbed={isDragging}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={finishDrag}
      onPointerCancel={finishDrag}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
    >
      <span className="copilot-fab__glow" aria-hidden="true" />
      <span className="copilot-fab__content">
        <strong className="copilot-fab__label">Ask MDL AI Assistant</strong>
        <span className="copilot-fab__emoji" aria-hidden="true">
          {' '}
          💬
        </span>
      </span>
    </button>
  );
}
