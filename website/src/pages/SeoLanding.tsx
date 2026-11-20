import { Link, Navigate, useParams } from 'react-router-dom';
import { Reveal } from '../components/Reveal';
import { SeoHead } from '../components/SeoHead';
import { getSeoPage, seoPages } from '../data/seoPages';
import { site } from '../data/site';

export function SeoLanding() {
  const { slug = '' } = useParams();
  const page = getSeoPage(slug);

  if (!page) {
    return <Navigate to="/" replace />;
  }

  const related = seoPages.filter((item) => item.slug !== page.slug).slice(0, 8);

  return (
    <>
      <SeoHead
        title={page.title}
        description={page.description}
        path={`/${page.slug}`}
        keywords={page.keywords}
      />

      <section className="page-hero page-hero--slim">
        <div className="page-hero__media">
          <img src="/ranges/lighting.jpg" alt="" />
        </div>
        <div className="container page-hero__content">
          <p className="kicker">moderndreamlight</p>
          <h1>{page.h1}</h1>
        </div>
      </section>

      <section className="section seo-page">
        <div className="container seo-page__grid">
          <Reveal>
            <p className="seo-page__intro">{page.intro}</p>
            {page.sections.map((section) => (
              <article key={section.heading} className="seo-page__block">
                <h2>{section.heading}</h2>
                <p>{section.body}</p>
              </article>
            ))}
            <div className="seo-page__cta">
              <a className="btn btn--gold" href={site.whatsappUrl} target="_blank" rel="noreferrer">
                whatsapp {site.phoneDisplay}
              </a>
              <Link className="btn btn--ghost" to="/contact">
                contact details
              </Link>
            </div>
          </Reveal>

          <aside className="seo-page__aside">
            <h2>explore more</h2>
            <ul>
              {related.map((item) => (
                <li key={item.slug}>
                  <Link to={`/${item.slug}`}>{item.h1}</Link>
                </li>
              ))}
            </ul>
            <p className="seo-page__aside-note">
              {site.coverage}. {site.address.mainBranch}. {site.address.shopsAtMain}.
            </p>
          </aside>
        </div>
      </section>
    </>
  );
}
