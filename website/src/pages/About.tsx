import { Reveal } from '../components/Reveal';
import { SeoHead } from '../components/SeoHead';
import { site } from '../data/site';

export function About() {
  return (
    <>
      <SeoHead
        title="about moderndreamlight | since 2000 · ghana"
        description="moderndreamlight is a ghanaian lighting and electrical house with shops nationwide. main branch at opera square, accra — 2 shops. since 2000."
        path="/about"
        keywords={['about moderndreamlight', 'lighting company ghana', 'opera square electrical']}
      />
      <section className="page-hero page-hero--slim">
        <div className="page-hero__media">
          <img
            src="https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1800&q=80"
            alt=""
          />
        </div>
        <div className="container page-hero__content">
          <p className="kicker">about</p>
          <h1>since {site.founded}</h1>
        </div>
      </section>

      <section className="section">
        <div className="container about-grid">
          <Reveal>
            <p className="kicker">our story</p>
            <h2>{site.name}</h2>
            <p className="about-copy">
              established in {site.founded}, {site.name} is a ghanaian company with shops nationwide. our
              main branch is at opera square in accra — and that branch alone has 2 shops. for more than two
              decades we have supplied quality lighting and electrical products to homeowners, electricians,
              contractors, and project buyers.
            </p>
            <p className="about-copy">
              from lights and lamps to cables, switches, fans, circuit protection, and distribution boards —
              we focus on dependable brands at competitive prices, with staff who know the stock.
            </p>
          </Reveal>
          <Reveal delay={0.12}>
            <aside className="about-panel">
              <h3>at a glance</h3>
              <ul>
                <li>
                  <strong>founded</strong>
                  <span>{site.founded}</span>
                </li>
                <li>
                  <strong>coverage</strong>
                  <span>{site.coverage}</span>
                </li>
                <li>
                  <strong>main branch</strong>
                  <span>
                    {site.address.landmark} — {site.address.shopsAtMain}
                  </span>
                </li>
                <li>
                  <strong>hours</strong>
                  <span>{site.hours}</span>
                </li>
                <li>
                  <strong>phone</strong>
                  <span>{site.phoneDisplay}</span>
                </li>
              </ul>
            </aside>
          </Reveal>
        </div>
      </section>

      <section className="section" style={{ paddingTop: 0 }}>
        <div className="container values">
          {[
            {
              title: 'quality first',
              text: 'recognisable brands and practical ranges customers trust for ghanaian homes and sites.',
            },
            {
              title: 'competitive prices',
              text: 'visit any of our shops nationwide, or the opera square main branch (2 shops there), for fair pricing — not an online shop.',
            },
            {
              title: 'people who know stock',
              text: 'whether you need a single fitting or materials for a project, our team helps you choose.',
            },
          ].map((item, index) => (
            <Reveal key={item.title} delay={0.08 * index}>
              <article>
                <h3>{item.title}</h3>
                <p>{item.text}</p>
              </article>
            </Reveal>
          ))}
        </div>
      </section>
    </>
  );
}
