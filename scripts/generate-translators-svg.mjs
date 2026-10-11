// Generates .github/images/translators.svg from .all-contributorsrc.
// Material 3 style cards (rounded container, circular avatar, name + role),
// stacked vertically. Light/dark mode handled inside the SVG.
// Usage: node scripts/generate-translators-svg.mjs
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";

const rc = JSON.parse(readFileSync(".all-contributorsrc", "utf8"));
const OUT = ".github/images/translators.svg";

const W = 520, CARD_H = 88, GAP = 12, AV = 56, RADIUS = 28;
const LABELS = { code: "Code", translation: "Translation", doc: "Documentation", bug: "Bug reports", ideas: "Ideas" };

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

async function avatarDataUri(c) {
  try {
    const res = await fetch(`https://github.com/${encodeURIComponent(c.login)}.png?size=128`, { redirect: "follow" });
    if (!res.ok) throw new Error(res.status);
    const type = res.headers.get("content-type") || "image/png";
    const buf = Buffer.from(await res.arrayBuffer());
    return `data:${type};base64,${buf.toString("base64")}`;
  } catch (e) {
    console.warn(`avatar for ${c.login} failed: ${e.message}`);
    return null;
  }
}

const people = rc.contributors ?? [];
const H = Math.max(1, people.length) * CARD_H + Math.max(0, people.length - 1) * GAP;

let cards = "";
for (const [i, c] of people.entries()) {
  const y = i * (CARD_H + GAP);
  const name = esc(c.name || c.login);
  const sub = esc(c.role || (c.contributions || []).map((t) => LABELS[t] || t).join(" · "));
  const uri = await avatarDataUri(c);
  const cx = 16 + AV / 2, cy = y + CARD_H / 2;
  const avatar = uri
    ? `<clipPath id="a${i}"><circle cx="${cx}" cy="${cy}" r="${AV / 2}"/></clipPath>
       <image href="${uri}" x="16" y="${cy - AV / 2}" width="${AV}" height="${AV}" clip-path="url(#a${i})" preserveAspectRatio="xMidYMid slice"/>`
    : `<circle cx="${cx}" cy="${cy}" r="${AV / 2}" class="ph"/>
       <text x="${cx}" y="${cy + 8}" text-anchor="middle" class="ini">${esc((c.login || "?")[0].toUpperCase())}</text>`;
  cards += `
  <a href="${esc(c.profile || `https://github.com/${c.login}`)}" target="_blank">
    <rect x="0" y="${y}" width="${W}" height="${CARD_H}" rx="${RADIUS}" class="card"/>
    ${avatar}
    <text x="${16 + AV + 16}" y="${cy - 4}" class="name">${name}</text>
    <text x="${16 + AV + 16}" y="${cy + 18}" class="sub">${sub}</text>
  </a>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Translators and contributors">
  <style>
    .card{fill:#F3EDF7;stroke:#CAC4D0;stroke-width:1}
    .ph{fill:#EADDFF}
    .ini{fill:#21005D;font:600 24px system-ui,sans-serif}
    .name{fill:#1D1B20;font:600 17px system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
    .sub{fill:#49454F;font:400 14px system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
    @media (prefers-color-scheme: dark){
      .card{fill:#2B2930;stroke:#49454F}
      .ph{fill:#4F378B}
      .ini{fill:#EADDFF}
      .name{fill:#E6E0E9}
      .sub{fill:#CAC4D0}
    }
  </style>${cards}
</svg>
`;
mkdirSync(".github/images", { recursive: true });
writeFileSync(OUT, svg);
console.log(`Wrote ${OUT} (${people.length} cards)`);
