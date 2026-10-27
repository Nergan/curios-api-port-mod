// Публикует уже собранные jar на McnCDN.
// Аутентификация: MCNS_WORKER_ID + MCNS_WORKER_KEY, либо MCNS_WORKER_TOKEN.
// Номер — SemVer MAJOR.MINOR.PATCH. Если записанный номер не выше пола
// пакета, скрипт берёт следующий PATCH. Те же исходники повторно не заливает.
import { createHash, createPrivateKey, randomBytes, sign } from "node:crypto";
import { createReadStream, realpathSync, statSync } from "node:fs";
import http from "node:http";
import https from "node:https";
import { basename } from "node:path";
import { fileURLToPath } from "node:url";

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

const sha = (data) => createHash("sha256").update(data).digest("hex");
let privateKey = null;

function signingKey() {
  if (!privateKey) privateKey = createPrivateKey(pemFromEnv());
  return privateKey;
}

const signText = (text) =>
  sign(null, Buffer.from(text), signingKey()).toString("base64url");

const RELEASE = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/;

export function assertReleaseVersion(version) {
  const match = RELEASE.exec(version);
  if (!match) {
    throw new Error(`${version} must be SemVer MAJOR.MINOR.PATCH, without a pre-release or +build`);
  }
  return [BigInt(match[1]), BigInt(match[2]), BigInt(match[3])];
}

export function compareSemver(left, right) {
  const a = assertReleaseVersion(left);
  const b = assertReleaseVersion(right);
  for (let i = 0; i < 3; i++) {
    if (a[i] < b[i]) return -1;
    if (a[i] > b[i]) return 1;
  }
  return 0;
}

function authorize(method, path, bodyHash) {
  if (token) return `Bearer ${token}`;
  const ts = Math.floor(Date.now() / 1000);
  const nonce = randomBytes(16).toString("base64url");
  const sig = signText(
    ["MCNS-CDN-REQUEST-V1", method, path, String(ts), nonce, worker, bodyHash].join("\n"),
  );
  return `MCNS-Ed25519 keyId="${worker}",ts="${ts}",nonce="${nonce}",sig="${sig}"`;
}

function hashFile(path) {
  return new Promise((resolve, reject) => {
    const digest = createHash("sha256");
    const input = createReadStream(path);
    input.on("error", reject);
    input.on("data", (chunk) => digest.update(chunk));
    input.on("end", () => resolve(digest.digest("hex")));
  });
}

function artifactSignature(pkg, version, slot, fileHash, size) {
  if (token) return {};
  const statement = [
    "MCNS-CDN-ARTIFACT-V1",
    pkg,
    version,
    slot,
    fileHash,
    String(size),
  ].join("\n");
  return { "x-artifact-signature": signText(statement) };
}

// Node fetch aborts a request that has not received response headers in 5 minutes.
// Sengoku is about 500 MB, and the CDN answers only after the body arrives.
function upload(urlString, filePath, headers) {
  const url = new URL(base + urlString);
  const size = statSync(filePath).size;
  const transport = url.protocol === "https:" ? https : http;
  return new Promise((resolve, reject) => {
    let settled = false;
    const fail = (error) => {
      if (settled) return;
      settled = true;
      reject(error);
    };
    const done = () => {
      if (settled) return;
      settled = true;
      resolve();
    };
    const req = transport.request(
      url,
      {
        method: "PUT",
        headers: {
          ...headers,
          "content-length": String(size),
        },
      },
      (res) => {
        const chunks = [];
        res.on("data", (chunk) => chunks.push(chunk));
        res.on("end", () => {
          const text = Buffer.concat(chunks).toString("utf8");
          if (res.statusCode < 200 || res.statusCode >= 300) {
            const snippet = text.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
            const error = new Error(
              `PUT ${url.pathname}: ${res.statusCode} ${snippet.slice(0, 180)}`,
            );
            error.status = res.statusCode;
            error.body = text;
            fail(error);
            return;
          }
          done();
        });
      },
    );
    req.setTimeout(30 * 60 * 1000, () => {
      req.destroy(new Error("upload stalled for 30 minutes"));
    });
    req.on("error", fail);
    let sent = 0;
    let nextMark = 0.25;
    const input = createReadStream(filePath);
    input.on("data", (chunk) => {
      sent += chunk.length;
      if (size > 0 && sent / size >= nextMark) {
        console.log(`uploaded ${Math.min(100, Math.round((sent / size) * 100))}%`);
        while (nextMark <= sent / size) nextMark += 0.25;
      }
    });
    input.on("error", fail);
    input.pipe(req);
  });
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function retryable(error) {
  return !error.status || error.status === 502 || error.status === 503 || error.status === 504;
}

async function withGatewayRetry(label, fn) {
  for (let attempt = 1; attempt <= 4; attempt++) {
    try {
      return await fn(attempt);
    } catch (error) {
      if (!retryable(error) || attempt === 4) throw error;
      const wait = 30 * attempt;
      const brief = String(error.message || error).split("\n")[0].slice(0, 180);
      console.error(`${label} attempt ${attempt} failed: ${brief}. Retrying in ${wait}s.`);
      await sleep(wait * 1000);
    }
  }
}

async function call(method, path, { json, raw, headers = {} } = {}) {
  const body = raw ?? Buffer.from(json === undefined ? "" : JSON.stringify(json));
  const res = await fetch(base + path, {
    method,
    headers: {
      authorization: authorize(method, path, sha(raw ? "" : body)),
      ...(json === undefined ? {} : { "content-type": "application/json" }),
      ...headers,
    },
    body: body.length ? body : undefined,
  });
  if (!res.ok) {
    const text = await res.text();
    const error = new Error(`${method} ${path}: ${res.status} ${text.slice(0, 500)}`);
    error.status = res.status;
    error.body = text;
    throw error;
  }
  return res;
}

function requireCredentials() {
  if (!token && (!worker || !process.env.MCNS_WORKER_KEY)) {
    throw new Error("Set MCNS_WORKER_ID and MCNS_WORKER_KEY, or MCNS_WORKER_TOKEN");
  }
}

async function loadVersion(pkg, version) {
  try {
    return await (await call("GET", `/v1/packages/${pkg}/versions/${version}`)).json();
  } catch (error) {
    if (error.status === 404) return null;
    throw error;
  }
}

async function loadLatest(pkg) {
  try {
    return await (await call("GET", `/v1/packages/${pkg}/latest`)).json();
  } catch (error) {
    if (error.status === 404) return null;
    throw error;
  }
}

export function nextPatch(version) {
  const [major, minor, patch] = assertReleaseVersion(version);
  return `${major}.${minor}.${patch + 1n}`;
}

export function versionAbove(requested, floor) {
  assertReleaseVersion(requested);
  if (floor == null || floor === "") return requested;
  const floorText = String(floor);
  let order;
  try {
    order = compareSemver(requested, floorText);
  } catch (error) {
    throw new Error(`Package floor ${floorText} is not MAJOR.MINOR.PATCH. ${error.message}`);
  }
  if (order > 0) return requested;
  return nextPatch(floorText);
}

export function decisionWhenPackageMissing(requested) {
  assertReleaseVersion(requested);
  return { action: "publish", version: requested };
}

export function decideVersion(requested, floor, latest, source, legacy = "") {
  const published = latest?.meta?.sourceSha256 || "";
  if (
    latest?.version &&
    published &&
    (published === source || (legacy && published === legacy))
  ) {
    return { action: "unchanged", version: latest.version };
  }
  return { action: "publish", version: versionAbove(requested, floor) };
}

export function isAbsentPackage(status) {
  // An unauthenticated read of a missing package is 401, the same as a private one.
  return status === 401 || status === 404;
}

async function publicJson(path) {
  const res = await fetch(base + path);
  if (isAbsentPackage(res.status)) return null;
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`GET ${path}: ${res.status} ${text.slice(0, 300)}`);
  }
  return JSON.parse(text);
}

async function printNextVersion() {
  const [pkg, requested] = process.argv.slice(3);
  if (!pkg || !requested || process.argv.length > 5) {
    throw new Error("usage: node scripts/mcns-release.mjs --next-version <package> <version>");
  }
  const packageView = await publicJson(`/v1/packages/${pkg}`);
  if (!packageView) {
    const decision = decisionWhenPackageMissing(requested);
    console.log(`${decision.action} ${decision.version}`);
    return;
  }
  const latest = await publicJson(`/v1/packages/${pkg}/latest`);
  const decision = decideVersion(
    requested,
    packageView.highestVersion,
    latest,
    sourceSha(),
    process.env.MCNS_SOURCE_SHA_LEGACY || "",
  );
  console.log(`${decision.action} ${decision.version}`);
}

function sourceSha() {
  return process.env.MCNS_SOURCE_SHA || "";
}

function alreadyMatches(view) {
  const published = view.meta?.sourceSha256 || "";
  const want = sourceSha();
  const legacy = process.env.MCNS_SOURCE_SHA_LEGACY || "";
  return Boolean(published) && (published === want || (Boolean(legacy) && published === legacy));
}

async function main() {
  const [pkg, requested, ...pairs] = process.argv.slice(2);
  if (!pkg || !requested || pairs.length === 0) {
    throw new Error(
      "usage: node scripts/mcns-release.mjs <package> <version> slot=path ...",
    );
  }
  assertReleaseVersion(requested);
  let version = requested;
  requireCredentials();

  const files = Object.fromEntries(
    pairs.map((pair) => {
      const index = pair.indexOf("=");
      if (index <= 0) throw new Error(`expected slot=path, got ${pair}`);
      return [pair.slice(0, index), pair.slice(index + 1)];
    }),
  );

  const packageView = await (await call("GET", `/v1/packages/${pkg}`)).json();
  const layout = packageView.layout;
  const slots = Object.keys(layout);
  const missing = slots.filter((slot) => !(slot in files));
  const extra = Object.keys(files).filter((slot) => !slots.includes(slot));
  if (missing.length || extra.length) {
    throw new Error(
      `layout mismatch, missing: ${missing.join(", ") || "-"}, extra: ${extra.join(", ") || "-"}`,
    );
  }

  const latest = await loadLatest(pkg);
  const decision = decideVersion(
    version,
    packageView.highestVersion,
    latest,
    sourceSha(),
    process.env.MCNS_SOURCE_SHA_LEGACY || "",
  );
  if (decision.action === "unchanged") {
    console.log(`unchanged ${pkg}@${decision.version}`);
    console.log(`${base}/v1/packages/${pkg}/latest`);
    return;
  }
  version = decision.version;
  console.log(`resolved ${pkg} ${version}`);

  const existing = await loadVersion(pkg, version);
  if (existing?.publishedAt) {
    if (alreadyMatches(existing)) {
      console.log(`unchanged ${pkg}@${version}`);
      console.log(`${base}/v1/packages/${pkg}/latest`);
      return;
    }
    throw new Error(
      `${pkg}@${version} is already published with different sources. The next PATCH is ${nextPatch(version)}.`,
    );
  }
  if (
    existing?.meta?.sourceSha256 &&
    sourceSha() &&
    existing.meta.sourceSha256 !== sourceSha()
  ) {
    throw new Error(
      `${pkg}@${version} is a draft for different sources. The next PATCH is ${nextPatch(version)}.`,
    );
  }

  const description = [
    process.env.MCNS_MOD_VERSION ? `mod ${process.env.MCNS_MOD_VERSION}` : null,
    process.env.MCNS_COMMIT ? process.env.MCNS_COMMIT.slice(0, 12) : null,
  ]
    .filter(Boolean)
    .join(" ");
  const meta = {
    modVersion: version,
    sourceSha256: sourceSha() || null,
    commit: process.env.MCNS_COMMIT || null,
    ref: process.env.MCNS_REF || null,
    files: Object.fromEntries(
      Object.entries(files).map(([slot, path]) => [slot, basename(path)]),
    ),
  };

  for (const slot of slots) statSync(files[slot]);
  if (process.env.MCNS_RELEASE_CHECK === "1") {
    console.log(`pending ${pkg}@${version}`);
    return;
  }

  if (!existing) {
    try {
      await call("POST", `/v1/packages/${pkg}/versions`, {
        json: { version, description, meta },
      });
    } catch (error) {
      if (error.status !== 409) throw error;
      const raced = await loadVersion(pkg, version);
      if (raced?.publishedAt) {
        if (alreadyMatches(raced)) {
          console.log(`unchanged ${pkg}@${version}`);
          console.log(`${base}/v1/packages/${pkg}/latest`);
          return;
        }
        throw new Error(
          `${pkg}@${version} is already published with different sources. The next PATCH is ${nextPatch(version)}.`,
        );
      }
      if (
        raced?.meta?.sourceSha256 &&
        sourceSha() &&
        raced.meta.sourceSha256 !== sourceSha()
      ) {
        throw new Error(
          `${pkg}@${version} is a draft for different sources. The next PATCH is ${nextPatch(version)}.`,
        );
      }
      // A draft is often invisible to GET, while create answers 409.
      // The upload below distinguishes an open draft from a burned number.
      const detail = String(error.body || error.message || "")
        .replace(/\s+/g, " ")
        .trim()
        .slice(0, 240);
      if (/must be greater|not higher|package floor/i.test(detail)) {
        throw new Error(
          `${pkg}@${version} is not above the package floor. The next PATCH is ${nextPatch(version)}. ${detail}`,
        );
      }
      console.log(`resuming draft ${pkg}@${version}: ${detail}`);
    }
  }

  try {
    for (const slot of slots) {
      const filePath = files[slot];
      const size = statSync(filePath).size;
      const fileHash = token ? "" : await hashFile(filePath);
      const target = `/v1/packages/${pkg}/versions/${version}/artifacts/${slot}`;
      await withGatewayRetry(`upload ${pkg}@${version} ${slot}`, async (attempt) => {
        console.log(`uploading ${pkg}@${version} ${slot} ${size} bytes, attempt ${attempt}`);
        await upload(target, filePath, {
          ...artifactSignature(pkg, version, slot, fileHash, size),
          authorization: authorize("PUT", target, sha("")),
        });
      });
    }

    await withGatewayRetry(`publish ${pkg}@${version}`, async () => {
      await call("POST", `/v1/packages/${pkg}/versions/${version}/publish`);
    });
    console.log(`published ${pkg}@${version}`);
    console.log(`${base}/v1/packages/${pkg}/latest`);
  } catch (error) {
    if (error.status === 404) {
      throw new Error(
        `${pkg}@${version} is not an open draft, and that number is already the package floor. The next PATCH is ${nextPatch(version)}.`,
      );
    }
    console.error(
      `draft ${pkg}@${version} was left unpublished. Rerun the same SemVer after fixing the upload.`,
    );
    throw error;
  }
}

function isDirectRun() {
  const entry = process.argv[1];
  if (!entry) return false;
  return realpathSync(entry) === realpathSync(fileURLToPath(import.meta.url));
}

if (isDirectRun()) {
  if (process.argv[2] === "--check-semver") {
    for (const version of process.argv.slice(3)) {
      assertReleaseVersion(version);
      console.log(version);
    }
  } else if (process.argv[2] === "--next-version") {
    printNextVersion().catch((error) => {
      console.error(error.message);
      process.exit(1);
    });
  } else {
    main().catch((error) => {
      console.error(error.message);
      if (error.cause) console.error(error.cause.code || error.cause.message || error.cause);
      process.exit(1);
    });
  }
}
