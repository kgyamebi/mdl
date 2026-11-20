import { useState } from 'react';
import type { Brand } from '../data/brands';

type BrandLogoProps = {
  brand: Brand;
  compact?: boolean;
};

export function BrandLogo({ brand, compact = false }: BrandLogoProps) {
  const [failed, setFailed] = useState(false);

  return (
    <article className={`brand-card${compact ? ' brand-card--compact' : ''}`}>
      <div className="brand-card__mark" style={{ ['--brand-accent' as string]: brand.accent ?? '#d4a84b' }}>
        {!failed ? (
          <img
            src={`/brand-marks/${brand.slug}.svg`}
            alt={`${brand.name} logo`}
            className="brand-card__logo"
            loading="lazy"
            onError={() => setFailed(true)}
          />
        ) : (
          <span className="brand-card__initials" aria-hidden>
            {brand.name
              .split(/\s+/)
              .map((part) => part[0])
              .join('')
              .slice(0, 3)}
          </span>
        )}
      </div>
      <p className="brand-card__name">{brand.name}</p>
    </article>
  );
}
