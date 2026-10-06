// Service Worker for MB Nursing Home Web App
// Enables background push notifications even when the web application or browser tab is closed.

importScripts('https://www.gstatic.com/firebasejs/10.13.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.13.0/firebase-messaging-compat.js');

// Initialize Firebase within the Service Worker context
const firebaseConfig = {
  apiKey: "AIzaSyAdOGC8fGBbEe49IFqU2HDdY1aGJs5Q5Jw",
  projectId: "gen-lang-client-0110759247",
  messagingSenderId: "300073810040",
  appId: "1:300073810040:android:1cd9173356655572c1d58d"
};

try {
  if (!firebase.apps.length) {
    firebase.initializeApp(firebaseConfig);
  }
} catch (e) {
  console.warn('[SW] Firebase init in Service Worker:', e);
}

let messaging = null;
try {
  messaging = firebase.messaging();
} catch (e) {
  console.warn('[SW] Firebase messaging unsupported or not initialized:', e);
}

// 1. Firebase Background Push Message Handler
if (messaging) {
  messaging.onBackgroundMessage((payload) => {
    console.log('[SW] Received background FCM message:', payload);

    const title = payload.notification?.title || payload.data?.title || 'MB Nursing Home Alert';
    const notificationOptions = {
      body: payload.notification?.body || payload.data?.body || 'New clinical update received.',
      icon: '/icon.svg',
      badge: '/icon.svg',
      tag: 'hospital-alert-' + Date.now(),
      renotify: true,
      requireInteraction: true,
      vibrate: [200, 100, 200, 100, 300],
      data: {
        url: '/',
        timestamp: Date.now(),
        audience: payload.data?.audience || 'all',
        kind: payload.data?.kind || 'admin'
      },
      actions: [
        { action: 'open_app', title: 'Open Hospital HMS' },
        { action: 'dismiss', title: 'Dismiss' }
      ]
    };

    return self.registration.showNotification(title, notificationOptions);
  });
}

// 2. Standard Web Push API Listener fallback
self.addEventListener('push', (event) => {
  console.log('[SW] Push event received:', event);
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch (e) {
    data = { title: 'MB Nursing Home', body: event.data ? event.data.text() : 'Hospital notification' };
  }

  const title = data.title || data.notification?.title || 'MB Nursing Home Alert';
  const options = {
    body: data.body || data.notification?.body || 'New notification received.',
    icon: '/icon.svg',
    badge: '/icon.svg',
    tag: data.tag || 'hospital-push-' + Date.now(),
    vibrate: [200, 100, 200],
    data: {
      url: data.url || '/'
    }
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

// 3. Notification Click Handler
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  if (event.action === 'dismiss') {
    return;
  }

  const targetUrl = event.notification.data?.url || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // Focus existing window if open
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          return client.focus();
        }
      }
      // Or open a new window
      if (clients.openWindow) {
        return clients.openWindow(targetUrl);
      }
    })
  );
});

// 4. Lifecycle: Immediate activation
self.addEventListener('install', (event) => {
  console.log('[SW] Service Worker installing...');
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  console.log('[SW] Service Worker activated and claiming clients.');
  event.waitUntil(clients.claim());
});
