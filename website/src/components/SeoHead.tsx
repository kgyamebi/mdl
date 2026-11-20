import { useEffect } from 'react';
import { site } from '../data/site';

type SeoHeadProps = {
  title: string;
  description: string;
  path: string;
  keywords?: string[];
};

export function SeoHead({ title, description, path, keywords = [] }: SeoHeadProps) {
  useEffect(() => {
    document.title = title;

    const ensure = (selector: string, create: () => HTMLElement) => {
      let el = document.head.querySelector(selector) as HTMLElement | null;
      if (!el) {
        el = create();
        document.head.appendChild(el);
      }
      return el;
    };

    const desc = ensure('meta[name="description"]', () => {
      const meta = document.createElement('meta');
      meta.setAttribute('name', 'description');
      return meta;
    });
    desc.setAttribute('content', description);

    const kw = ensure('meta[name="keywords"]', () => {
      const meta = document.createElement('meta');
      meta.setAttribute('name', 'keywords');
      return meta;
    });
    kw.setAttribute('content', keywords.join(', '));

    const canonical = ensure('link[rel="canonical"]', () => {
      const link = document.createElement('link');
      link.setAttribute('rel', 'canonical');
      return link;
    }) as HTMLLinkElement;
    canonical.href = `https://moderndreamlight.com${path}`;

    const ogTitle = ensure('meta[property="og:title"]', () => {
      const meta = document.createElement('meta');
      meta.setAttribute('property', 'og:title');
      return meta;
    });
    ogTitle.setAttribute('content', title);

    const ogDesc = ensure('meta[property="og:description"]', () => {
      const meta = document.createElement('meta');
      meta.setAttribute('property', 'og:description');
      return meta;
    });
    ogDesc.setAttribute('content', description);

    const ld = ensure('script[data-seo-jsonld]', () => {
      const script = document.createElement('script');
      script.type = 'application/ld+json';
      script.setAttribute('data-seo-jsonld', 'true');
      return script;
    });
    ld.textContent = JSON.stringify({
      '@context': 'https://schema.org',
      '@type': 'Store',
      name: site.name,
      alternateName: site.logo,
      url: 'https://moderndreamlight.com',
      email: site.email,
      telephone: site.phone,
      foundingDate: String(site.founded),
      address: {
        '@type': 'PostalAddress',
        streetAddress: site.address.line1,
        addressLocality: 'Accra',
        addressCountry: 'GH',
      },
      openingHours: 'Mo-Sa 08:00-16:30',
      areaServed: 'Ghana',
    });
  }, [title, description, path, keywords]);

  return null;
}
