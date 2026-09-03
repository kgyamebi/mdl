/** Stylized italic M mark — shared between logo, icon, favicon, and print. */
export const MDL_MARK_PATH =
  'M2 36V3h6.8l7.2 21.5L23.2 3H30v33h-5.8V15.8L16.8 34h-1.6L7.8 15.8V36H2z';

export const MDL_MARK_VIEWBOX = '0 0 32 38';

export function mdlMarkSvg(color: string, skew = true): string {
  const transform = skew ? 'transform="skewX(-11)"' : '';
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${MDL_MARK_VIEWBOX}" width="28" height="34" fill="${color}" aria-hidden="true"><path d="${MDL_MARK_PATH}" ${transform}/></svg>`;
}

export function mdlBrandHtml(color = '#111'): string {
  return `<div style="display:flex;align-items:center;justify-content:center;gap:7px;color:${color};font-family:'Segoe UI',system-ui,sans-serif;font-weight:700;font-size:17px;line-height:1;letter-spacing:-0.03em;margin:0 auto 10px">
  ${mdlMarkSvg(color)}
  <span style="text-transform:lowercase">modern</span>
  <span style="display:inline-flex;align-items:center;justify-content:center;border:1.5px solid ${color};border-radius:5px;padding:3px 7px;font-size:11px;letter-spacing:0.1em">DL</span>
</div>`;
}
