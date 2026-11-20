export type Range = {
  id: string;
  title: string;
  blurb: string;
  image: string;
  imageWebp: string;
  /** cover = lifestyle scene; contain = product on field */
  fit: 'cover' | 'contain';
  focus?: string;
};

/** Category overview only — local images verified to match each range. */
export const ranges: Range[] = [
  {
    id: 'lighting',
    title: 'lighting & lamps',
    blurb:
      'indoor and outdoor luminaires, leds, floodlights, and decorative fittings for homes and commercial spaces.',
    image: '/ranges/lighting.opt.jpg',
    imageWebp: '/ranges/lighting.webp',
    fit: 'cover',
    focus: 'center 35%',
  },
  {
    id: 'cables',
    title: 'cables & wiring',
    blurb: 'reliable wiring solutions for residential, commercial, and project installations.',
    image: '/ranges/cables.opt.jpg',
    imageWebp: '/ranges/cables.webp',
    fit: 'contain',
  },
  {
    id: 'switches',
    title: 'switches & sockets',
    blurb: 'wiring accessories and finishing fittings from trusted electrical brands.',
    image: '/ranges/switches.opt.jpg',
    imageWebp: '/ranges/switches.webp',
    fit: 'contain',
  },
  {
    id: 'fans',
    title: 'fans & ventilation',
    blurb: 'ceiling, wall, and orbit fans for comfort in ghanaian homes and workplaces.',
    image: '/ranges/fans.opt.jpg',
    imageWebp: '/ranges/fans.webp',
    fit: 'cover',
    focus: 'center 30%',
  },
  {
    id: 'protection',
    title: 'circuit protection',
    blurb: 'changeovers, breakers, and protection equipment for safer installations.',
    image: '/ranges/protection.opt.jpg',
    imageWebp: '/ranges/protection.webp',
    fit: 'contain',
  },
  {
    id: 'boards',
    title: 'distribution boards',
    blurb: 'spn and tpn boards for residential and light commercial distribution.',
    image: '/ranges/boards.opt.jpg',
    imageWebp: '/ranges/boards.webp',
    fit: 'contain',
  },
  {
    id: 'conduit',
    title: 'conduit & trunking',
    blurb: 'conduit lighting and trunking systems for neat, durable installs.',
    image: '/ranges/conduit.opt.jpg',
    imageWebp: '/ranges/conduit.webp',
    fit: 'contain',
  },
  {
    id: 'tools',
    title: 'tools & accessories',
    blurb: 'essential accessories that complete every electrical and lighting job.',
    image: '/ranges/tools.opt.jpg',
    imageWebp: '/ranges/tools.webp',
    fit: 'contain',
  },
];
