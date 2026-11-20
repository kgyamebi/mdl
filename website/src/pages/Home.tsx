import { Link } from 'react-router-dom';
import { motion, useReducedMotion } from 'framer-motion';
import { Reveal } from '../components/Reveal';
import { CinematicHero } from '../components/CinematicHero';
import { BrandMarquee } from '../components/BrandMarquee';
import { SeoHead } from '../components/SeoHead';
import { RangeFilm } from '../components/RangeFilm';
import { AnimatedStat } from '../components/AnimatedStat';
import { ranges } from '../data/ranges';
import { site } from '../data/site';

export function Home() {
  const reduce = useReducedMotion();

  return (
    <>
      <SeoHead
        title="moderndreamlight | lighting & electricals for ghana"
        description="moderndreamlight — lighting, cables, switches, fans and electricals from 26+ trusted brands. shops nationwide. since 2000."
        path="/"
        keywords={[
          'lighting shop ghana',
          'electrical supplies ghana',
          'moderndreamlight',
          'led lights ghana',
          'contractor electrical supplies',
        ]}
      />
      <CinematicHero />

      <section className="strip">
        <div className="container strip__inner">
          <Reveal>
            <p className="kicker">built for real work</p>
          </Reveal>
          <Reveal delay={0.08}>
            <div className="strip__copy">
              <p>from a single fitting to a full project list.</p>
              <p>lighting that holds the room. wiring that holds the load.</p>
              <p>brands contractors already specify — ready in shops nationwide.</p>
            </div>
          </Reveal>
          <div className="strip__stats">
            {[
              { n: `${site.years}+`, l: 'years of supply' },
              { n: '26+', l: 'brands stocked' },
              { n: '8', l: 'product ranges' },
              { n: 'nationwide', l: 'shops' },
            ].map((item, i) => (
              <AnimatedStat key={item.l} value={item.n} label={item.l} delay={0.1 + i * 0.07} />
            ))}
          </div>
        </div>
      </section>

      <section className="brand-reel">
        <div className="container brand-reel__head">
          <Reveal>
            <p className="kicker">26+ brands</p>
            <h2>names your electrician already knows.</h2>
          </Reveal>
        </div>
        <motion.div
          initial={reduce ? false : { opacity: 0 }}
          whileInView={reduce ? undefined : { opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 1 }}
        >
          <BrandMarquee speed={48} />
          <BrandMarquee reverse speed={56} />
        </motion.div>
        <div className="container brand-reel__foot">
          <Reveal>
            <Link className="text-link" to="/brands">
              view full brand list →
            </Link>
          </Reveal>
        </div>
      </section>

      <section className="ranges-cinematic">
        <div className="container ranges-cinematic__head">
          <Reveal>
            <p className="kicker">the product story</p>
            <h2>light the space. power the build.</h2>
          </Reveal>
        </div>
        <div className="ranges-cinematic__list">
          {ranges.map((range, index) => (
            <RangeFilm key={range.id} range={range} index={index} />
          ))}
        </div>
      </section>

      <section className="visit-cinema">
        <motion.div
          className="visit-cinema__glow"
          aria-hidden
          animate={reduce ? undefined : { opacity: [0.35, 0.7, 0.35], scale: [1, 1.08, 1] }}
          transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}
        />
        <div className="container visit-cinema__inner">
          <Reveal y={40}>
            <p className="kicker">come see the stock</p>
            <h2>
              shops nationwide
              <em>walk in for brands, ranges and advice — pricing confirmed in-store</em>
              <em>{site.hours}</em>
            </h2>
            <motion.div
              className="visit-cinema__actions"
              initial={reduce ? false : { opacity: 0, y: 16 }}
              whileInView={reduce ? undefined : { opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.7, delay: 0.15 }}
            >
              <a className="btn btn--gold" href={site.whatsappUrl} target="_blank" rel="noreferrer">
                whatsapp us
              </a>
              <a className="btn btn--ghost" href={site.telUrl}>
                call {site.phoneDisplay}
              </a>
              <Link className="btn btn--ghost" to="/contact">
                contact details
              </Link>
            </motion.div>
          </Reveal>
        </div>
      </section>
    </>
  );
}
