import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

export function CopilotFloatingButton() {
  const { hasPermission } = useAuth();
  const location = useLocation();

  if (!hasPermission('copilot:use') || location.pathname === '/copilot') {
    return null;
  }

  return (
    <NavLink to="/copilot" className="copilot-fab" aria-label="Ask MDL Assistant">
      <span className="copilot-fab__icon" aria-hidden="true">
        💬
      </span>
      <span className="copilot-fab__label">Ask MDL ASSISTANT</span>
    </NavLink>
  );
}
