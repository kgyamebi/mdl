import { Reveal } from '../components/Reveal';
import { SeoHead } from '../components/SeoHead';
import { RangeFilm } from '../components/RangeFilm';
import { ranges } from '../data/ranges';
import { site } from '../data/site';

export function Ranges() {
  return (
    <>
      <SeoHead
        title="product ranges | moderndreamlight lighting & electricals"
        description="lighting, cables, switches, fans, circuit protection, boards and conduit from moderndreamlight. shops nationwide."
        path={"/ranges"}
        keywords={['lighting ranges ghana', 'electrical products accra', 'cables switches fans']}
      />
      <section className="page-hero page-hero--slim">
        <div className="page-hero__media">
          <img src="/ranges/lighting.opt.jpg" alt="" />
        </div>
        <div className="container page-hero__content">
          <p className="kicker">ranges</p>
          <h1>what we carry</h1>
        </div>
      </section>

      <section className="ranges-cinematic">
        <div className="container ranges-cinematic__head">
          <Reveal>
            <p className="kicker">showroom ranges</p>
            <h2>from lights to cables</h2>
            <p className="brand-index__lede">
              category overviews only. for models, prices and availability, visit our shops nationwide or
              whatsapp {site.phoneDisplay}.
            </p>
          </Reveal>
        </div>
        <div className="ranges-cinematic__list">
          {ranges.map((range, index) => (
            <RangeFilm key={range.id} range={range} index={index} asLink={false} />
          ))}
        </div>
      </section>
    </>
  );
}
