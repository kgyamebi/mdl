import { Link } from 'react-router-dom';
import { site } from '../data/site';
import { seoPages } from '../data/seoPages';

const featuredSeo = [
  ...seoPages.filter((p) =>
    [
      'electrical-contractor-supplies-ghana',
      'buy-lights-accra-for-house-project',
      'send-electricals-to-ghana',
      'ghana-lighting-for-diaspora-builds',
      'wholesale-led-ghana',
      'best-lighting-shop-accra',
      'hotel-lighting-ghana',
      'philips-led-ghana',
      'schneider-breaker-ghana',
      'building-electrical-materials-ghana',
      'security-lights-ghana',
      'lighting-shop-accra',
    ].includes(p.slug),
  ),
];

export function Footer() {
  return (
    <footer className="footer">
      <div className="container footer__grid">
        <div>
          <p className="brand-mark__logo footer__logo" data-keep-case="true">
            {site.logo}
          </p>
          <h2>{site.name}</h2>
          <p className="footer__lede">
            quality lighting and electricals since {site.founded}. competitive prices. come and see for
            yourself.
          </p>
        </div>

        <div>
          <h3>visit</h3>
          <p>{site.coverage}</p>
          <p>{site.hours}</p>
          <p>{site.hoursNote}</p>
        </div>

        <div>
          <h3>contact</h3>
          <p>
            <a href={site.telUrl}>{site.phoneDisplay}</a>
          </p>
          <p>
            <a href={site.whatsappUrl} target="_blank" rel="noreferrer">
              whatsapp
            </a>
          </p>
          <p>
            <a href={site.emailUrl}>{site.email}</a>
          </p>
        </div>

        <div>
          <h3>explore</h3>
          <p>
            <Link to="/about">about</Link>
          </p>
          <p>
            <Link to="/ranges">ranges</Link>
          </p>
          <p>
            <Link to="/brands">brands</Link>
          </p>
          <p>
            <Link to="/contact">contact</Link>
          </p>
        </div>
      </div>

      <div className="container footer__seo">
        <details className="footer__seo-details">
          <summary>popular searches</summary>
          <div className="footer__seo-links">
            {featuredSeo.map((page) => (
              <Link key={page.slug} to={`/${page.slug}`}>
                {page.h1}
              </Link>
            ))}
          </div>
          <p className="footer__seo-more">
            <Link to="/ranges">see product ranges</Link>
            <span aria-hidden>·</span>
            <Link to="/brands">browse brands</Link>
            <span aria-hidden>·</span>
            <Link to="/contact">get directions help</Link>
          </p>
        </details>
      </div>

      <div className="container footer__bottom">
        <span>
          © {new Date().getFullYear()} {site.name}. all rights reserved.
        </span>
        <span>publicity site · visit shops nationwide for pricing & availability</span>
      </div>
    </footer>
  );
}
