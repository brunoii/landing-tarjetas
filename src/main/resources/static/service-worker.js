const SHELL_CACHE_NAME = "privacy-safe-shell-v2";
const OFFLINE_DOCUMENT_URL = "/offline.html";
const CACHEABLE_SHELL_URLS = new Set([
    "/",
    "/index.html",
    OFFLINE_DOCUMENT_URL,
    "/manifest.webmanifest",
    "/icons/icon-192.svg",
    "/icons/icon-512.svg",
    "/css/styles.css?v=20260730-app-shell-domain-navigation-ui",
    "/js/app.js?v=20260730-app-shell-domain-navigation-ui",
    "/js/api.js?v=20260727-mobile-scanner-ocr-pwa-foundation-api",
    "/js/api.js?v=20260713-pending-main",
    "/js/categories.js",
    "/js/dashboard.js?v=20260713-pending-main",
    "/js/incomes.js?v=20260713-pending-main",
    "/js/manual-expenses.js?v=20260713-pending-main",
    "/js/navigation.js?v=20260730-app-shell-domain-navigation-ui",
    "/js/simulator.js?v=20260713-pending-main",
    "/js/statements.js?v=20260713-pending-main",
    "/js/supermarket.js?v=20260730-app-shell-domain-navigation-ui",
    "/js/transactions.js?v=20260713-pending-main",
    "/js/utils.js"
]);
const NETWORK_ONLY_PATTERNS = [
    (url) => url.pathname.startsWith("/api/"),
    (url) => url.pathname === "/login",
    (url) => url.pathname === "/logout",
    (url) => url.pathname.startsWith("/auth"),
    (url) => url.pathname.includes("ticket"),
    (url) => url.pathname.includes("upload"),
    (url) => url.pathname.includes("private"),
    (url) => url.pathname.includes("scan-session"),
    (url) => url.pathname.includes("barcode-alias"),
    (url) => url.pathname.endsWith(".pdf")
];

self.addEventListener("install", (event) => {
    event.waitUntil((async () => {
        const cache = await caches.open(SHELL_CACHE_NAME);
        const offlineResponse = await fetch(OFFLINE_DOCUMENT_URL);
        if (offlineResponse.ok) {
            await cache.put(OFFLINE_DOCUMENT_URL, offlineResponse.clone());
        }
        await self.skipWaiting();
    })());
});

self.addEventListener("activate", (event) => {
    event.waitUntil((async () => {
        const cacheNames = await caches.keys();
        await Promise.all(cacheNames.filter((cacheName) => cacheName !== SHELL_CACHE_NAME).map((cacheName) => caches.delete(cacheName)));
        await self.clients.claim();
    })());
});

self.addEventListener("fetch", (event) => {
    const request = event.request;
    if (request.method !== "GET") {
        return;
    }
    const url = new URL(request.url);
    if (url.origin !== self.location.origin) {
        return;
    }
    if (NETWORK_ONLY_PATTERNS.some((matches) => matches(url))) {
        event.respondWith(fetch(request));
        return;
    }
    const cacheKey = toCacheKey(url);
    if (!CACHEABLE_SHELL_URLS.has(cacheKey)) {
        event.respondWith(handleUnmatchedRequest(request));
        return;
    }
    event.respondWith(handleAllowlistedRequest(request, cacheKey));
});

async function handleAllowlistedRequest(request, cacheKey) {
    const cache = await caches.open(SHELL_CACHE_NAME);
    try {
        const networkResponse = await fetch(request);
        if (networkResponse.ok) {
            await cache.put(cacheKey, networkResponse.clone());
        }
        return networkResponse;
    } catch (error) {
        const cachedResponse = await cache.match(cacheKey);
        if (cachedResponse) {
            return cachedResponse;
        }
        if (isNavigationRequest(request)) {
            return (await cache.match(OFFLINE_DOCUMENT_URL)) || new Response("Offline", { status: 503 });
        }
        throw error;
    }
}

async function handleUnmatchedRequest(request) {
    try {
        return await fetch(request);
    } catch (error) {
        if (!isNavigationRequest(request)) {
            throw error;
        }
        const cache = await caches.open(SHELL_CACHE_NAME);
        return (await cache.match(OFFLINE_DOCUMENT_URL)) || new Response("Offline", { status: 503 });
    }
}

function isNavigationRequest(request) {
    return request.mode === "navigate" || String(request.headers.get("accept") || "").includes("text/html");
}

function toCacheKey(url) {
    return `${url.pathname}${url.search}` || "/";
}
