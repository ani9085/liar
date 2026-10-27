// 앱 화면을 저장해 두어 인터넷이 끊겨도 기본 검사가 되게 한다.
const CACHE = 'liar-v1';
const SHELL = [
  '/',
  '/index.html',
  '/style.css',
  '/app.js',
  '/js/rules.js',
  '/manifest.webmanifest',
  '/icons/icon.svg',
  '/icons/icon-192.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))),
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== location.origin || url.pathname.startsWith('/api/')) return;

  // 최신 화면을 먼저 받아오고, 실패하면 저장해 둔 화면을 쓴다.
  event.respondWith(
    fetch(event.request)
      .then((res) => {
        const copy = res.clone();
        caches.open(CACHE).then((cache) => cache.put(url.pathname === '/' ? '/' : event.request, copy));
        return res;
      })
      .catch(() => caches.match(url.pathname === '/' ? '/' : event.request)),
  );
});
