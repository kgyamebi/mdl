import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion, useReducedMotion, useScroll, useTransform } from 'framer-motion';
import { site } from '../data/site';

const VIDEO_SRC = '/media/hero.mp4';
const VIDEO_FALLBACK =
  'https://videos.pexels.com/video-files/7578552/7578552-hd_1920_1080_30fps.mp4';
const POSTER =
  'https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?auto=format&fit=crop&w=2000&q=80';

export function CinematicHero() {
  const sectionRef = useRef<HTMLElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const reduce = useReducedMotion();

  const { scrollYProgress } = useScroll({
    target: sectionRef,
    offset: ['start start', 'end start'],
  });
  const mediaY = useTransform(scrollYProgress, [0, 1], reduce ? [0, 0] : [0, 120]);
  const contentY = useTransform(scrollYProgress, [0, 1], reduce ? [0, 0] : [0, 70]);
  const contentOpacity = useTransform(scrollYProgress, [0, 0.75], [1, 0]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || reduce) return;
    const play = () => {
      void video.play().catch(() => undefined);
    };
    play();
    video.addEventListener('loadeddata', play);
    return () => video.removeEventListener('loadeddata', play);
  }, [reduce]);

  return (
    <section className="hero" ref={sectionRef}>
      <motion.div className="hero__media" style={{ y: mediaY }}>
        {!reduce ? (
          <video
            ref={videoRef}
            className="hero__video"
            autoPlay
            muted
            loop
            playsInline
            poster={POSTER}
            preload="metadata"
          >
            <source src={VIDEO_SRC} type="video/mp4" />
            <source src={VIDEO_FALLBACK} type="video/mp4" />
          </video>
        ) : (
          <img src={POSTER} alt="" />
        )}
        <div className="hero__veil" />
      </motion.div>

      <motion.div className="hero__content" style={{ y: contentY, opacity: contentOpacity }}>
        <motion.p
          className="hero__logo"
          data-keep-case="true"
          initial={reduce ? false : { opacity: 0, y: 28 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1.05, ease: [0.22, 1, 0.36, 1] }}
        >
          {site.logo}
        </motion.p>
        <motion.h1
          className="hero__brand"
          initial={reduce ? false : { opacity: 0, y: 36 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1.1, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
        >
          {site.name}
        </motion.h1>
        <motion.p
          className="hero__sub"
          initial={reduce ? false : { opacity: 0, y: 22 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.16, ease: [0.22, 1, 0.36, 1] }}
        >
          lighting & electricals · since {site.founded}
        </motion.p>
        <motion.p
          className="hero__lede"
          initial={reduce ? false : { opacity: 0, y: 22 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.24, ease: [0.22, 1, 0.36, 1] }}
        >
          26+ brands. full ranges. competitive prices.
          stock homeowners, electricians and contractors can trust.
        </motion.p>
        <motion.div
          className="hero__actions"
          initial={reduce ? false : { opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.85, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}
        >
          <Link className="btn btn--light" to="/ranges">
            explore ranges
          </Link>
          <a className="btn btn--ghost-light" href={site.whatsappUrl} target="_blank" rel="noreferrer">
            whatsapp {site.phoneDisplay}
          </a>
        </motion.div>
      </motion.div>
    </section>
  );
}
