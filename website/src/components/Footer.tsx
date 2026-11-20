import { Link } from 'react-router-dom';
import { site } from '../data/site';
import { seoPages } from '../data/seoPages';

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
          <p>{site.address.mainBranch}</p>
          <p>{site.address.shopsAtMain}</p>
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
        <h3>popular searches</h3>
        <div className="footer__seo-links">
          {seoPages.map((page) => (
            <Link key={page.slug} to={`/${page.slug}`}>
              {page.h1}
            </Link>
          ))}
        </div>
      </div>

      <div className="container footer__bottom">
        <span>
          © {new Date().getFullYear()} {site.name}. all rights reserved.
        </span>
        <span>publicity site · visit our shops nationwide for pricing & availability</span>
      </div>
    </footer>
  );
}
