import { site } from './site';

export type SeoPage = {
  slug: string;
  title: string;
  description: string;
  h1: string;
  intro: string;
  sections: { heading: string; body: string }[];
  keywords: string[];
};

const cta = `visit our shops nationwide, or the opera square main branch in accra (2 shops). call or whatsapp ${site.phoneDisplay}. email ${site.email}.`;

/** Local + long-tail pages for organic discovery (publicity site, no online shop). */
export const seoPages: SeoPage[] = [
  {
    slug: 'lighting-shop-accra',
    title: 'lighting shop accra | moderndreamlight opera square',
    description:
      'looking for a lighting shop in accra? moderndreamlight stocks lights, lamps and electricals. shops nationwide. main branch opera square with 2 shops.',
    h1: 'lighting shop in accra',
    intro:
      'if you searched for a lighting shop in accra, moderndreamlight is a ghanaian lighting and electrical house with shops nationwide. our main branch is at opera square, and that branch has 2 shops.',
    sections: [
      {
        heading: 'what you can find',
        body: 'indoor and outdoor lighting, led lamps, floodlights, decorative fittings and electrical accessories for homes, trade and projects.',
      },
      {
        heading: 'visit or message us',
        body: cta,
      },
    ],
    keywords: ['lighting shop accra', 'lights shop accra', 'buy lights accra'],
  },
  {
    slug: 'electrical-supplies-ghana',
    title: 'electrical supplies ghana | moderndreamlight',
    description:
      'electrical supplies in ghana from moderndreamlight — cables, switches, boards, fans and lighting. shops nationwide. main branch opera square accra.',
    h1: 'electrical supplies in ghana',
    intro:
      'moderndreamlight supplies quality electrical products across ghana. whether you are a contractor, electrician or homeowner, visit our shops nationwide or the opera square main branch.',
    sections: [
      {
        heading: 'ranges we stock',
        body: 'lighting and lamps, cables and wiring, switches and sockets, fans, circuit protection, distribution boards, conduit and trunking, plus tools and accessories.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['electrical supplies ghana', 'electrical shop ghana', 'buy electricals ghana'],
  },
  {
    slug: 'opera-square-lighting',
    title: 'opera square lighting shop | moderndreamlight',
    description:
      'moderndreamlight main branch is at opera square, accra — 2 shops at the branch. lighting and electricals since 2000. shops also nationwide.',
    h1: 'lighting at opera square, accra',
    intro:
      'opera square is a known hub for electrical and lighting shops in accra. moderndreamlight’s main branch is here, with 2 shops at the opera square branch, plus more shops nationwide.',
    sections: [
      {
        heading: 'why visit opera square',
        body: 'see products in person, ask staff about brands and fittings, and get competitive prices in-store. this website is for publicity only — pricing is confirmed at the shop.',
      },
      {
        heading: 'hours and contact',
        body: `${site.hours}. ${site.hoursNote}. ${cta}`,
      },
    ],
    keywords: ['opera square lighting', 'opera square electrical', 'lighting shop opera square'],
  },
  {
    slug: 'led-lights-accra',
    title: 'led lights accra | moderndreamlight',
    description:
      'looking for led lights in accra? moderndreamlight stocks lighting and lamps at shops nationwide. main branch opera square with 2 shops.',
    h1: 'led lights in accra',
    intro:
      'searching for led lights in accra? visit moderndreamlight for lighting options including leds and lamps. shops nationwide; main branch at opera square with 2 shops.',
    sections: [
      {
        heading: 'brands and ranges',
        body: 'we stock recognisable lighting brands and related electricals. ask in-store for the exact led models available on the day.',
      },
      {
        heading: 'next step',
        body: cta,
      },
    ],
    keywords: ['led lights accra', 'buy led accra', 'led bulbs ghana'],
  },
  {
    slug: 'cables-wiring-ghana',
    title: 'cables and wiring ghana | moderndreamlight',
    description:
      'cables and wiring in ghana from moderndreamlight. shops nationwide. main branch opera square accra with 2 shops. call or whatsapp for help.',
    h1: 'cables and wiring in ghana',
    intro:
      'need cables and wiring for a house or project? moderndreamlight stocks cables and electrical wiring supplies at shops nationwide, with the main branch at opera square.',
    sections: [
      {
        heading: 'for trade and homes',
        body: 'electricians, contractors and homeowners can visit for wiring needs alongside switches, boards and lighting.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['cables ghana', 'electrical cable accra', 'wiring shop ghana'],
  },
  {
    slug: 'switches-sockets-accra',
    title: 'switches and sockets accra | moderndreamlight',
    description:
      'switches and sockets in accra from moderndreamlight. shops nationwide. main branch opera square with 2 shops.',
    h1: 'switches and sockets in accra',
    intro:
      'looking for switches and sockets in accra? moderndreamlight stocks wiring accessories from trusted brands. visit shops nationwide or the opera square main branch.',
    sections: [
      {
        heading: 'in-store selection',
        body: 'availability changes with stock. staff can help you match switches, sockets and related fittings.',
      },
      {
        heading: 'visit us',
        body: cta,
      },
    ],
    keywords: ['switches accra', 'sockets ghana', 'wiring accessories accra'],
  },
  {
    slug: 'ceiling-fans-accra',
    title: 'ceiling fans accra | moderndreamlight',
    description:
      'ceiling fans and ventilation in accra from moderndreamlight. shops nationwide. main branch opera square with 2 shops.',
    h1: 'ceiling fans in accra',
    intro:
      'searching for ceiling fans in accra? moderndreamlight stocks fans and ventilation alongside lighting and electricals.',
    sections: [
      {
        heading: 'what we offer',
        body: 'ceiling, wall and related fans for homes and workplaces. ask in-store for brands and sizes available.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['ceiling fans accra', 'fans ghana', 'buy fan accra'],
  },
  {
    slug: 'circuit-breakers-ghana',
    title: 'circuit breakers ghana | moderndreamlight',
    description:
      'circuit protection and breakers in ghana from moderndreamlight. shops nationwide. main branch opera square accra.',
    h1: 'circuit breakers and protection in ghana',
    intro:
      'for circuit protection, changeovers and breakers, visit moderndreamlight. shops nationwide with main branch at opera square.',
    sections: [
      {
        heading: 'trade and project buyers',
        body: 'contractors and electricians can check stock in person and get help choosing protection equipment.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['circuit breaker ghana', 'changeover switch accra', 'mcb ghana'],
  },
  {
    slug: 'distribution-boards-accra',
    title: 'distribution boards accra | moderndreamlight',
    description:
      'distribution boards in accra from moderndreamlight. spn and tpn boards and related electricals. shops nationwide.',
    h1: 'distribution boards in accra',
    intro:
      'need a distribution board in accra? moderndreamlight stocks boards and related circuit protection. main branch opera square with 2 shops.',
    sections: [
      {
        heading: 'ask in-store',
        body: 'board ways and brands vary by stock. visit or whatsapp for current options.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['distribution board accra', 'consumer unit ghana', 'db board accra'],
  },
  {
    slug: 'electrical-conduit-ghana',
    title: 'electrical conduit ghana | moderndreamlight',
    description:
      'conduit and trunking in ghana from moderndreamlight. shops nationwide. main branch opera square accra.',
    h1: 'electrical conduit and trunking in ghana',
    intro:
      'looking for conduit and trunking? moderndreamlight stocks conduit systems and related electrical accessories.',
    sections: [
      {
        heading: 'for neat installs',
        body: 'useful for residential and commercial wiring jobs. confirm sizes and types at the shop.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['conduit ghana', 'electrical pipe accra', 'trunking ghana'],
  },
  {
    slug: 'wholesale-electrical-accra',
    title: 'wholesale electrical accra | moderndreamlight',
    description:
      'electrical and lighting for trade in accra. moderndreamlight — shops nationwide, main branch opera square with 2 shops.',
    h1: 'wholesale and trade electrical in accra',
    intro:
      'electricians and project buyers shop with moderndreamlight for lighting and electrical supplies. competitive prices in-store.',
    sections: [
      {
        heading: 'how to buy',
        body: 'this site is publicity only — no online checkout. visit a shop or message whatsapp for help.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['wholesale electrical accra', 'electrical wholesaler ghana', 'trade electrical accra'],
  },
  {
    slug: 'where-to-buy-lights-accra',
    title: 'where to buy lights in accra | moderndreamlight',
    description:
      'where to buy lights in accra: moderndreamlight shops nationwide. main branch at opera square with 2 shops. since 2000.',
    h1: 'where to buy lights in accra',
    intro:
      'if you are asking where to buy lights in accra, moderndreamlight has shops nationwide and a main branch at opera square with 2 shops.',
    sections: [
      {
        heading: 'what to expect',
        body: 'quality lighting and electricals at competitive prices, with staff who know the stock.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['where to buy lights accra', 'best lighting shop accra', 'lights near me accra'],
  },
  {
    slug: 'electrician-supplies-accra',
    title: 'electrician supplies accra | moderndreamlight',
    description:
      'electrician supplies in accra — cables, boards, switches, lighting and tools. moderndreamlight shops nationwide.',
    h1: 'electrician supplies in accra',
    intro:
      'electricians in accra can get supplies from moderndreamlight: wiring, protection, lighting and accessories.',
    sections: [
      {
        heading: 'for the job site',
        body: 'visit in person to pick what you need the same day. ask about brands like philips, havells, hager and more.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['electrician supplies accra', 'electrical materials accra', 'wiring materials ghana'],
  },
  {
    slug: 'floodlights-accra',
    title: 'floodlights accra | moderndreamlight',
    description:
      'floodlights and outdoor lighting in accra from moderndreamlight. shops nationwide. main branch opera square.',
    h1: 'floodlights in accra',
    intro:
      'searching for floodlights in accra? moderndreamlight stocks lighting for outdoor and commercial use. check availability in-store.',
    sections: [
      {
        heading: 'outdoor and site lighting',
        body: 'ask staff for wattages and brands currently available. pricing is confirmed at the shop.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['floodlights accra', 'outdoor lights ghana', 'security lights accra'],
  },
  {
    slug: 'philips-lights-ghana',
    title: 'philips lights ghana | stocked at moderndreamlight',
    description:
      'looking for philips lighting in ghana? moderndreamlight stocks philips and other brands. shops nationwide. opera square main branch.',
    h1: 'philips lights in ghana',
    intro:
      'many customers search for philips lights in ghana. moderndreamlight stocks philips lighting lines when available, alongside other trusted brands.',
    sections: [
      {
        heading: 'availability',
        body: 'stock changes. visit or whatsapp to confirm the philips items you need before you travel.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['philips lights ghana', 'philips led accra', 'philips lighting shop'],
  },
  {
    slug: 'havells-ghana',
    title: 'havells ghana | stocked at moderndreamlight',
    description:
      'havells products in ghana — check stock at moderndreamlight. shops nationwide. main branch opera square accra.',
    h1: 'havells in ghana',
    intro:
      'searching for havells in ghana? moderndreamlight stocks havells and other electrical brands when available.',
    sections: [
      {
        heading: 'ask before you visit',
        body: 'message whatsapp or call to confirm the havells item you need.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['havells ghana', 'havells accra', 'havells electrical shop'],
  },
  {
    slug: 'visit-opera-square',
    title: 'visit opera square branch | moderndreamlight',
    description:
      'visit moderndreamlight main branch at opera square, accra — 2 shops at the branch. hours monday–saturday 8:00am–4:30pm.',
    h1: 'visit the opera square main branch',
    intro:
      'our main branch is at opera square, accra central. there are 2 shops at this branch. we also have shops nationwide across ghana.',
    sections: [
      {
        heading: 'opening hours',
        body: `${site.hours}. ${site.hoursNote}.`,
      },
      {
        heading: 'how to reach us',
        body: cta,
      },
    ],
    keywords: ['opera square shop', 'moderndreamlight location', 'electrical shop opera square'],
  },
  {
    slug: 'shops-nationwide-ghana',
    title: 'shops nationwide ghana | moderndreamlight',
    description:
      'moderndreamlight has shops nationwide across ghana. main branch at opera square, accra with 2 shops. lighting and electricals since 2000.',
    h1: 'shops nationwide across ghana',
    intro:
      'moderndreamlight serves customers across ghana with shops nationwide. the main branch is at opera square in accra, and that branch has 2 shops.',
    sections: [
      {
        heading: 'find the right shop',
        body: 'call or whatsapp and we will guide you to the best location for what you need.',
      },
      {
        heading: 'contact',
        body: cta,
      },
    ],
    keywords: ['electrical shops ghana', 'lighting shops ghana', 'nationwide electrical'],
  },
  {
    slug: 'outdoor-lighting-ghana',
    title: 'outdoor lighting ghana | moderndreamlight',
    description:
      'outdoor lighting in ghana from moderndreamlight — floodlights, garden and security lighting. shops nationwide. opera square main branch.',
    h1: 'outdoor lighting in ghana',
    intro:
      'looking for outdoor lighting in ghana? moderndreamlight stocks outdoor and site lighting alongside indoor fittings and electricals.',
    sections: [
      {
        heading: 'what to ask for',
        body: 'floodlights, wall washers, garden fittings and security lights — confirm wattage and stock in-store.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['outdoor lighting ghana', 'garden lights accra', 'security lights ghana'],
  },
  {
    slug: 'indoor-lighting-accra',
    title: 'indoor lighting accra | moderndreamlight',
    description:
      'indoor lighting in accra from moderndreamlight. ceiling, wall and decorative lights. shops nationwide. main branch opera square.',
    h1: 'indoor lighting in accra',
    intro:
      'searching for indoor lighting in accra? visit moderndreamlight for lamps, fittings and related electricals at shops nationwide.',
    sections: [
      {
        heading: 'homes and offices',
        body: 'staff can help you match fittings for living rooms, bedrooms, shops and offices. pricing is confirmed at the shop.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['indoor lighting accra', 'ceiling lights ghana', 'home lights accra'],
  },
  {
    slug: 'lamps-bulbs-ghana',
    title: 'lamps and bulbs ghana | moderndreamlight',
    description:
      'lamps and bulbs in ghana from moderndreamlight. led and lighting lamps. shops nationwide. main branch opera square accra.',
    h1: 'lamps and bulbs in ghana',
    intro:
      'need lamps or bulbs in ghana? moderndreamlight stocks lighting lamps and related fittings. ask for led options currently available.',
    sections: [
      {
        heading: 'stock changes daily',
        body: 'whatsapp or visit to confirm the bulb type and brand you need before you travel.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['bulbs ghana', 'led bulbs accra', 'lamps shop ghana'],
  },
  {
    slug: 'changeover-switch-ghana',
    title: 'changeover switch ghana | moderndreamlight',
    description:
      'changeover switches in ghana from moderndreamlight. circuit protection and boards too. shops nationwide. opera square main branch.',
    h1: 'changeover switches in ghana',
    intro:
      'looking for a changeover switch in ghana? moderndreamlight stocks changeovers and related circuit protection for homes and projects.',
    sections: [
      {
        heading: 'trade buyers welcome',
        body: 'electricians and contractors can check ratings and brands in person at any of our shops nationwide.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['changeover switch ghana', 'changeover accra', 'ats switch ghana'],
  },
  {
    slug: 'wall-lights-accra',
    title: 'wall lights accra | moderndreamlight',
    description:
      'wall lights in accra from moderndreamlight. decorative and functional wall fittings. shops nationwide. opera square main branch.',
    h1: 'wall lights in accra',
    intro:
      'searching for wall lights in accra? moderndreamlight stocks wall fittings and related indoor lighting.',
    sections: [
      {
        heading: 'see them in person',
        body: 'finish and size options are easiest to judge at the shop. staff can suggest matching lamps.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['wall lights accra', 'wall lamps ghana', 'decorative wall lights'],
  },
  {
    slug: 'chandelier-accra',
    title: 'chandelier accra | moderndreamlight',
    description:
      'chandeliers and decorative lights in accra from moderndreamlight. shops nationwide. main branch opera square with 2 shops.',
    h1: 'chandeliers in accra',
    intro:
      'looking for a chandelier in accra? moderndreamlight stocks decorative lighting when available. visit shops nationwide or the opera square main branch.',
    sections: [
      {
        heading: 'availability',
        body: 'decorative stock rotates. message whatsapp with photos or sizes you need and we will guide you.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['chandelier accra', 'decorative lights ghana', 'crystal lights accra'],
  },
  {
    slug: 'commercial-lighting-ghana',
    title: 'commercial lighting ghana | moderndreamlight',
    description:
      'commercial and project lighting in ghana from moderndreamlight. shops nationwide. main branch opera square accra.',
    h1: 'commercial lighting in ghana',
    intro:
      'for shops, offices and project sites, moderndreamlight supplies commercial lighting and electricals across ghana.',
    sections: [
      {
        heading: 'project support',
        body: 'bring your list or drawings to the shop. we help match lighting, boards, cables and accessories.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['commercial lighting ghana', 'office lights accra', 'project lighting ghana'],
  },
  {
    slug: 'buy-electricals-accra',
    title: 'buy electricals accra | moderndreamlight',
    description:
      'buy electricals in accra at moderndreamlight. shops nationwide. main branch opera square with 2 shops. since 2000.',
    h1: 'buy electricals in accra',
    intro:
      'want to buy electricals in accra? moderndreamlight is a lighting and electrical house with shops nationwide and a main branch at opera square.',
    sections: [
      {
        heading: 'in-store only pricing',
        body: 'this website is for publicity. visit a shop or whatsapp for current prices and stock.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['buy electricals accra', 'electrical shop near me accra', 'electrical materials shop'],
  },
  {
    slug: 'accra-central-electrical',
    title: 'accra central electrical shop | moderndreamlight',
    description:
      'electrical shop in accra central — moderndreamlight main branch at opera square with 2 shops. lighting and electricals.',
    h1: 'electrical shop in accra central',
    intro:
      'if you are around accra central or opera square, visit moderndreamlight’s main branch — 2 shops at the branch — for lighting and electricals.',
    sections: [
      {
        heading: 'hours',
        body: `${site.hours}. ${site.hoursNote}.`,
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['accra central electrical', 'electrical shop accra central', 'opera square electrical shop'],
  },
  {
    slug: 'schneider-electric-ghana',
    title: 'schneider electric ghana | stocked at moderndreamlight',
    description:
      'looking for schneider electric in ghana? check stock at moderndreamlight. shops nationwide. main branch opera square.',
    h1: 'schneider electric in ghana',
    intro:
      'many buyers search for schneider electric in ghana. moderndreamlight stocks schneider and other protection brands when available.',
    sections: [
      {
        heading: 'confirm stock',
        body: 'call or whatsapp before you visit so we can check the exact schneider item you need.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['schneider ghana', 'schneider electric accra', 'schneider breaker ghana'],
  },
  {
    slug: 'hager-ghana',
    title: 'hager ghana | stocked at moderndreamlight',
    description:
      'hager electrical products in ghana — check availability at moderndreamlight. shops nationwide. opera square main branch.',
    h1: 'hager in ghana',
    intro:
      'searching for hager in ghana? moderndreamlight stocks hager and related circuit protection lines when available.',
    sections: [
      {
        heading: 'ask in-store',
        body: 'mcbs, rcds and boards vary by stock. staff can help match what you need.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['hager ghana', 'hager mcb accra', 'hager electrical shop'],
  },
  {
    slug: 'legrand-ghana',
    title: 'legrand ghana | stocked at moderndreamlight',
    description:
      'legrand switches and electricals in ghana from moderndreamlight when in stock. shops nationwide. opera square main branch.',
    h1: 'legrand in ghana',
    intro:
      'looking for legrand in ghana? moderndreamlight stocks legrand wiring accessories and related lines when available.',
    sections: [
      {
        heading: 'availability',
        body: 'message whatsapp with the model or photo and we will confirm before you travel.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['legrand ghana', 'legrand switches accra', 'legrand socket ghana'],
  },
  {
    slug: 'osram-lights-ghana',
    title: 'osram lights ghana | stocked at moderndreamlight',
    description:
      'osram lighting in ghana — check stock at moderndreamlight. shops nationwide. main branch opera square accra.',
    h1: 'osram lights in ghana',
    intro:
      'customers often search for osram lights in ghana. moderndreamlight stocks osram and other lighting brands when available.',
    sections: [
      {
        heading: 'visit or message',
        body: 'confirm the lamp type you need — stock moves quickly on popular bulbs.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['osram ghana', 'osram led accra', 'osram bulbs ghana'],
  },
  {
    slug: 'abb-electrical-ghana',
    title: 'abb electrical ghana | stocked at moderndreamlight',
    description:
      'abb electrical products in ghana — check at moderndreamlight. shops nationwide. main branch opera square.',
    h1: 'abb electrical in ghana',
    intro:
      'looking for abb in ghana? moderndreamlight stocks abb and other protection brands when available.',
    sections: [
      {
        heading: 'for trade',
        body: 'bring your parts list. we will check what is on the shelf that day.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['abb ghana', 'abb breaker accra', 'abb electrical shop'],
  },
  {
    slug: 'panasonic-lights-ghana',
    title: 'panasonic lights ghana | stocked at moderndreamlight',
    description:
      'panasonic lighting and electricals in ghana from moderndreamlight when in stock. shops nationwide. opera square.',
    h1: 'panasonic lights in ghana',
    intro:
      'searching for panasonic lights in ghana? moderndreamlight stocks panasonic and other brands when available.',
    sections: [
      {
        heading: 'confirm first',
        body: 'whatsapp the item you need and we will check current stock.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['panasonic lights ghana', 'panasonic electrical accra', 'panasonic fan ghana'],
  },
  {
    slug: 'electrical-tools-accra',
    title: 'electrical tools accra | moderndreamlight',
    description:
      'electrical tools and accessories in accra from moderndreamlight. shops nationwide. main branch opera square.',
    h1: 'electrical tools in accra',
    intro:
      'need electrical tools or accessories in accra? moderndreamlight stocks tools alongside lighting, cables and boards.',
    sections: [
      {
        heading: 'for electricians',
        body: 'pick up materials and related accessories in one visit. ask staff what is currently available.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['electrical tools accra', 'wiring tools ghana', 'electrician tools accra'],
  },
  {
    slug: 'street-lights-ghana',
    title: 'street lights ghana | moderndreamlight',
    description:
      'street and outdoor lighting in ghana from moderndreamlight. shops nationwide. main branch opera square accra.',
    h1: 'street and outdoor lights in ghana',
    intro:
      'for street-style and outdoor project lighting, ask moderndreamlight about current outdoor stock at shops nationwide.',
    sections: [
      {
        heading: 'project buyers',
        body: 'bring wattage and quantity requirements. we confirm availability and pricing in-store.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['street lights ghana', 'road lights accra', 'outdoor project lighting'],
  },
  {
    slug: 'downlights-accra',
    title: 'downlights accra | moderndreamlight',
    description:
      'downlights and recessed lighting in accra from moderndreamlight. shops nationwide. opera square main branch.',
    h1: 'downlights in accra',
    intro:
      'looking for downlights in accra? moderndreamlight stocks recessed and related indoor lighting when available.',
    sections: [
      {
        heading: 'see samples',
        body: 'cut sizes and finishes are easier to choose in person. staff can advise on matching lamps.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['downlights accra', 'recessed lights ghana', 'spot lights accra'],
  },
  {
    slug: 'voltage-protector-ghana',
    title: 'voltage protector ghana | moderndreamlight',
    description:
      'voltage protectors and surge protection in ghana from moderndreamlight when in stock. shops nationwide.',
    h1: 'voltage protectors in ghana',
    intro:
      'searching for a voltage protector in ghana? moderndreamlight stocks protection devices and related electricals when available.',
    sections: [
      {
        heading: 'brands vary',
        body: 'ask for sollatek and other protection options currently on the shelf.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['voltage protector ghana', 'surge protector accra', 'avr ghana'],
  },
  {
    slug: 'lighting-shop-near-me-accra',
    title: 'lighting shop near me accra | moderndreamlight',
    description:
      'lighting shop near you in accra? moderndreamlight has shops nationwide. main branch opera square with 2 shops.',
    h1: 'lighting shop near you in accra',
    intro:
      'if you searched “lighting shop near me” in accra, moderndreamlight is a nationwide lighting and electrical house. start with the opera square main branch (2 shops) or call for the closest shop.',
    sections: [
      {
        heading: 'get directions',
        body: `${site.address.mainBranch}. ${site.address.shopsAtMain}. ${cta}`,
      },
    ],
    keywords: ['lighting shop near me accra', 'lights near me ghana', 'electrical shop near me accra'],
  },
  {
    slug: 'quality-lights-ghana',
    title: 'quality lights ghana | moderndreamlight',
    description:
      'quality lights in ghana from moderndreamlight since 2000. trusted brands. shops nationwide. opera square main branch.',
    h1: 'quality lights in ghana',
    intro:
      'moderndreamlight has supplied quality lighting and electricals in ghana since 2000. competitive prices. come and see for yourself.',
    sections: [
      {
        heading: 'brands people trust',
        body: 'philips, havells, osram, schneider, hager, legrand and more — availability depends on stock.',
      },
      { heading: 'contact', body: cta },
    ],
    keywords: ['quality lights ghana', 'best lights accra', 'trusted lighting shop ghana'],
  },
];

export function getSeoPage(slug: string) {
  return seoPages.find((page) => page.slug === slug);
}
