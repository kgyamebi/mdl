import { Link } from 'react-router-dom';
import { site } from '../data/site';

type BrandMarkProps = {
  to?: string;
  onClick?: () => void;
  compact?: boolean;
};

export function BrandMark({ to = '/', onClick, compact = false }: BrandMarkProps) {
  return (
    <Link to={to} className={`brand-mark${compact ? ' brand-mark--compact' : ''}`} onClick={onClick}>
      <span className="brand-mark__logo" data-keep-case="true">
        {site.logo}
      </span>
      {!compact && <span className="brand-mark__name">{site.name}</span>}
    </Link>
  );
}
