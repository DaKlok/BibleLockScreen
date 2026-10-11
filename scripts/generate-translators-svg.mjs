// Generates Material 3 style "translator cards" from .all-contributorsrc.
//
// Output (PNG, because GitHub serves repo SVGs with a CSP that blocks the
// embedded avatar images):
//   .github/images/translators-light.png
//   .github/images/translators-dark.png
// The README picks the right one with <picture> + prefers-color-scheme.
//
// Usage:  npm i --no-save @resvg/resvg-js && node scripts/generate-translators-svg.mjs
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { Resvg } from "@resvg/resvg-js";

const rc = JSON.parse(readFileSync(".all-contributorsrc", "utf8"));
const OUT_DIR = ".github/images";

const W = 520, CARD_H = 88, GAP = 12, AV = 56, RADIUS = 28;
const LABELS = { code: "Code", translation: "Translation", doc: "Documentation", bug: "Bug reports", ideas: "Ideas" };
const FONT = "Roboto, 'Noto Sans', 'DejaVu Sans', sans-serif";

const THEMES = {
  light: { card: "#F3EDF7", stroke: "#CAC4D0", ph: "#EADDFF", ini: "#21005D", name: "#1D1B20", sub: "#49454F" },
  dark:  { card: "#2B2930", stroke: "#49454F", ph: "#4F378B", ini: "#EADDFF", name: "#E6E0E9", sub: "#CAC4D0" },
};

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

async function avatarDataUri(c) {
  try {
    const res = await fetch(`https://github.com/${encodeURIComponent(c.login)}.png?size=128`, { redirect: "follow" });
    if (!res.ok) throw new Error(res.status);
    const type = res.headers.get("content-type") || "image/png";
    return `data:${type};base64,${Buffer.from(await res.arrayBuffer()).toString("base64")}`;
  } catch (e) {
    console.warn(`avatar for ${c.login} failed: ${e.message}`);
    return null;
  }
}

const people = rc.contributors ?? [];
const avatars = await Promise.all(people.map(avatarDataUri));
const H = Math.max(1, people.length) * CARD_H + Math.max(0, people.length - 1) * GAP;

function buildSvg(t) {
  let cards = "";
  people.forEach((c, i) => {
    const y = i * (CARD_H + GAP);
    const name = esc(c.name || c.login);
    const sub = esc(c.role || (c.contributions || []).map((x) => LABELS[x] || x).join(" · "));
    const cx = 16 + AV / 2, cy = y + CARD_H / 2;
    const avatar = avatars[i]
      ? `<clipPath id="a${i}"><circle cx="${cx}" cy="${cy}" r="${AV / 2}"/></clipPath>
    <image href="${avatars[i]}" x="16" y="${cy - AV / 2}" width="${AV}" height="${AV}" clip-path="url(#a${i})" preserveAspectRatio="xMidYMid slice"/>`
      : `<circle cx="${cx}" cy="${cy}" r="${AV / 2}" fill="${t.ph}"/>
    <text x="${cx}" y="${cy + 8}" text-anchor="middle" fill="${t.ini}" font-family="${FONT}" font-size="24" font-weight="600">${esc((c.login || "?")[0].toUpperCase())}</text>`;
    cards += `
  <rect x="0.5" y="${y + 0.5}" width="${W - 1}" height="${CARD_H - 1}" rx="${RADIUS}" fill="${t.card}" stroke="${t.stroke}"/>
  ${avatar}
  <text x="${16 + AV + 16}" y="${cy - 4}" fill="${t.name}" font-family="${FONT}" font-size="17" font-weight="600">${name}</text>
  <text x="${16 + AV + 16}" y="${cy + 18}" fill="${t.sub}" font-family="${FONT}" font-size="14">${sub}</text>`;
  });
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${cards}
</svg>`;
}

mkdirSync(OUT_DIR, { recursive: true });
for (const [mode, theme] of Object.entries(THEMES)) {
  const png = new Resvg(buildSvg(theme), {
    fitTo: { mode: "zoom", value: 2 },
    font: { loadSystemFonts: true, defaultFontFamily: "Roboto" },
  }).render().asPng();
  writeFileSync(`${OUT_DIR}/translators-${mode}.png`, png);
  console.log(`Wrote ${OUT_DIR}/translators-${mode}.png (${people.length} cards)`);
}
