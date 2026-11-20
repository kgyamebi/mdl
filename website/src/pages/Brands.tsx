import { Reveal } from '../components/Reveal';
import { BrandMarquee } from '../components/BrandMarquee';
import { SeoHead } from '../components/SeoHead';
import { brands } from '../data/brands';

export function Brands() {
  return (
    <>
      <SeoHead
        title="brands we stock | moderndreamlight"
        description="philips, havells, schneider, osram, hager, legrand and more — brands stocked at moderndreamlight. shops nationwide. opera square main branch."
        path="/brands"
        keywords={['philips ghana', 'havells ghana', 'schneider ghana', 'lighting brands accra']}
      />
      <section className="page-hero page-hero--slim">
        <div className="page-hero__media">
          <img
            src="https://images.unsplash.com/photo-1565814329452-e1efa11c5b89?auto=format&fit=crop&w=1800&q=80"
            alt=""
          />
        </div>
        <div className="container page-hero__content">
          <p className="kicker">brands</p>
          <h1>names on our shelves</h1>
        </div>
      </section>

      <section className="brand-reel brand-reel--page">
        <BrandMarquee speed={40} />
        <BrandMarquee reverse speed={52} />
      </section>

      <section className="section brand-index">
        <div className="container">
          <Reveal>
            <p className="kicker">full list</p>
            <h2>all brands we publicise</h2>
            <p className="brand-index__lede">
              logos identify products we stock. availability is confirmed in-store at our shops nationwide,
              including the opera square main branch (2 shops).
            </p>
          </Reveal>

          <ul className="brand-index__list">
            {brands.map((brand, index) => (
              <Reveal key={brand.slug} delay={Math.min(index * 0.02, 0.3)} className="brand-index__row-wrap">
                <li className="brand-index__row">
                  <img src={`/brands/${brand.slug}.svg`} alt="" />
                  <span>{brand.name}</span>
                </li>
              </Reveal>
            ))}
          </ul>
        </div>
      </section>
    </>
  );
}
