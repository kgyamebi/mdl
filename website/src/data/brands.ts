export type Brand = {
  name: string;
  slug: string;
  accent?: string;
  keepCase?: boolean;
};

/** Safe-to-list brands from the live MDL catalog, plus house brand modernDL. */
export const brands: Brand[] = [
  { name: 'modernDL', slug: 'moderndl', accent: '#9A7428', keepCase: true },
  { name: 'Philips', slug: 'philips', accent: '#0A5C9C' },
  { name: 'V-TAC', slug: 'v-tac', accent: '#E31C23' },
  { name: 'V-MAX', slug: 'v-max', accent: '#1A1A1A' },
  { name: 'Havells', slug: 'havells', accent: '#ED1C24' },
  { name: 'Schneider Electric', slug: 'schneider-electric', accent: '#3DCD58' },
  { name: 'Osram', slug: 'osram', accent: '#FFCC00' },
  { name: 'Hager', slug: 'hager', accent: '#009BDE' },
  { name: 'Orient', slug: 'orient', accent: '#C41230' },
  { name: 'Rexton', slug: 'rexton', accent: '#1B4F72' },
  { name: 'HPL', slug: 'hpl', accent: '#003366' },
  { name: 'Clipsal', slug: 'clipsal', accent: '#E30613' },
  { name: 'ABB', slug: 'abb', accent: '#FF000F' },
  { name: 'Panasonic', slug: 'panasonic', accent: '#0049A5' },
  { name: 'Crompton', slug: 'crompton', accent: '#0057A8' },
  { name: 'Bajaj', slug: 'bajaj', accent: '#ED1C24' },
  { name: 'Legrand', slug: 'legrand', accent: '#C8102E' },
  { name: 'MK', slug: 'mk', accent: '#E30613' },
  { name: 'Chint', slug: 'chint', accent: '#E60012' },
  { name: 'FSL', slug: 'fsl', accent: '#F36C00' },
  { name: 'Winluk', slug: 'winluk', accent: '#0D47A1' },
  { name: 'Zenith', slug: 'zenith', accent: '#111111' },
  { name: 'Emkay', slug: 'emkay', accent: '#2E7D32' },
  { name: 'Modi', slug: 'modi', accent: '#6A1B9A' },
  { name: 'Seera', slug: 'seera', accent: '#00695C' },
  { name: 'Sollatek', slug: 'sollatek', accent: '#009640' },
  { name: 'Tungsram', slug: 'tungsram', accent: '#E30613' },
];
