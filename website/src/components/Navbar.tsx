import { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { BrandMark } from './BrandMark';
import { site } from '../data/site';

const links = [
  { to: '/', label: 'home' },
  { to: '/about', label: 'about' },
  { to: '/ranges', label: 'ranges' },
  { to: '/brands', label: 'brands' },
  { to: '/contact', label: 'contact' },
];

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : '';
    return () => {
      document.body.style.overflow = '';
    };
  }, [open]);

  return (
    <header className={`nav${scrolled ? ' nav--solid' : ''}`}>
      <div className="container nav__inner">
        <BrandMark onClick={() => setOpen(false)} />

        <nav className="nav__links" aria-label="Primary">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.to === '/'}>
              {link.label}
            </NavLink>
          ))}
        </nav>

        <a className="btn btn--gold nav__cta" href={site.whatsappUrl} target="_blank" rel="noreferrer">
          whatsapp
        </a>

        <button
          type="button"
          className={`nav__toggle${open ? ' is-open' : ''}`}
          aria-label={open ? 'Close menu' : 'Open menu'}
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
        >
          <span />
          <span />
        </button>
      </div>

      <AnimatePresence>
        {open && (
          <motion.div
            className="nav__mobile"
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
          >
            {links.map((link, index) => (
              <motion.div
                key={link.to}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.05 * index }}
              >
                <NavLink to={link.to} end={link.to === '/'} onClick={() => setOpen(false)}>
                  {link.label}
                </NavLink>
              </motion.div>
            ))}
            <a className="btn btn--gold" href={site.telUrl} onClick={() => setOpen(false)}>
              call {site.phoneDisplay}
            </a>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
