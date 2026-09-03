import { MDL_MARK_PATH, MDL_MARK_VIEWBOX } from './brandMark';

type MdlLogoVariant = 'full' | 'sidebar' | 'auth' | 'compact';
type MdlLogoTone = 'auto' | 'light' | 'dark';

interface MdlLogoProps {
  variant?: MdlLogoVariant;
  tone?: MdlLogoTone;
  className?: string;
}

function MdlMark({ className = '' }: { className?: string }) {
  return (
    <svg
      className={`mdl-brand__mark ${className}`.trim()}
      viewBox={MDL_MARK_VIEWBOX}
      fill="currentColor"
      aria-hidden="true"
    >
      <path d={MDL_MARK_PATH} />
    </svg>
  );
}

export function MdlLogo({ variant = 'full', tone = 'auto', className = '' }: MdlLogoProps) {
  return (
    <div
      className={`mdl-brand mdl-brand--${variant} mdl-brand--tone-${tone} ${className}`.trim()}
      role="img"
      aria-label="modern DL"
    >
      <MdlMark />
      <span className="mdl-brand__word">modern</span>
      <span className="mdl-brand__box">DL</span>
    </div>
  );
}

export function MdlIcon({ className = '', tone = 'auto' }: { className?: string; tone?: MdlLogoTone }) {
  return (
    <span
      className={`mdl-brand-icon mdl-brand--tone-${tone} ${className}`.trim()}
      role="img"
      aria-label="modern DL"
    >
      <MdlMark />
    </span>
  );
}
