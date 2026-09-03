interface DetailCloseButtonProps {
  onClose: () => void;
  label?: string;
}

export function DetailCloseButton({ onClose, label = 'Back to list' }: DetailCloseButtonProps) {
  return (
    <button type="button" className="detail-close btn btn--ghost" onClick={onClose}>
      ← {label}
    </button>
  );
}
