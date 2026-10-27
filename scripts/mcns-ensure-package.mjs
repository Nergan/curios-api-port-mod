// Creates a public one-slot package when it is missing.
// Authentication matches scripts/mcns-release.mjs: MCNS_WORKER_TOKEN, or
// MCNS_WORKER_ID plus MCNS_WORKER_KEY. The layout is frozen after creation.
import { createHash, createPrivateKey, randomBytes, sign } from "node:crypto";

const base = (process.env.MCNS_CDN_URL ?? "https://cdn.mikchan.net").replace(/\/$/, "");
const token = process.env.MCNS_WORKER_TOKEN || "";
const worker = process.env.MCNS_WORKER_ID || "";

function pemFromEnv() {
  let pem = process.env.MCNS_WORKER_KEY ?? "";
  pem = pem.replace(/^\uFEFF/, "").trim();
  if (!pem.includes("\n") && pem.includes("\\n")) {
    pem = pem.replace(/\\n/g, "\n");
  }
  if (!pem.includes("BEGIN")) {
    throw new Error("MCNS_WORKER_KEY is empty or not a PEM private key");
  }
  return pem.endsWith("\n") ? pem : `${pem}\n`;
}

if (!token && (!worker || !process.env.MCNS_WORKER_KEY)) {
  throw new Error("Set MCNS_WORKER_ID and MCNS_WORKER_KEY, or MCNS_WORKER_TOKEN");
}

const privateKey = token ? null : createPrivateKey(pemFromEnv());
const sha = (data) => createHash("sha256").update(data).digest("hex");
const signText = (text) =>
  sign(null, Buffer.from(text), privateKey).toString("base64url");

function authorize(method, path, bodyHash) {
  if (token) return `Bearer ${token}`;
  const ts = Math.floor(Date.now() / 1000);
  const nonce = randomBytes(16).toString("base64url");
  const sig = signText(
    ["MCNS-CDN-REQUEST-V1", method, path, String(ts), nonce, worker, bodyHash].join("\n"),
  );
  return `MCNS-Ed25519 keyId="${worker}",ts="${ts}",nonce="${nonce}",sig="${sig}"`;
}

async function call(method, path, { json } = {}) {
  const body = Buffer.from(json === undefined ? "" : JSON.stringify(json));
  const res = await fetch(base + path, {
    method,
    headers: {
      authorization: authorize(method, path, sha(body)),
      ...(json === undefined ? {} : { "content-type": "application/json" }),
    },
    body: body.length ? body : undefined,
  });
  if (!res.ok) {
    const text = await res.text();
    const error = new Error(`${method} ${path}: ${res.status} ${text.slice(0, 500)}`);
    error.status = res.status;
    throw error;
  }
  return res;
}

async function main() {
  const args = process.argv.slice(2);
  const [pkg, slot, title, templateArg] = args;
  if (!pkg || !slot || args.length > 4) {
    throw new Error(
      "usage: node scripts/mcns-ensure-package.mjs <package> <slot> [title] [template]",
    );
  }
  const template = templateArg || "[package]-[version].jar";
  const layout = { [slot]: template };
  let existing = null;
  try {
    existing = await (await call("GET", `/v1/packages/${pkg}`)).json();
  } catch (error) {
    if (error.status !== 404) throw error;
  }
  if (!existing) {
    await call("POST", "/v1/packages", {
      json: {
        id: pkg,
        visibility: "public",
        layout,
        metadata: {
          title: title || pkg,
          minecraft: "1.21.1",
          loader: "fabric",
          homepage: "https://github.com/Nergan/curios-api-port-mod",
        },
      },
    });
    console.log(`created ${pkg}`);
    return;
  }
  const slots = Object.keys(existing.layout || {});
  if (slots.length !== 1 || slots[0] !== slot || existing.layout[slot] !== template) {
    throw new Error(
      `${pkg} layout is ${JSON.stringify(existing.layout || {})}, expected ${JSON.stringify(layout)}`,
    );
  }
  if (existing.visibility !== "public") {
    throw new Error(`${pkg} is ${existing.visibility}; Kidou needs a public package`);
  }
  console.log(`exists ${pkg}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
