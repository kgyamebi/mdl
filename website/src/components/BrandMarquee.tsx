import { brands } from '../data/brands';

type BrandMarqueeProps = {
  reverse?: boolean;
  speed?: number;
};

function logoSrc(slug: string) {
  // Official SVG logo marks / brand wordmarks — no boxes.
  return `/brand-marks/${slug}.svg`;
}

export function BrandMarquee({ reverse = false, speed = 42 }: BrandMarqueeProps) {
  const row = [...brands, ...brands];

  return (
    <div className={`marquee${reverse ? ' marquee--reverse' : ''}`} style={{ ['--marquee-duration' as string]: `${speed}s` }}>
      <div className="marquee__track" aria-hidden={false}>
        {row.map((brand, index) => (
          <div className="marquee__item" key={`${brand.slug}-${index}`} title={brand.name}>
            <img
              src={logoSrc(brand.slug)}
              alt={index < brands.length ? brand.name : ''}
              loading="lazy"
            />
          </div>
        ))}
      </div>
    </div>
  );
}
