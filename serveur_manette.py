import sys, os, io, base64, asyncio, json, socket, threading, http.server, secrets, time
from urllib.parse import urlparse
import websockets, vgamepad as vg

PAGE = r"""<!doctype html>
<html lang="fr"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Manette</title>
<style>
:root{--bg:#0f1218;--card:#181c25;--btn:#2b3140;--line:#4a5266;--on:#35c2ff;--txt:#e8ecf4;--mut:#8b94a8}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--txt);font-family:system-ui,sans-serif;padding:14px;
  display:flex;flex-direction:column;align-items:center}
.ecran{display:flex;flex-direction:column;align-items:center;gap:12px;width:100%}
.col{display:flex;flex-direction:column;gap:12px}
.barre{display:flex;gap:12px;align-items:center;flex-wrap:wrap;background:var(--card);
  padding:8px 12px;border-radius:12px;width:100%;max-width:900px;font-size:14px}
.carte{background:var(--card);border-radius:16px;padding:22px;width:100%;max-width:420px;
  display:flex;flex-direction:column;gap:14px;margin-top:6vh}
h1{margin:0;font-size:24px}
.ligne{display:flex;gap:10px;align-items:center}
.pt{width:10px;height:10px;border-radius:50%;background:#e5484d;flex:none}
.pt.ok{background:#30c48d}
.mut{color:var(--mut);font-size:14px}
svg{width:100%;max-width:900px;max-height:calc(100vh - 130px);max-height:calc(100dvh - 130px);
  background:var(--card);border-radius:16px}
svg.edit{touch-action:none;-webkit-user-select:none;user-select:none;-webkit-touch-callout:none;
  outline:2px solid var(--on)}
.corps{fill:#222735;stroke:var(--line);stroke-width:2}
.b{fill:var(--btn);stroke:var(--line);stroke-width:2}
.b.on{fill:var(--on);stroke:#fff}
.fond{fill:#12151d;stroke:var(--line);stroke-width:2}
.rempli{fill:var(--on)}
.sym{fill:var(--txt);font:bold 15px sans-serif;text-anchor:middle;dominant-baseline:central;pointer-events:none}
.leg{fill:var(--mut);font:12px sans-serif;text-anchor:middle;pointer-events:none}
#actifs{font-weight:600;color:var(--on);min-height:1.3em;text-align:center}
.lig{display:flex;gap:10px;align-items:center;flex-wrap:wrap}
.lig b{width:28px}
button,select,.btn{background:var(--btn);color:var(--txt);border:1px solid var(--line);border-radius:8px;
  padding:6px 10px;font-size:14px}
.btn{display:inline-block;padding:10px 14px}
button.att{background:var(--on);color:#000}
.grand{width:100%;padding:14px;font-size:16px;border-radius:12px}
.icone{font-size:20px;padding:6px 14px}
input{background:#12151d;color:var(--txt);border:1px solid var(--line);border-radius:10px;padding:14px;
  font-size:28px;letter-spacing:.5em;text-align:center;width:100%}
details{background:#12151d;border:1px solid var(--line);border-radius:10px;padding:10px}
summary{font-weight:600;cursor:pointer}
details>*:not(summary){margin-top:10px}
.section{background:#12151d;border:1px solid var(--line);border-radius:10px;padding:10px;
  display:flex;flex-direction:column;gap:8px}
#tiroir{position:fixed;top:0;left:0;bottom:0;width:min(340px,88vw);background:var(--card);
  border-right:1px solid var(--line);transform:translateX(-102%);transition:transform .25s;
  z-index:20;padding:14px;overflow-y:auto;display:flex;flex-direction:column;gap:12px}
#tiroir.ouvert{transform:none}
#voile{position:fixed;inset:0;background:rgba(0,0,0,.55);opacity:0;pointer-events:none;
  transition:opacity .25s;z-index:10}
#voile.ouvert{opacity:1;pointer-events:auto}
.tete{display:flex;justify-content:space-between;align-items:center}
#videoVeille{position:fixed;right:8px;bottom:8px;width:72px;height:44px;border-radius:6px;
  background:#000;z-index:5;display:none}
</style></head><body>

<!-- ============ ECRAN MENU ============ -->
<div class="ecran" id="ecranMenu">
  <div class="carte">
    <h1>🎮 Manette</h1>
    <div class="ligne"><span class="pt" id="ptM"></span><span id="etatM">Connexion au PC...</span></div>

    <div class="col" id="blocCode">
      <div class="mut">Entre le code à 4 chiffres affiché sur le PC.</div>
      <input id="champCode" inputmode="numeric" maxlength="4" placeholder="0000" autocomplete="off">
      <button id="btnCode" class="grand att">Se connecter</button>
      <div class="mut" id="msgCode"></div>
    </div>

    <div class="col" id="blocOk" style="display:none">
      <button id="btnJouer" class="grand att">🎮 Ouvrir la manette</button>
      <button id="btnOubli" class="grand">Changer de code</button>
    </div>

    <div class="mut" id="codePC"></div>
  </div>
</div>

<!-- ============ ECRAN MANETTE ============ -->
<div class="ecran" id="ecranJeu" style="display:none">
  <div class="barre">
    <button id="btnMenu" class="icone">☰</button>
    <span class="pt" id="pt"></span><span id="etat">Connexion au PC...</span>
    <span class="mut" id="lat"></span>
    <span class="mut" id="codeBadge"></span>
    <button id="btnEdit" style="margin-left:auto">Edit : OFF</button>
  </div>
  <div id="actifs">Aucun bouton</div>

<svg viewBox="0 0 600 330">
  <path class="corps" d="M140 70 Q300 40 460 70 Q520 82 548 190 Q570 300 525 318 Q485 332 458 280 Q438 245 405 245 L195 245 Q162 245 142 280 Q115 332 75 318 Q30 300 52 190 Q80 82 140 70 Z"/>

  <rect id="l2z" class="fond" x="110" y="14" width="90" height="22" rx="8"/>
  <rect id="l2f" class="rempli" x="110" y="14" width="0" height="22" rx="8"/>
  <text class="sym" x="155" y="25">L2</text>
  <rect id="r2z" class="fond" x="400" y="14" width="90" height="22" rx="8"/>
  <rect id="r2f" class="rempli" x="400" y="14" width="0" height="22" rx="8"/>
  <text class="sym" x="445" y="25">R2</text>

  <rect id="l1" class="b" x="110" y="42" width="90" height="18" rx="8"/><text class="sym" x="155" y="51">L1</text>
  <rect id="r1" class="b" x="400" y="42" width="90" height="18" rx="8"/><text class="sym" x="445" y="51">R1</text>

  <rect id="up" class="b" x="140" y="98" width="20" height="28" rx="4"/>
  <rect id="down" class="b" x="140" y="134" width="20" height="28" rx="4"/>
  <rect id="left" class="b" x="112" y="120" width="28" height="20" rx="4"/>
  <rect id="right" class="b" x="160" y="120" width="28" height="20" rx="4"/>

  <circle id="triangle" class="b" cx="450" cy="94" r="16"/><text class="sym" x="450" y="94">△</text>
  <circle id="circle" class="b" cx="486" cy="130" r="16"/><text class="sym" x="486" y="130">○</text>
  <circle id="cross" class="b" cx="450" cy="166" r="16"/><text class="sym" x="450" y="166">✕</text>
  <circle id="square" class="b" cx="414" cy="130" r="16"/><text class="sym" x="414" y="130">□</text>

  <rect id="pad" class="b" x="240" y="80" width="120" height="60" rx="12"/>
  <rect id="create" class="b" x="203" y="92" width="22" height="10" rx="5"/>
  <rect id="options" class="b" x="375" y="92" width="22" height="10" rx="5"/>
  <circle id="ps" class="b" cx="300" cy="170" r="12"/><text class="sym" style="font-size:9px" x="300" y="170">PS</text>

  <circle id="zl" class="fond" cx="225" cy="205" r="36"/>
  <circle id="kl" class="b" cx="225" cy="205" r="22"/>
  <circle id="zr" class="fond" cx="375" cy="205" r="36"/>
  <circle id="kr" class="b" cx="375" cy="205" r="22"/>

  <text class="leg" x="300" y="264">Palettes (dos de la manette)</text>
  <rect id="pML" class="b" x="130" y="274" width="62" height="28" rx="8"/><text class="sym" x="161" y="288">ML</text>
  <rect id="pPL" class="b" x="205" y="274" width="62" height="28" rx="8"/><text class="sym" x="236" y="288">PL</text>
  <rect id="pPR" class="b" x="333" y="274" width="62" height="28" rx="8"/><text class="sym" x="364" y="288">PR</text>
  <rect id="pMR" class="b" x="408" y="274" width="62" height="28" rx="8"/><text class="sym" x="439" y="288">MR</text>
</svg>
</div>

<!-- ============ PANNEAU DE GAUCHE ============ -->
<div id="voile"></div>
<aside id="tiroir">
  <div class="tete"><b>Paramètres</b><button id="btnFermer" class="icone">✕</button></div>
  <button id="btnRetour" class="grand">← Retour au menu</button>

  <details open id="secPalettes">
    <summary>Palettes</summary>
    <button id="btnReset">Réinitialiser</button>
    <div class="mut" id="aide">Clique sur Détecter, puis appuie sur la palette sur ta manette.</div>
    <div id="reglages" style="display:flex;flex-direction:column;gap:8px"></div>
  </details>

  <div class="section" id="secVeille">
    <b>Garder l'écran allumé</b>
    <div class="mut" id="statutVeille">Choisis une vidéo (courte de préférence) : elle sera lue en boucle, sans son.</div>
    <label class="btn" for="fichierVideo">Choisir une vidéo</label>
    <input type="file" id="fichierVideo" accept="video/*" style="display:none">
  </div>

  <details id="secInfos">
    <summary>Infos manette</summary>
    <div class="mut" id="manette">Appuie sur un bouton de la manette...</div>
    <div class="mut" id="sticks"></div>
    <div class="mut" id="brut"></div>
  </details>
</aside>

<video id="videoVeille" loop muted playsinline webkit-playsinline disablePictureInPicture></video>

<script>
const PAL = ["ML","PL","PR","MR"];
const CIBLES = [["-1","Aucune"],["0","✕ (A)"],["1","○ (B)"],["2","□ (X)"],["3","△ (Y)"],
  ["4","L1"],["5","R1"],["10","L3"],["11","R3"],["8","Create"],["9","Options"],
  ["12","Haut"],["13","Bas"],["14","Gauche"],["15","Droite"]];
const VUE = location.search.includes("vue");   // affichage seul, sans envoyer

let ws, dernier = "", authOk = false, ecranCourant = "menu", codeEnvoye = "", envoiAuto = true;
let appren = null, precedent = [];

let cfg = {idx: {ML:-1, PL:-1, PR:-1, MR:-1}, cible: {ML:-1, PL:-1, PR:-1, MR:-1}};
try {
  const s = JSON.parse(localStorage.getItem("cfgManette") || "null");
  if (s && s.idx && s.cible) cfg = s;
} catch (e) {}
function sauver() { try { localStorage.setItem("cfgManette", JSON.stringify(cfg)); } catch (e) {} }
function codeStocke() { try { return localStorage.getItem("codeManette") || ""; } catch (e) { return ""; } }
function stockerCode(c) {
  try { if (c) localStorage.setItem("codeManette", c); else localStorage.removeItem("codeManette"); } catch (e) {}
}

const zone = document.getElementById("reglages");
PAL.forEach(n => {
  const opts = CIBLES.map(c => `<option value="${c[0]}">${c[1]}</option>`).join("");
  zone.insertAdjacentHTML("beforeend",
    `<div class="lig"><b>${n}</b><span class="mut" id="i_${n}"></span>
     <button data-n="${n}">Détecter</button>
     <span class="mut">copie :</span><select id="c_${n}">${opts}</select></div>`);
});

const el = {};
document.querySelectorAll("[id]").forEach(n => el[n.id] = n);

if (VUE) {
  el.secPalettes.style.display = "none";
  el.secVeille.style.display = "none";
  el.btnEdit.style.display = "none";
  el.btnRetour.style.display = "none";
}

function majReglages() {
  PAL.forEach(n => {
    el["i_" + n].textContent = cfg.idx[n] >= 0 ? "bouton n° " + cfg.idx[n] : "non défini";
    el["c_" + n].value = String(cfg.cible[n]);
  });
  document.querySelectorAll("#reglages button").forEach(b =>
    b.classList.toggle("att", b.dataset.n === appren));
}
zone.addEventListener("click", e => {
  const n = e.target.dataset && e.target.dataset.n;
  if (!n) return;
  appren = (appren === n) ? null : n;
  el.aide.textContent = appren ? "Appuie maintenant sur la palette " + appren + " de la manette..."
                               : "Clique sur Détecter, puis appuie sur la palette sur ta manette.";
  majReglages();
});
zone.addEventListener("change", e => {
  if (e.target.id && e.target.id.startsWith("c_")) {
    cfg.cible[e.target.id.slice(2)] = Number(e.target.value);
    sauver();
  }
});
el.btnReset.addEventListener("click", () => {
  cfg = {idx: {ML:-1, PL:-1, PR:-1, MR:-1}, cible: {ML:-1, PL:-1, PR:-1, MR:-1}};
  sauver(); majReglages();
});
majReglages();

// ---------- Panneau de gauche ----------
function ouvrirTiroir() { el.tiroir.classList.add("ouvert"); el.voile.classList.add("ouvert"); }
function fermerTiroir() { el.tiroir.classList.remove("ouvert"); el.voile.classList.remove("ouvert"); }
el.btnMenu.addEventListener("click", ouvrirTiroir);
el.btnFermer.addEventListener("click", fermerTiroir);
el.voile.addEventListener("click", fermerTiroir);
el.btnRetour.addEventListener("click", () => montrerEcran("menu"));

// ---------- Ecrans ----------
function montrerEcran(n) {
  const quitte = ecranCourant === "jeu" && n !== "jeu";
  ecranCourant = n;
  el.ecranMenu.style.display = n === "menu" ? "flex" : "none";
  el.ecranJeu.style.display = n === "jeu" ? "flex" : "none";
  fermerTiroir();
  if (quitte) arretTotal();
}
function arretTotal() {
  reglerEdit(false);
  appren = null; majReglages();
  if (!VUE && ws && ws.readyState === 1 && authOk)
    ws.send(JSON.stringify({b: [], p: [0,0,0,0], a: [0,0,0,0], lt: 0, rt: 0}));
  dernier = "";
}
function montrerBlocs() {
  el.blocCode.style.display = authOk ? "none" : "flex";
  el.blocOk.style.display = authOk ? "flex" : "none";
}
el.btnJouer.addEventListener("click", () => montrerEcran("jeu"));
el.btnOubli.addEventListener("click", () => {
  stockerCode("");
  envoyerCode("", true);
});

// ---------- Code de connexion ----------
function validerCode() {
  const c = el.champCode.value.trim();
  if (c.length !== 4) { el.msgCode.textContent = "Le code a 4 chiffres."; return; }
  el.msgCode.textContent = "Vérification...";
  envoyerCode(c, false);
}
el.champCode.addEventListener("input", () => {
  el.champCode.value = el.champCode.value.replace(/\D/g, "").slice(0, 4);
});
el.champCode.addEventListener("keydown", e => { if (e.key === "Enter") validerCode(); });
el.btnCode.addEventListener("click", validerCode);

function envoyerCode(c, auto) {
  codeEnvoye = c; envoiAuto = !!auto;
  if (ws && ws.readyState === 1) ws.send(JSON.stringify({auth: c}));
}
function majEtat(t, ok) {
  el.etat.textContent = t; el.etatM.textContent = t;
  [el.pt, el.ptM].forEach(p => p.classList.toggle("ok", ok));
}
function reponseAuth(m) {
  authOk = !!m.auth;
  dernier = "";
  if (m.code) {
    el.codePC.textContent = "Code pour le téléphone : " + m.code;
    el.codeBadge.textContent = "Code : " + m.code;
  }
  if (authOk) {
    if (codeEnvoye) stockerCode(codeEnvoye);
    majEtat("PC connecté", true);
    el.msgCode.textContent = "";
    el.champCode.value = "";
    if (VUE) montrerEcran("jeu");
  } else {
    majEtat("Code requis", false);
    if (m.verrou) el.msgCode.textContent = "Trop d'essais. Réessaie dans " + m.verrou + " s.";
    else if (!envoiAuto) el.msgCode.textContent = "Code incorrect" + (m.reste ? " (" + m.reste + " essais restants)" : "") + ".";
    else el.msgCode.textContent = codeEnvoye ? "Le code a changé. Entre celui affiché sur le PC." : "";
    stockerCode("");
    if (ecranCourant === "jeu") montrerEcran("menu");
  }
  montrerBlocs();
}

// ---------- Affichage ----------
const IDS = {0:"cross",1:"circle",2:"square",3:"triangle",4:"l1",5:"r1",8:"create",9:"options",
  10:"kl",11:"kr",12:"up",13:"down",14:"left",15:"right",16:"ps",17:"pad"};
const NOMS = {0:"✕",1:"○",2:"□",3:"△",4:"L1",5:"R1",8:"Create",9:"Options",10:"L3",11:"R3",
  12:"Haut",13:"Bas",14:"Gauche",15:"Droite",16:"PS",17:"Pavé tactile"};

function afficher(d) {
  const actifs = [];
  for (const i in IDS) {
    const on = !!d.b[i];
    el[IDS[i]].classList.toggle("on", on);
    if (on) actifs.push(NOMS[i]);
  }
  const p = d.p || [];
  PAL.forEach((n, k) => {
    el["p" + n].classList.toggle("on", !!p[k]);
    if (p[k]) actifs.push(n);
  });
  if (d.lt > 0.02) actifs.push("L2 " + Math.round(d.lt * 100) + "%");
  if (d.rt > 0.02) actifs.push("R2 " + Math.round(d.rt * 100) + "%");
  el.l2f.setAttribute("width", 90 * d.lt);
  el.r2f.setAttribute("width", 90 * d.rt);
  el.kl.setAttribute("cx", 225 + d.a[0] * 22); el.kl.setAttribute("cy", 205 + d.a[1] * 22);
  el.kr.setAttribute("cx", 375 + d.a[2] * 22); el.kr.setAttribute("cy", 205 + d.a[3] * 22);
  el.actifs.textContent = actifs.length ? actifs.join("  ·  ") : "Aucun bouton";
  el.sticks.textContent = "Stick gauche : " + d.a[0].toFixed(2) + " ; " + d.a[1].toFixed(2) +
    "   |   Stick droit : " + d.a[2].toFixed(2) + " ; " + d.a[3].toFixed(2);
}

// ---------- Connexion au PC ----------
function connecter() {
  ws = new WebSocket("ws://" + location.hostname + ":8765");
  ws.onopen = () => { majEtat("PC joint, vérification...", false); envoyerCode(codeStocke(), true); };
  ws.onclose = () => {
    authOk = false;
    majEtat("PC déconnecté, nouvelle tentative...", false);
    el.lat.textContent = "";
    montrerBlocs();
    setTimeout(connecter, 1000);
  };
  ws.onmessage = e => {
    let m;
    try { m = JSON.parse(e.data); } catch (x) { return; }
    if ("pong" in m) el.lat.textContent = "Latence : " + (Date.now() - m.pong) + " ms";
    else if ("auth" in m) reponseAuth(m);
    else if (m.b) afficher(m);
  };
}
connecter();
setInterval(() => { if (ws.readyState === 1) ws.send(JSON.stringify({ping: Date.now()})); }, 2000);

// ---------- Garder l'écran allumé : vidéo choisie, lue en boucle ----------
let verrou = null, veilleActive = false;
const vid = el.videoVeille;

async function demanderVerrou() {
  try { verrou = await navigator.wakeLock.request("screen"); return true; }
  catch (e) { return false; }
}
document.addEventListener("pointerdown", demanderVerrou, {once: true});

async function lancerVideo() {
  try {
    vid.loop = true;
    vid.muted = true;
    await vid.play();
    veilleActive = true;
    el.statutVeille.textContent = "Vidéo lue en boucle : l'écran devrait rester allumé.";
  } catch (err) {
    el.statutVeille.textContent = "Touche la petite vidéo en bas à droite pour la lancer.";
  }
}

el.fichierVideo.addEventListener("change", async e => {
  const f = e.target.files && e.target.files[0];
  if (!f) return;
  if (vid.dataset.url) URL.revokeObjectURL(vid.dataset.url);
  const url = URL.createObjectURL(f);
  vid.dataset.url = url;
  vid.src = url;
  vid.style.display = "block";
  el.statutVeille.textContent = "Chargement de la vidéo...";
  await demanderVerrou();
  await lancerVideo();
});

vid.addEventListener("click", lancerVideo);
vid.addEventListener("pause", () => {
  if (veilleActive && document.visibilityState === "visible" && !vid.ended) vid.play().catch(() => {});
});
document.addEventListener("visibilitychange", async () => {
  if (document.visibilityState !== "visible" || !veilleActive) return;
  await demanderVerrou();
  vid.play().catch(() => {});
});

// ---------- Mode Edit : toucher la manette dessinée ----------
const svg = document.querySelector("svg");
const REV = {};
for (const i in IDS) REV[IDS[i]] = Number(i);
const touch = {b: {}, p: [0,0,0,0], lt: 0, rt: 0, a: [0,0,0,0]};
const doigts = new Map();   // identifiant du doigt -> action en cours
let modeEdit = false, forcer = false;

function actionPour(id) {
  if (id === "kl" || id === "zl") return {t: "stick", c: 0};
  if (id === "kr" || id === "zr") return {t: "stick", c: 1};
  if (id === "l2f" || id === "l2z") return {t: "gach", c: "lt"};
  if (id === "r2f" || id === "r2z") return {t: "gach", c: "rt"};
  if (id.startsWith("p") && PAL.includes(id.slice(1))) return {t: "pal", c: PAL.indexOf(id.slice(1))};
  if (id in REV) return {t: "btn", c: REV[id]};
  return null;
}
function appliquer(a, on) {
  if (a.t === "btn") touch.b[a.c] = on ? 1 : 0;
  else if (a.t === "pal") touch.p[a.c] = on ? 1 : 0;
  else if (a.t === "gach") touch[a.c] = on ? 1 : 0;
}
function majStick(a, e) {
  let dx = (e.clientX - a.cx) / a.rayon, dy = (e.clientY - a.cy) / a.rayon;
  const n = Math.hypot(dx, dy);
  if (n > 1) { dx /= n; dy /= n; }
  if (n > 0.2) a.bouge = true;
  touch.a[a.c * 2] = dx;
  touch.a[a.c * 2 + 1] = dy;
}

svg.addEventListener("pointerdown", e => {
  if (!modeEdit) return;
  const a = actionPour(e.target.id || "");
  if (!a) return;
  e.preventDefault();
  try { svg.setPointerCapture(e.pointerId); } catch (x) {}
  if (a.t === "stick") {
    const m = svg.getScreenCTM();
    a.cx = m.a * (a.c ? 375 : 225) + m.e;
    a.cy = m.d * 205 + m.f;
    a.rayon = 36 * m.a;
    a.t0 = Date.now();
    a.bouge = false;
    majStick(a, e);
  } else {
    appliquer(a, true);
  }
  doigts.set(e.pointerId, a);
});
svg.addEventListener("pointermove", e => {
  const a = doigts.get(e.pointerId);
  if (a && a.t === "stick") { e.preventDefault(); majStick(a, e); }
});
function relacher(e) {
  const a = doigts.get(e.pointerId);
  if (!a) return;
  doigts.delete(e.pointerId);
  if (a.t === "stick") {
    touch.a[a.c * 2] = 0;
    touch.a[a.c * 2 + 1] = 0;
    if (!a.bouge && Date.now() - a.t0 < 300) {      // simple tap sur un stick = L3 / R3
      const i = a.c ? 11 : 10;
      touch.b[i] = 1;
      setTimeout(() => { touch.b[i] = 0; }, 120);
    }
  } else {
    appliquer(a, false);
  }
}
svg.addEventListener("pointerup", relacher);
svg.addEventListener("pointercancel", relacher);
svg.addEventListener("contextmenu", e => e.preventDefault());

function reglerEdit(on) {
  modeEdit = on;
  svg.classList.toggle("edit", on);
  el.btnEdit.textContent = "Edit : " + (on ? "ON" : "OFF");
  el.btnEdit.classList.toggle("att", on);
  if (!on) {
    doigts.clear();
    touch.b = {}; touch.p = [0,0,0,0]; touch.lt = 0; touch.rt = 0; touch.a = [0,0,0,0];
    forcer = true;      // envoie un état « tout relâché » au PC
  }
}
el.btnEdit.addEventListener("click", () => reglerEdit(!modeEdit));

// ---------- Boucle principale ----------
function boucle() {
  if (ecranCourant === "jeu" && authOk) {
    const g = VUE ? null : [...navigator.getGamepads()].find(x => x);
    if (g || modeEdit || forcer) {
      forcer = false;
      const rb = g ? g.buttons.map(x => x.pressed ? 1 : 0) : [];

      if (appren && g) {
        const nouveau = rb.findIndex((v, i) => v && !precedent[i]);
        if (nouveau >= 0) {
          cfg.idx[appren] = nouveau;
          sauver();
          appren = null;
          el.aide.textContent = "Enregistré. Clique sur Détecter pour une autre palette.";
          majReglages();
        }
      }
      precedent = rb;

      const b = rb.slice();
      while (b.length < 18) b.push(0);
      for (const i in touch.b) if (touch.b[i]) b[i] = 1;

      const pal = PAL.map((n, k) => ((cfg.idx[n] >= 0 && rb[cfg.idx[n]]) || touch.p[k]) ? 1 : 0);
      PAL.forEach((n, k) => {
        const c = Number(cfg.cible[n]);
        if (pal[k] && c >= 0) b[c] = 1;
      });

      if (g) {
        const presses = [];
        rb.forEach((v, i) => { if (v) presses.push(i); });
        el.brut.textContent = "Indices bruts appuyés : " + (presses.length ? presses.join(", ") : "aucun") +
          "  (" + rb.length + " boutons détectés)";
        el.manette.textContent = "Manette détectée";
      }

      const a = [0, 1, 2, 3].map(i => {
        const v = g && g.axes[i] ? g.axes[i] : 0;
        const vz = Math.abs(v) < 0.08 ? 0 : v;
        return touch.a[i] !== 0 ? touch.a[i] : vz;
      });
      const d = {
        b: b, p: pal, a: a,
        lt: Math.max(g && g.buttons[6] ? g.buttons[6].value : 0, touch.lt),
        rt: Math.max(g && g.buttons[7] ? g.buttons[7].value : 0, touch.rt) };
      const s = JSON.stringify(d);
      if (s !== dernier) {
        dernier = s;
        afficher(d);
        if (ws.readyState === 1) ws.send(s);
      }
    }
  }
  requestAnimationFrame(boucle);
}
document.addEventListener("keydown", e => {
  if (!/INPUT|SELECT|TEXTAREA/.test(e.target.tagName)) e.preventDefault();
});
boucle();
</script></body></html>"""

def quitter_avec_message(texte):
    print("\n" + texte)
    try:                                         # fenêtre d'erreur (le programme n'a pas de console)
        import tkinter as tk
        from tkinter import messagebox
        r = tk.Tk()
        r.withdraw()
        messagebox.showerror("Manette Virtuelle", texte)
        r.destroy()
    except Exception:
        pass
    sys.exit(1)


try:
    pad = vg.VX360Gamepad()
except Exception as e:
    quitter_avec_message(
        "Impossible de créer la manette virtuelle.\n"
        "Le pilote ViGEmBus n'est probablement pas installé.\n"
        "Installe-le (ViGEmBus_Setup.exe), redémarre le PC si besoin, puis relance.\n"
        f"Détail : {e}")
B = vg.XUSB_BUTTON
MAP = {0: B.XUSB_GAMEPAD_A, 1: B.XUSB_GAMEPAD_B, 2: B.XUSB_GAMEPAD_X,
       3: B.XUSB_GAMEPAD_Y, 4: B.XUSB_GAMEPAD_LEFT_SHOULDER,
       5: B.XUSB_GAMEPAD_RIGHT_SHOULDER, 8: B.XUSB_GAMEPAD_BACK,
       9: B.XUSB_GAMEPAD_START, 10: B.XUSB_GAMEPAD_LEFT_THUMB,
       11: B.XUSB_GAMEPAD_RIGHT_THUMB, 12: B.XUSB_GAMEPAD_DPAD_UP,
       13: B.XUSB_GAMEPAD_DPAD_DOWN, 14: B.XUSB_GAMEPAD_DPAD_LEFT,
       15: B.XUSB_GAMEPAD_DPAD_RIGHT, 16: B.XUSB_GAMEPAD_GUIDE}

# ---------- Sécurité : code de connexion ----------
CODE = f"{secrets.randbelow(10000):04d}"     # nouveau code à chaque lancement
LOCAL = ("127.0.0.1", "::1", "::ffff:127.0.0.1")
essais = {}                                  # ip -> [échecs, verrouillé jusqu'à]

clients = set()                              # uniquement les appareils authentifiés


def ip_locale():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("10.255.255.255", 1))
        return s.getsockname()[0]
    except Exception:
        return socket.gethostbyname(socket.gethostname())
    finally:
        s.close()


IP_PC = ip_locale()
HOTES_OK = {"localhost", "127.0.0.1", "::1", IP_PC}


def ip_de(ws):
    try:
        return ws.remote_address[0]
    except Exception:
        return "?"


def origine_ok(ws):
    """Refuse les connexions venant d'un autre site web (protection contre
    un site malveillant ouvert dans le navigateur du PC)."""
    try:
        h = ws.request.headers               # websockets récent
    except AttributeError:
        try:
            h = ws.request_headers           # ancienne version
        except AttributeError:
            return False
    o = h.get("Origin")
    if not o:
        return True                          # client non navigateur
    try:
        u = urlparse(o)
        return u.port == 8000 and u.hostname in HOTES_OK
    except ValueError:
        return False


def verifier(ip, code):
    if ip in LOCAL:                          # le PC lui-même : pas de code
        return {"auth": True, "code": CODE}
    etat = essais.setdefault(ip, [0, 0.0])
    reste = etat[1] - time.time()
    if reste > 0:
        return {"auth": False, "verrou": int(reste) + 1}
    if not code:
        return {"auth": False}
    if secrets.compare_digest(code.encode("utf-8"), CODE.encode("utf-8")):
        etat[0] = 0
        return {"auth": True}
    etat[0] += 1
    if etat[0] >= 5:                         # 5 erreurs = blocage 60 secondes
        etat[0] = 0
        etat[1] = time.time() + 60
        return {"auth": False, "verrou": 60}
    return {"auth": False, "reste": 5 - etat[0]}


def nombre(x, bas, haut):
    try:
        x = float(x)
    except (TypeError, ValueError):
        return 0.0
    if x != x:
        return 0.0
    return max(bas, min(haut, x))


def tout_relacher():
    pad.reset()
    pad.update()


async def recevoir(ws):
    ip = ip_de(ws)
    if not origine_ok(ws):
        await ws.close()
        return
    ok = False
    emetteur = False
    try:
        async for msg in ws:
            try:
                d = json.loads(msg)
            except ValueError:
                continue
            if not isinstance(d, dict):
                continue
            if "ping" in d:
                await ws.send(json.dumps({"pong": d["ping"]}))
                continue
            if "auth" in d:
                rep = verifier(ip, str(d["auth"]))
                if rep["auth"] and not ok:
                    print("Appareil connecte :", ip)
                ok = rep["auth"]
                if ok:
                    clients.add(ws)
                else:
                    clients.discard(ws)
                await ws.send(json.dumps(rep))
                continue
            if not ok:
                continue                     # ignoré tant que le code n'est pas bon
            b, a = d.get("b"), d.get("a")
            if not isinstance(b, list) or not isinstance(a, list) or len(a) < 4:
                continue
            emetteur = True
            mask = 0
            for i, bouton in MAP.items():
                if i < len(b) and b[i]:
                    mask |= bouton
            pad.report.wButtons = mask
            pad.left_joystick_float(nombre(a[0], -1, 1), -nombre(a[1], -1, 1))
            pad.right_joystick_float(nombre(a[2], -1, 1), -nombre(a[3], -1, 1))
            pad.left_trigger_float(nombre(d.get("lt"), 0, 1))
            pad.right_trigger_float(nombre(d.get("rt"), 0, 1))
            pad.update()
            websockets.broadcast(clients - {ws}, msg)
    except Exception:
        pass
    finally:
        clients.discard(ws)
        if emetteur:
            tout_relacher()
        if ok:
            print("Appareil deconnecte :", ip)


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        data = PAGE.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Frame-Options", "DENY")
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *a):
        pass


async def main():
    serveur_http = http.server.ThreadingHTTPServer(("0.0.0.0", 8000), Handler)
    threading.Thread(target=serveur_http.serve_forever, daemon=True).start()
    print("\n=== Serveur manette pret ===")
    print(f"Sur le telephone (Chrome/Safari) :  http://{IP_PC}:8000")
    print(f"CODE DE CONNEXION : {CODE}")
    print("Sur le PC (affichage seul, montre aussi le code) :  http://localhost:8000/?vue\n")
    async with websockets.serve(recevoir, "0.0.0.0", 8765, max_size=4096):
        await asyncio.Future()


erreur_serveur = []


def lancer_serveur():
    try:
        asyncio.run(main())
    except OSError as e:
        erreur_serveur.append(
            "Impossible de démarrer le serveur (port 8000 ou 8765 déjà utilisé ?).\n"
            "Ferme l'autre instance de Manette Virtuelle puis relance.\n"
            f"Détail : {e}")
    except Exception as e:
        erreur_serveur.append(f"Le serveur s'est arrêté : {e}")


# ---------- Fenêtre du programme (adresse + code à 4 chiffres) ----------
def ressource(nom):
    base = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, nom)


def nb_appareils():
    try:
        return sum(1 for c in list(clients) if ip_de(c) not in LOCAL)
    except Exception:
        return 0


def image_qr(url):
    """QR code de l'adresse (PNG en base64), ou None si indisponible."""
    try:
        import segno
        tampon = io.BytesIO()
        segno.make(url, error="m").save(tampon, kind="png", scale=5, border=2)
        return base64.b64encode(tampon.getvalue()).decode("ascii")
    except Exception:
        return None


def fenetre():
    import tkinter as tk
    try:                                         # texte net sur les écrans à forte résolution
        import ctypes
        ctypes.windll.shcore.SetProcessDpiAwareness(1)
    except Exception:
        pass

    BG, CARTE, LIGNE = "#0f1218", "#181c25", "#2d3342"
    ACC, TXT, MUT, VERT, ROUGE = "#35c2ff", "#e8ecf4", "#8b94a8", "#30c48d", "#e5484d"
    url = f"http://{IP_PC}:8000"

    racine = tk.Tk()
    racine.title("Manette Virtuelle")
    racine.configure(bg=BG)
    racine.resizable(False, False)
    try:
        racine.iconbitmap(ressource("icone.ico"))
    except Exception:
        pass

    def carte():
        return tk.Frame(racine, bg=CARTE, highlightbackground=LIGNE,
                        highlightthickness=1, padx=18, pady=14)

    tk.Label(racine, text="Manette Virtuelle", bg=BG, fg=TXT,
             font=("Segoe UI", 20, "bold")).pack(padx=28, pady=(20, 2))
    ligne = tk.Frame(racine, bg=BG)
    ligne.pack()
    point = tk.Label(ligne, text="●", bg=BG, fg=ROUGE, font=("Segoe UI", 12))
    point.pack(side="left")
    etat = tk.Label(ligne, text="En attente du téléphone...", bg=BG, fg=MUT,
                    font=("Segoe UI", 11))
    etat.pack(side="left", padx=(6, 0))

    # Étape 1 : l'adresse
    c1 = carte()
    c1.pack(fill="x", padx=28, pady=(16, 8))
    tk.Label(c1, text="1.  Sur ton téléphone, ouvre cette adresse :", bg=CARTE, fg=MUT,
             font=("Segoe UI", 11), anchor="w").pack(fill="x")
    tk.Label(c1, text=url, bg=CARTE, fg=ACC,
             font=("Consolas", 18, "bold")).pack(pady=(8, 8))
    bouton = tk.Button(c1, text="Copier l'adresse", bg="#2b3140", fg=TXT,
                       activebackground="#3a4254", activeforeground=TXT, relief="flat",
                       font=("Segoe UI", 10), padx=12, pady=4, cursor="hand2")
    bouton.pack()

    def copier():
        racine.clipboard_clear()
        racine.clipboard_append(url)
        bouton.config(text="Copié ✓")
        racine.after(1500, lambda: bouton.config(text="Copier l'adresse"))
    bouton.config(command=copier)

    # Étape 2 : le code
    c2 = carte()
    c2.pack(fill="x", padx=28, pady=8)
    tk.Label(c2, text="2.  Tape ce code sur le téléphone :", bg=CARTE, fg=MUT,
             font=("Segoe UI", 11), anchor="w").pack(fill="x")
    tk.Label(c2, text="  ".join(CODE), bg=CARTE, fg=TXT,
             font=("Consolas", 54, "bold")).pack(pady=(6, 0))

    # QR code (facultatif)
    donnees_qr = image_qr(url)
    if donnees_qr:
        try:
            photo = tk.PhotoImage(data=donnees_qr)
            racine._photo_qr = photo             # garde l'image en vie
            tk.Label(racine, text="ou scanne ce QR code avec ton téléphone", bg=BG, fg=MUT,
                     font=("Segoe UI", 10)).pack(pady=(10, 4))
            tk.Label(racine, image=photo, bg=BG).pack()
        except Exception:
            pass

    tk.Label(racine, text="Le téléphone et le PC doivent être sur le même Wi-Fi.\n"
                          "Garde cette fenêtre ouverte pendant que tu joues. La fermer arrête le programme.",
             bg=BG, fg=MUT, font=("Segoe UI", 9), justify="center").pack(padx=28, pady=(14, 20))

    def maj():
        if erreur_serveur:
            from tkinter import messagebox
            messagebox.showerror("Manette Virtuelle", erreur_serveur[0])
            racine.destroy()
            return
        n = nb_appareils()
        if n:
            point.config(fg=VERT)
            etat.config(fg=VERT, text="1 téléphone connecté" if n == 1 else f"{n} appareils connectés")
        else:
            point.config(fg=ROUGE)
            etat.config(fg=MUT, text="En attente du téléphone...")
        racine.after(700, maj)

    racine.after(300, maj)
    racine.mainloop()


fil_serveur = threading.Thread(target=lancer_serveur, daemon=True)
fil_serveur.start()
try:
    fenetre()
except ImportError:                              # pas de tkinter : on reste en mode console
    try:
        fil_serveur.join()
    except KeyboardInterrupt:
        pass
    if erreur_serveur:
        quitter_avec_message(erreur_serveur[0])
