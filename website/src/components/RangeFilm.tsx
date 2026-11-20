import { useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion, useReducedMotion, useScroll, useTransform } from 'framer-motion';
import type { Range } from '../data/ranges';

type RangeFilmProps = {
  range: Range;
  index: number;
  href?: string;
  asLink?: boolean;
};

export function RangeFilm({ range, index, href = '/ranges', asLink = true }: RangeFilmProps) {
  const ref = useRef<HTMLDivElement>(null);
  const reduce = useReducedMotion();
  const odd = index % 2 === 1;

  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ['start end', 'end start'],
  });
  const imgY = useTransform(scrollYProgress, [0, 1], reduce ? [0, 0] : [40, -40]);
  const imgScale = useTransform(scrollYProgress, [0, 0.45, 1], reduce ? [1, 1, 1] : [1.1, 1.02, 1.08]);

  const body = (
    <>
      <div className={`range-film__media range-film__media--${range.fit}`}>
        <motion.div className="range-film__parallax" style={{ y: imgY, scale: imgScale }}>
          <picture>
            <source srcSet={range.imageWebp} type="image/webp" />
            <img
              src={range.image}
              alt=""
              loading="lazy"
              decoding="async"
              width={1600}
              height={900}
              style={range.focus ? { objectPosition: range.focus } : undefined}
            />
          </picture>
        </motion.div>
      </div>
      <motion.div
        className="range-film__meta"
        initial={reduce ? false : { opacity: 0, y: 24 }}
        whileInView={reduce ? undefined : { opacity: 1, y: 0 }}
        viewport={{ once: true, amount: 0.45 }}
        transition={{ duration: 0.65, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
      >
        <span>0{index + 1}</span>
        <h3>{range.title}</h3>
        <p>{range.blurb}</p>
        {asLink && <span className="range-film__cue">view ranges →</span>}
      </motion.div>
    </>
  );

  return (
    <motion.div
      ref={ref}
      className={`range-film${odd ? ' range-film--flip' : ''}`}
      initial={reduce ? false : { opacity: 0, y: 56 }}
      whileInView={reduce ? undefined : { opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.18 }}
      transition={{ duration: 0.85, delay: Math.min(index * 0.04, 0.28), ease: [0.22, 1, 0.36, 1] }}
      whileHover={reduce ? undefined : { y: -4 }}
    >
      {asLink ? (
        <Link to={href} className="range-film__link">
          {body}
        </Link>
      ) : (
        <div className="range-film__link">{body}</div>
      )}
    </motion.div>
  );
}
