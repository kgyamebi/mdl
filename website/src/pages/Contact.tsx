import { useState } from 'react';
import type { FormEvent } from 'react';
import { Reveal } from '../components/Reveal';
import { SeoHead } from '../components/SeoHead';
import { site } from '../data/site';

export function Contact() {
  const [sent, setSent] = useState(false);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const name = String(data.get('name') ?? '').trim();
    const message = String(data.get('message') ?? '').trim();
    const body = encodeURIComponent(
      `Hello ${site.name},\n\nMy name is ${name || 'a customer'}.\n\n${message || 'I would like to enquire about your products.'}\n`,
    );
    window.location.href = `${site.emailUrl}?subject=${encodeURIComponent('Website enquiry')}&body=${body}`;
    setSent(true);
  }

  return (
    <>
      <SeoHead
        title="contact moderndreamlight | opera square, accra"
        description={`contact moderndreamlight — ${site.phoneDisplay}, ${site.email}. shops nationwide. main branch opera square, accra. ${site.hours}.`}
        path="/contact"
        keywords={['contact moderndreamlight', 'opera square electrical phone', 'lighting shop accra contact']}
      />
      <section className="page-hero">
        <div className="page-hero__media">
          <img
            src="https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1800&q=80"
            alt="reception desk lighting"
          />
        </div>
        <div className="container page-hero__content">
          <p className="kicker">contact</p>
          <h1>talk to us</h1>
        </div>
      </section>

      <section className="section">
        <div className="container contact-grid">
          <Reveal>
            <p className="kicker">find us</p>
            <h2>{site.address.mainBranch}</h2>
            <div className="contact-list">
              <div>
                <span>coverage</span>
                <strong>{site.coverage}</strong>
              </div>
              <div>
                <span>main branch</span>
                <strong>
                  {site.address.landmark}
                  <br />
                  {site.address.shopsAtMain}
                </strong>
              </div>
              <div>
                <span>hours</span>
                <strong>
                  {site.hours}
                  <br />
                  {site.hoursNote}
                </strong>
              </div>
              <div>
                <span>phone / whatsapp</span>
                <strong>
                  <a href={site.telUrl}>{site.phoneDisplay}</a>
                </strong>
              </div>
              <div>
                <span>email</span>
                <strong>
                  <a href={site.emailUrl}>{site.email}</a>
                </strong>
              </div>
            </div>
            <div className="hero__actions" style={{ marginTop: '1.75rem' }}>
              <a className="btn btn--gold" href={site.whatsappUrl} target="_blank" rel="noreferrer">
                whatsapp
              </a>
              <a className="btn btn--ghost" href={site.mapsUrl} target="_blank" rel="noreferrer">
                open in maps
              </a>
            </div>
          </Reveal>

          <Reveal delay={0.1}>
            <form className="contact-form" onSubmit={handleSubmit}>
              <h3>send an enquiry</h3>
              <p>opens your email app with a message ready for {site.email}.</p>
              <label>
                name
                <input name="name" type="text" placeholder="your name" autoComplete="name" />
              </label>
              <label>
                message
                <textarea name="message" rows={5} placeholder="what are you looking for?" />
              </label>
              <button className="btn btn--gold" type="submit">
                continue in email
              </button>
              {sent && <p className="contact-form__note">if your email app did not open, write to {site.email}.</p>}
            </form>
          </Reveal>
        </div>
      </section>
    </>
  );
}
