import L from 'leaflet';
import './index.css';

// Application State
const state = {
  tab: 'twin',
  lang: 'hi', // 'hi' | 'en'
  userLocation: null,
  twin: null,
  selectedZone: null,
  map: null,
  mapLayers: {
    satellite: null,
    street: null,
    zones: null,
    boundary: null,
    user: null
  },
  isDrawing: false,
  drawPoints: [],
  llmModel: 'llama3:8b',
  llmHealth: { status: 'checking', models: [] },
  chatMessages: [
    { sender: 'ai', text: 'नमस्कार किसान साथी! 🙏 मैं कृषिमित्र AI सहायक हूँ। बड़े LLM मॉडल (Llama 3 8B) और ICAR ज्ञानकोश द्वारा संचालित। आप फसल, कीट, खाद, डिजिटल ट्विन या योजनाओं के बारे में कुछ भी पूछ सकते हैं।' }
  ],
  weather: null,
  crops: [],
  mandi: [],
  schemes: [],
  rental: []
};

const API_BASE = '/api/v1';

// Translations
const i18n = {
  hi: {
    brandSubtitle: 'इंटेलिजेंट कृषि प्लेटफॉर्म व डिजिटल ट्विन',
    navTwin: '🌾 डिजिटल खेत ट्विन',
    navChat: '🤖 AI कृषि साथी (Ollama)',
    navScanner: '🍃 पत्ती रोग स्कैनर',
    navWeather: '⛅ मौसम व छिड़काव',
    navCrops: '🌱 फसल मार्गदर्शिका (ICAR)',
    navMandi: '📈 मंडी भाव व ट्रेंड',
    navSchemes: '💰 सरकारी योजनाएं व लोन',
    navRental: '🚜 कृषि यंत्र किराया',
    fieldTitle: 'मेरा गेहूं का खेत (My Wheat Field)',
    fieldSubtitle: '5.2 एकड़ • गेहूं (HD-2967 पूसा) • 9 प्रबंधन ज़ोन',
    sourceTag: 'डेटा स्रोत: प्रदर्शन डेटा (SIH प्रोटोटाइप)',
    btnMyGps: '📍 मेरा GPS स्थान',
    btnDrawField: '📐 नई मेड़ बनाएं',
    btnMarkIrrigated: '✓ सिंचाई दर्ज करें (35mm)',
    btnAskAi: 'इस ज़ोन पर AI से सलाह लें',
    alertWaterStress: '⚠️ पानी की कमी की चेतावनी: ज़ोन 7 में नमी घटकर 18% रह गई है!',
    sendPlaceholder: 'फसल, खाद या रोग से जुड़ा कोई भी प्रश्न पूछें...',
    modelLabel: 'सक्रिय LLM मॉडल:'
  },
  en: {
    brandSubtitle: 'Intelligent Farming & Field Twin',
    navTwin: '🌾 Field Digital Twin',
    navChat: '🤖 AI Assistant (Ollama 8B)',
    navScanner: '🍃 Crop Leaf Scanner',
    navWeather: '⛅ Weather & Spray Window',
    navCrops: '🌱 Crop Guide (ICAR)',
    navMandi: '📈 Mandi APMC Prices',
    navSchemes: '💰 Schemes & KCC Loans',
    navRental: '🚜 Farm Equipment Rental',
    fieldTitle: 'My Wheat Field',
    fieldSubtitle: '5.2 Acres • Wheat (HD-2967) • 9 Management Zones',
    sourceTag: 'Data Source: Demonstration Data (SIH Prototype)',
    btnMyGps: '📍 My GPS Location',
    btnDrawField: '📐 Draw Field Boundary',
    btnMarkIrrigated: '✓ Mark Irrigated (35mm)',
    btnAskAi: 'Ask AI About This Zone',
    alertWaterStress: '⚠️ Water Stress Alert: Zone 7 moisture dropped to 18%!',
    sendPlaceholder: 'Ask any farming, fertilizer, or crop question...',
    modelLabel: 'Active LLM Model:'
  }
};

function t(key) {
  return i18n[state.lang][key] || key;
}

// Initialization
document.addEventListener('DOMContentLoaded', async () => {
  renderAppLayout();
  setupNavigation();
  fetchLlmHealth();
  await loadTwinData();
  loadWeatherData();
});

// Render Main App Structure
function renderAppLayout() {
  const app = document.getElementById('app');
  app.innerHTML = `
    <div class="app-container">
      <!-- Left Sidebar Navigation -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <div class="logo-badge">🌾</div>
          <div class="brand-text">
            <h1>KrishiMitra</h1>
            <p id="brand-subtitle">${t('brandSubtitle')}</p>
          </div>
        </div>

        <ul class="nav-links">
          <li class="nav-item">
            <a class="nav-link ${state.tab === 'twin' ? 'active' : ''}" data-tab="twin">
              <span class="nav-icon">🌾</span>
              <span>${t('navTwin')}</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link ${state.tab === 'chat' ? 'active' : ''}" data-tab="chat">
              <span class="nav-icon">🤖</span>
              <span>${t('navChat')}</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link ${state.tab === 'scanner' ? 'active' : ''}" data-tab="scanner">
              <span class="nav-icon">🍃</span>
              <span>${t('navScanner')}</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link ${state.tab === 'weather' ? 'active' : ''}" data-tab="weather">
              <span class="nav-icon">⛅</span>
              <span>${t('navWeather')}</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link ${state.tab === 'crops' ? 'active' : ''}" data-tab="crops">
              <span class="nav-icon">🌱</span>
              <span>${t('navCrops')}</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link ${state.tab === 'mandi' ? 'active' : ''}" data-tab="mandi">
              <span class="nav-icon">📈</span>
              <span>${t('navMandi')}</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link ${state.tab === 'schemes' ? 'active' : ''}" data-tab="schemes">
              <span class="nav-icon">💰</span>
              <span>${t('navSchemes')}</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link ${state.tab === 'rental' ? 'active' : ''}" data-tab="rental">
              <span class="nav-icon">🚜</span>
              <span>${t('navRental')}</span>
            </a>
          </li>
        </ul>

        <div class="sidebar-footer">
          <div style="font-size: 0.78rem; color: var(--text-muted);">
            <div><strong>Ollama Engine:</strong> <span id="sidebar-llm-status">Connecting...</span></div>
            <div style="margin-top: 4px;"><strong>SIH Edition:</strong> v2.0.0 Web</div>
          </div>
        </div>
      </aside>

      <!-- Main Content Area -->
      <div class="main-wrapper">
        <header class="topbar">
          <div class="topbar-title" id="page-title">
            🌾 ${t('navTwin')}
          </div>

          <div class="topbar-actions">
            <!-- Language Toggle Switcher -->
            <div class="lang-toggle">
              <button class="lang-btn ${state.lang === 'hi' ? 'active' : ''}" id="lang-hi">हिंदी</button>
              <button class="lang-btn ${state.lang === 'en' ? 'active' : ''}" id="lang-en">English</button>
            </div>

            <!-- Provenance Badge -->
            <span class="badge badge-info">${t('sourceTag')}</span>
          </div>
        </header>

        <main class="content-area" id="content-area">
          <!-- Dynamic Content rendered by tab -->
        </main>
      </div>
    </div>
  `;

  // Language Event Handlers
  document.getElementById('lang-hi').addEventListener('click', () => switchLanguage('hi'));
  document.getElementById('lang-en').addEventListener('click', () => switchLanguage('en'));
}

function switchLanguage(newLang) {
  if (state.lang === newLang) return;
  state.lang = newLang;
  renderAppLayout();
  setupNavigation();
  renderTabContent();
}

function setupNavigation() {
  document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const tab = link.getAttribute('data-tab');
      if (tab) {
        state.tab = tab;
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        link.classList.add('active');
        document.getElementById('page-title').innerHTML = `${link.querySelector('.nav-icon').innerText} ${link.querySelector('span:last-child').innerText}`;
        renderTabContent();
      }
    });
  });
  renderTabContent();
}

// Render Content Based on Active Tab
function renderTabContent() {
  const container = document.getElementById('content-area');
  if (!container) return;

  switch (state.tab) {
    case 'twin':
      renderTwinTab(container);
      break;
    case 'chat':
      renderChatTab(container);
      break;
    case 'scanner':
      renderScannerTab(container);
      break;
    case 'weather':
      renderWeatherTab(container);
      break;
    case 'crops':
      renderCropsTab(container);
      break;
    case 'mandi':
      renderMandiTab(container);
      break;
    case 'schemes':
      renderSchemesTab(container);
      break;
    case 'rental':
      renderRentalTab(container);
      break;
  }
}

// ----------------------------------------------------
// 1. FIELD DIGITAL TWIN TAB (Leaflet Satellite + 9 Zones)
// ----------------------------------------------------
function renderTwinTab(container) {
  const summary = state.twin?.summary || { avg_moisture: 24, avg_ndvi: 0.61, attention_zones_count: 2 };
  const criticalZone = state.twin?.zones?.find(z => z.id === 'Z-07');
  const isStressed = criticalZone && criticalZone.moisture < 20;

  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 1.5rem;">
      <!-- Field Hero Card -->
      <div class="card" style="background: linear-gradient(135deg, #ffffff 0%, #f1f8e9 100%);">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
          <div>
            <h2 style="font-size: 1.35rem; font-weight: 800; color: var(--primary);">${t('fieldTitle')}</h2>
            <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 2px;">${t('fieldSubtitle')}</p>
          </div>
          <div style="display: flex; gap: 0.65rem; align-items: center;">
            <button class="btn btn-secondary btn-sm" id="btn-gps">📍 ${t('btnMyGps')}</button>
            <button class="btn btn-secondary btn-sm" id="btn-reset-demo">🔄 Reset Demo</button>
            <span class="badge badge-success">● 9 Zones Active</span>
          </div>
        </div>

        ${summary.last_action ? `
          <div style="margin-top: 1rem; padding: 0.65rem 1rem; background: #e0f2fe; border-radius: var(--radius-sm); font-size: 0.85rem; color: #0369a1; font-weight: 600;">
            ⚡ ${summary.last_action}
          </div>
        ` : ''}
      </div>

      <!-- Critical Alert Banner if Zone 7 is stressed -->
      ${isStressed ? `
        <div class="card" style="background: #fee2e2; border-color: #fca5a5; display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 0.75rem;">
            <span style="font-size: 1.75rem;">⚠️</span>
            <div>
              <strong style="color: #991b1b; font-size: 0.98rem;">${t('alertWaterStress')}</strong>
              <div style="font-size: 0.84rem; color: #7f1d1d;">ज़ोन 7 में 36°C तापमान व 2 मिमी वर्षा पर मिट्टी की नमी 18% तक गिर गई है।</div>
            </div>
          </div>
          <button class="btn btn-primary btn-sm" id="btn-inspect-z7">ज़ोन 7 देखें व सिंचाई करें →</button>
        </div>
      ` : `
        <div class="card" style="background: #dcfce7; border-color: #86efac;">
          <span style="color: #166534; font-weight: 700;">✓ सभी 9 प्रबंधन ज़ोन सामान्य स्थिति में हैं (All Zones Healthy)</span>
        </div>
      `}

      <!-- Summary Metrics Bar -->
      <div class="grid-4">
        <div class="card" style="text-align: center;">
          <div style="font-size: 1.5rem;">🌱</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">फसल (Crop)</div>
          <div style="font-weight: 800; font-size: 1.1rem; color: var(--primary);">गेहूं (Wheat)</div>
        </div>
        <div class="card" style="text-align: center;">
          <div style="font-size: 1.5rem;">💧</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">औसत नमी (Avg Moisture)</div>
          <div style="font-weight: 800; font-size: 1.1rem; color: var(--info);">${summary.avg_moisture}%</div>
        </div>
        <div class="card" style="text-align: center;">
          <div style="font-size: 1.5rem;">🌿</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">औसत NDVI</div>
          <div style="font-weight: 800; font-size: 1.1rem; color: var(--success);">${summary.avg_ndvi}</div>
        </div>
        <div class="card" style="text-align: center;">
          <div style="font-size: 1.5rem;">⚠️</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">तनावग्रस्त ज़ोन (At Risk)</div>
          <div style="font-weight: 800; font-size: 1.1rem; color: ${summary.attention_zones_count > 0 ? 'var(--danger)' : 'var(--success)'};">${summary.attention_zones_count} ज़ोन</div>
        </div>
      </div>

      <!-- Main Map and Zone Detail Side-by-Side -->
      <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 1.5rem;">
        <!-- Leaflet Satellite Map -->
        <div class="map-container">
          <div id="leaflet-map"></div>
        </div>

        <!-- Selected Zone Detail Inspector -->
        <div class="card" id="zone-inspector" style="display: flex; flex-direction: column; justify-content: space-between;">
          <div id="zone-inspector-content">
            <h3 style="font-size: 1.1rem; font-weight: 800; color: var(--primary); margin-bottom: 0.5rem;">ज़ोन स्थिति निरीक्षक</h3>
            <p style="font-size: 0.85rem; color: var(--text-muted);">मानचित्र पर किसी भी ज़ोन पर क्लिक करके उसका लाइव डेटा व समय-श्रृंखला देखें।</p>
          </div>
          
          <div style="margin-top: 1rem; border-top: 1px solid var(--border-glass); padding-top: 1rem;" id="zone-actions-container">
            <!-- Action buttons populated on zone click -->
          </div>
        </div>
      </div>
    </div>
  `;

  // Initialize Leaflet Map after DOM insertion
  setTimeout(() => {
    initLeafletMap();
    if (criticalZone) {
      inspectZone(criticalZone);
    }
  }, 100);

  // Bind GPS Button
  document.getElementById('btn-gps')?.addEventListener('click', handleUserGps);
  document.getElementById('btn-reset-demo')?.addEventListener('click', async () => {
    await fetch(`${API_BASE}/twin/reset`, { method: 'POST' });
    await loadTwinData();
    renderTabContent();
  });
  document.getElementById('btn-inspect-z7')?.addEventListener('click', () => {
    const z7 = state.twin?.zones?.find(z => z.id === 'Z-07');
    if (z7) inspectZone(z7);
  });
}

function initLeafletMap() {
  const mapElement = document.getElementById('leaflet-map');
  if (!mapElement) return;

  if (state.map) {
    state.map.remove();
  }

  const centerLat = 28.70275;
  const centerLon = 77.1030;

  const map = L.map('leaflet-map', {
    zoomControl: true
  }).setView([centerLat, centerLon], 16);

  // Esri World Imagery Satellite Tiles
  const satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
    maxZoom: 19,
    attribution: 'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS'
  }).addTo(map);

  const zonesLayer = L.layerGroup().addTo(map);
  const userLayer = L.layerGroup().addTo(map);

  state.map = map;
  state.mapLayers.satellite = satellite;
  state.mapLayers.zones = zonesLayer;
  state.mapLayers.user = userLayer;

  // Render 9 Management Zones
  renderZonesOnMap();
}

function renderZonesOnMap() {
  if (!state.map || !state.mapLayers.zones || !state.twin?.zones) return;
  state.mapLayers.zones.clearLayers();

  state.twin.zones.forEach(z => {
    const latLngs = z.boundary.map(p => [p.latitude, p.longitude]);
    const isSelected = state.selectedZone?.id === z.id;
    
    // Translucent risk colors
    let fillColor = '#4caf50'; // Green
    if (z.crop_health === 'POOR' || z.risk_level === 'HIGH') fillColor = '#ef4444'; // Red
    else if (z.crop_health === 'MODERATE' || z.risk_level === 'MEDIUM') fillColor = '#f59e0b'; // Amber

    const polygon = L.polygon(latLngs, {
      color: isSelected ? '#ffffff' : fillColor,
      fillColor: fillColor,
      fillOpacity: isSelected ? 0.65 : 0.42,
      weight: isSelected ? 3.5 : 1.5
    }).addTo(state.mapLayers.zones);

    polygon.on('click', () => {
      inspectZone(z);
    });

    // Centroid Label Badge
    if (z.center) {
      const badgeIcon = L.divIcon({
        className: 'zone-leaflet-label',
        html: `${z.id} (${z.moisture}%)`,
        iconSize: [60, 20],
        iconAnchor: [30, 10]
      });
      const marker = L.marker([z.center.latitude, z.center.longitude], { icon: badgeIcon }).addTo(state.mapLayers.zones);
      marker.on('click', () => inspectZone(z));
    }
  });
}

function inspectZone(zone) {
  state.selectedZone = zone;
  renderZonesOnMap();

  const content = document.getElementById('zone-inspector-content');
  const actions = document.getElementById('zone-actions-container');
  if (!content || !actions) return;

  const isStressed = zone.moisture < 20;

  content.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
      <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--primary);">${zone.label_hi} (${zone.id})</h3>
      <span class="badge ${isStressed ? 'badge-danger' : 'badge-success'}">${zone.crop_health}</span>
    </div>

    <!-- Gauge Metrics -->
    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.5rem; text-align: center; margin-bottom: 1rem;">
      <div style="background: #f8fafc; padding: 0.6rem; border-radius: var(--radius-sm);">
        <div style="font-size: 0.75rem; color: var(--text-muted);">नमी (Moisture)</div>
        <div style="font-weight: 800; font-size: 1.1rem; color: ${isStressed ? 'var(--danger)' : 'var(--info)'};">${zone.moisture}%</div>
      </div>
      <div style="background: #f8fafc; padding: 0.6rem; border-radius: var(--radius-sm);">
        <div style="font-size: 0.75rem; color: var(--text-muted);">NDVI</div>
        <div style="font-weight: 800; font-size: 1.1rem; color: var(--success);">${zone.ndvi}</div>
      </div>
      <div style="background: #f8fafc; padding: 0.6rem; border-radius: var(--radius-sm);">
        <div style="font-size: 0.75rem; color: var(--text-muted);">तापमान</div>
        <div style="font-weight: 800; font-size: 1.1rem; color: var(--text-main);">${zone.temperature}°C</div>
      </div>
    </div>

    <!-- Time Series History -->
    <div style="font-size: 0.85rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-main);">समय-श्रृंखला नमी ट्रेंड (30 Days):</div>
    <div style="display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.8rem;">
      ${(zone.history || []).map(h => `
        <div style="display: flex; justify-content: space-between; padding: 4px 8px; background: #f1f5f9; border-radius: 4px;">
          <span>${h.date}:</span>
          <strong>${h.moisture}% नमी | NDVI ${h.ndvi}</strong>
        </div>
      `).join('')}
    </div>

    <!-- Agronomic Reasoning -->
    <div style="margin-top: 1rem; padding: 0.75rem; background: ${isStressed ? '#fff1f2' : '#f0fdf4'}; border-radius: var(--radius-sm); font-size: 0.82rem;">
      <strong>ICAR नियम आधारित विश्लेषण:</strong>
      <div style="margin-top: 3px; color: ${isStressed ? '#9f1239' : '#166534'};">
        ${isStressed ? 'नमी < 20% और तापमान > 34°C। मुकुट जड़ (CRI) पर जल तनाव है। तुरंत 35 मिमी सिंचाई करें।' : 'संतुलित मृदा नमी और सामान्य क्लोरोफिल स्तर। फसल का विकास सही दिशा में है।'}
      </div>
    </div>
  `;

  actions.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 0.5rem;">
      ${isStressed ? `
        <button class="btn btn-primary" id="btn-irrigate-action" style="width: 100%;">
          ${t('btnMarkIrrigated')}
        </button>
      ` : `
        <button class="btn btn-secondary" disabled style="width: 100%; opacity: 0.7;">
          ✓ ज़ोन में पर्याप्त नमी उपलब्ध है
        </button>
      `}
      <button class="btn btn-secondary" id="btn-ask-ai-zone" style="width: 100%;">
        🤖 ${t('btnAskAi')}
      </button>
    </div>
  `;

  // Bind Action Buttons
  document.getElementById('btn-irrigate-action')?.addEventListener('click', async () => {
    const res = await fetch(`${API_BASE}/twin/irrigation`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ zone_id: zone.id, amount_mm: 35.0, notes: 'Delivered 35mm irrigation' })
    });
    const data = await res.json();
    alert(data.message || 'Irrigation recorded!');
    await loadTwinData();
    renderTabContent();
  });

  document.getElementById('btn-ask-ai-zone')?.addEventListener('click', () => {
    state.tab = 'chat';
    renderAppLayout();
    setupNavigation();
    sendChatMessage(`मेरे खेत के ज़ोन 7 में नमी ${zone.moisture}% और तापमान ${zone.temperature}°C है। मुझे इस समय क्या करना चाहिए?`, zone);
  });
}

function handleUserGps() {
  if (!navigator.geolocation) {
    alert('Geolocation is not supported by your browser');
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = pos.coords.latitude;
      const lon = pos.coords.longitude;
      state.userLocation = { latitude: lat, longitude: lon };

      if (state.map) {
        state.map.setView([lat, lon], 17);
        state.mapLayers.user.clearLayers();

        const userIcon = L.divIcon({
          className: 'user-gps-pin',
          iconSize: [18, 18],
          iconAnchor: [9, 9]
        });
        L.marker([lat, lon], { icon: userIcon }).addTo(state.mapLayers.user);
        L.circle([lat, lon], { radius: 30, color: '#0284c7', fillColor: '#38bdf8', fillOpacity: 0.2 }).addTo(state.mapLayers.user);
      }
      alert(`✓ GPS स्थान मिला: ${lat.toFixed(5)}, ${lon.toFixed(5)}`);
    },
    (err) => {
      alert('Could not acquire GPS: ' + err.message);
    }
  );
}

// ----------------------------------------------------
// 2. AI ASSISTANT TAB (Ollama 8B+ Streaming)
// ----------------------------------------------------
function renderChatTab(container) {
  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 1rem; max-width: 900px; margin: 0 auto;">
      <div class="card" style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 style="font-size: 1.25rem; font-weight: 800; color: var(--primary);">${t('navChat')}</h2>
          <p style="font-size: 0.85rem; color: var(--text-muted);">बड़े LLM मॉडलों (Llama 3 8B, Qwen 2.5) व ICAR कृषि मैनुअल से प्रामाणिक उत्तर</p>
        </div>
        <div style="display: flex; align-items: center; gap: 0.65rem;">
          <span style="font-size: 0.85rem; font-weight: 700;">${t('modelLabel')}</span>
          <select id="select-llm-model" style="padding: 6px 12px; border-radius: var(--radius-sm); border: 1px solid #cbd5e1; font-weight: 600;">
            <option value="llama3:8b" ${state.llmModel === 'llama3:8b' ? 'selected' : ''}>Llama 3 (8B — 9 GB)</option>
            <option value="qwen2.5:7b" ${state.llmModel === 'qwen2.5:7b' ? 'selected' : ''}>Qwen 2.5 (7B)</option>
            <option value="mistral" ${state.llmModel === 'mistral' ? 'selected' : ''}>Mistral (7B)</option>
          </select>
        </div>
      </div>

      <!-- Quick Suggestion Chips -->
      <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
        <button class="btn btn-secondary btn-sm chip-btn" data-query="गेहूं में पहली सिंचाई और यूरिया की मात्रा कितनी रखनी चाहिए?">🌾 गेहूं में पहली सिंचाई</button>
        <button class="btn btn-secondary btn-sm chip-btn" data-query="पीला रतुआ के लक्षण और तुरंत रोकथाम का जैविक व रासायनिक उपाय?">🍃 पीला रतुआ का इलाज</button>
        <button class="btn btn-secondary btn-sm chip-btn" data-query="KCC किसान क्रेडिट कार्ड पर 3 लाख तक 4% ब्याज दर पर लोन कैसे लें?">💰 KCC लोन पात्रता</button>
      </div>

      <!-- Chat Window -->
      <div class="chat-window">
        <div class="chat-messages" id="chat-messages">
          ${state.chatMessages.map(m => `
            <div class="chat-bubble ${m.sender === 'user' ? 'chat-user' : 'chat-ai'}">
              ${m.text.replace(/\n/g, '<br/>')}
            </div>
          `).join('')}
        </div>

        <form class="chat-input-bar" id="chat-form">
          <input type="text" class="chat-input" id="chat-input" placeholder="${t('sendPlaceholder')}" autocomplete="off" />
          <button type="submit" class="btn btn-primary">भेजें (Send)</button>
        </form>
      </div>
    </div>
  `;

  // Bind Form
  const form = document.getElementById('chat-form');
  const input = document.getElementById('chat-input');
  form?.addEventListener('submit', (e) => {
    e.preventDefault();
    const val = input.value.trim();
    if (val) {
      input.value = '';
      sendChatMessage(val);
    }
  });

  // Model Selector
  document.getElementById('select-llm-model')?.addEventListener('change', (e) => {
    state.llmModel = e.target.value;
  });

  // Suggestion Chips
  document.querySelectorAll('.chip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const q = btn.getAttribute('data-query');
      if (q) sendChatMessage(q);
    });
  });
}

async function sendChatMessage(prompt, zoneContext = null) {
  state.chatMessages.push({ sender: 'user', text: prompt });
  const aiMsgIndex = state.chatMessages.length;
  state.chatMessages.push({ sender: 'ai', text: '...' });
  renderTabContent();

  const msgContainer = document.getElementById('chat-messages');
  if (msgContainer) msgContainer.scrollTop = msgContainer.scrollHeight;

  try {
    const res = await fetch(`${API_BASE}/ai/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        prompt: prompt,
        model: state.llmModel,
        zone_context: zoneContext
      })
    });

    const reader = res.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let accumulated = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value);
      const lines = chunk.split('\n');

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const token = line.replace('data: ', '');
          if (token === '[DONE]') break;
          accumulated += token;
          state.chatMessages[aiMsgIndex].text = accumulated;
          
          // Re-render message bubble in-place for 60fps typing effect
          if (msgContainer) {
            const bubbles = msgContainer.querySelectorAll('.chat-bubble');
            if (bubbles[aiMsgIndex]) {
              bubbles[aiMsgIndex].innerHTML = accumulated.replace(/\n/g, '<br/>');
              msgContainer.scrollTop = msgContainer.scrollHeight;
            }
          }
        }
      }
    }
  } catch (err) {
    const p = prompt.toLowerCase();
    let reply = '';
    if (p.includes('पानी') || p.includes('सिंचाई') || p.includes('water') || p.includes('irrigation')) {
      reply = `🌾 **गेहूं व रबी फसलों में सिंचाई प्रबंधन (ICAR दिशानिर्देश):**\n\n1. **पहली सिंचाई (CRI अवस्था):** बुवाई के 20-25 दिन बाद मुकुट जड़ बनते समय 35 मिमी हल्की सिंचाई अनिवार्य है।\n2. **दूसरी सिंचाई (कल्ले फूटते समय):** 40-45 दिन बाद।\n3. **तीसरी सिंचाई (गांठ बनने पर):** 60-65 दिन बाद।\n4. **फूल व दाना भरते समय:** 80-85 व 100-105 दिन बाद हल्की सिंचाई करें।\n\n💡 *सावधानी: तेज धूप में दोपहर के बजाय शाम के समय पानी लगाएं ताकि वाष्पीकरण कम हो।*`;
    } else if (p.includes('रतुआ') || p.includes('पीला') || p.includes('rust') || p.includes('रोग') || p.includes('disease')) {
      reply = `🍃 **पीला रतुआ (Yellow Rust) व कवक रोग नियंत्रण:**\n\n• **रासायनिक उपचार:** प्रोपिकोनाजोल 25% EC (टिल्ट / Tilt) 1 मिली प्रति लीटर पानी (200 मिली/एकड़) या टेबुकोनाजोल 1.25 मिली/लीटर का छिड़काव करें।\n• **जैविक रोकथाम:** 5% नीम तेल (10,000 PPM) या ट्राइकोडर्मा विरिडी 5 ग्राम प्रति लीटर पानी का स्प्रे करें।\n• **सावधानी:** रोगग्रस्त खेत में अत्यधिक यूरिया का प्रयोग न करें।`;
    } else if (p.includes('खाद') || p.includes('यूरिया') || p.includes('fertilizer') || p.includes('dap')) {
      reply = `🧪 **संतुलित उर्वरक अनुपात (NPK 120:60:40 किग्रा/हेक्टेयर):**\n\n• **बुवाई के समय:** DAP 50 किग्रा (1 बोरी) + पोटाश (MOP) 20-25 किग्रा + जिंक सल्फेट 5 किग्रा प्रति एकड़।\n• **प्रथम सिंचाई पर:** यूरिया 40-45 किग्रा/एकड़।\n• **द्वितीय सिंचाई पर:** यूरिया 40-45 किग्रा/एकड़।\n• **दाना भराव टिप:** बालियां निकलते समय NPK 0:52:34 का 1 किग्रा/100 लीटर पानी में स्प्रे करें।`;
    } else if (p.includes('kcc') || p.includes('लोन') || p.includes('loan') || p.includes('योजना') || p.includes('pm kisan')) {
      reply = `💰 **किसान क्रेडिट कार्ड (KCC) व सरकारी योजनाएं:**\n\n• **KCC फसल ऋण:** ₹1.60 लाख तक बिना किसी बंधक (Collateral) के, प्रभावी ब्याज दर मात्र 4% वार्षिक।\n• **PM-KISAN:** ₹6,000 प्रतिवर्ष 3 किस्तों में सीधे बैंक खाते में। पोर्टल: pmkisan.gov.in।\n• **PMFBY:** रबी फसलों पर मात्र 1.5% प्रीमियम देकर फसल बीमा सुरक्षा पाएं।`;
    } else {
      reply = `🌾 **कृषिमित्र विशेषज्ञ कृषि परामर्श:**\n\nआपके प्रश्न **"${prompt}"** के संबंध में ICAR प्रमाणित मार्गदर्शन:\n\n1. **मृदा व पोषण:** मिट्टी की जांच के आधार पर ही पोषक तत्व दें और गोबर की सड़ी खाद से जीवांश कार्बन बढ़ाएं।\n2. **जल संरक्षण:** मौसम साफ रहने पर आवश्यकतानुसार हल्की सिंचाई करें। आगामी 48 घंटों में वर्षा की संभावना होने पर पानी न लगाएं।\n3. **कीट प्रबंधन:** रासायनिक दवाओं के बजाय पहले नीम तेल व फेरोमोन ट्रैप का प्रयोग करें।\n\n*(नोट: बैकएंड सर्वर पोर्ट 8000 पर पुनः संपर्क स्थापित किया जा रहा है)*`;
    }
    state.chatMessages[aiMsgIndex].text = reply;
    renderTabContent();
  }
}

// ----------------------------------------------------
// 3. LEAF DISEASE SCANNER TAB
// ----------------------------------------------------
function renderScannerTab(container) {
  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 1.5rem; max-width: 850px; margin: 0 auto;">
      <div class="card">
        <h2 style="font-size: 1.35rem; font-weight: 800; color: var(--primary);">🍃 AI पत्ती रोग निदान (Leaf Disease Diagnosis)</h2>
        <p style="font-size: 0.9rem; color: var(--text-muted); margin-top: 2px;">
          प्रभावित फसल की पत्ती की तस्वीर अपलोड करें अथवा नीचे दिए गए त्वरित नमूनों पर क्लिक करें।
        </p>

        <div style="margin-top: 1.25rem; display: flex; gap: 0.65rem; flex-wrap: wrap;">
          <button class="btn btn-secondary btn-sm test-leaf-btn" data-type="yellow_rust">🌾 पीला रतुआ (Yellow Rust)</button>
          <button class="btn btn-secondary btn-sm test-leaf-btn" data-type="leaf_blight">🍅 पत्ती झुलसा (Leaf Blight)</button>
          <button class="btn btn-secondary btn-sm test-leaf-btn" data-type="healthy">🌿 स्वस्थ पत्ती (Healthy Leaf)</button>
        </div>
      </div>

      <div class="card" id="diagnosis-result-box" style="display: none;">
        <!-- Filled on diagnosis -->
      </div>
    </div>
  `;

  document.querySelectorAll('.test-leaf-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const type = btn.getAttribute('data-type');
      runDiagnosis(type);
    });
  });

  // Run default yellow rust
  runDiagnosis('yellow_rust');
}

async function runDiagnosis(demoType) {
  const resultBox = document.getElementById('diagnosis-result-box');
  if (!resultBox) return;

  resultBox.style.display = 'block';
  resultBox.innerHTML = `<div style="text-align:center; padding: 2rem;">AI मॉडल पत्ती की जांच कर रहा है... ⏳</div>`;

  const formData = new FormData();
  formData.append('demo_type', demoType);

  const res = await fetch(`${API_BASE}/disease/diagnose`, {
    method: 'POST',
    body: formData
  });
  const data = await res.json();

  resultBox.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
      <div>
        <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--primary);">${data.disease_name_hi}</h3>
        <div style="font-size: 0.85rem; color: var(--text-muted);">${data.disease_name} • ${data.crop}</div>
      </div>
      <span class="badge badge-success">विश्वास स्तर: ${(data.confidence * 100).toFixed(0)}%</span>
    </div>

    <div style="display: flex; flex-direction: column; gap: 0.75rem; font-size: 0.92rem;">
      <div style="background: #f8fafc; padding: 0.75rem; border-radius: var(--radius-sm);">
        <strong style="color: var(--text-main);">लक्षण (Symptoms):</strong>
        <p style="color: var(--text-muted); margin-top: 2px;">${data.symptoms_hi}</p>
      </div>

      <div style="background: #f0fdf4; padding: 0.75rem; border-radius: var(--radius-sm);">
        <strong style="color: var(--primary);">जैविक उपचार (Organic Remedy):</strong>
        <p style="color: #166534; margin-top: 2px;">${data.organic_remedy_hi}</p>
      </div>

      <div style="background: #fffbeb; padding: 0.75rem; border-radius: var(--radius-sm);">
        <strong style="color: var(--secondary);">रासायनिक उपचार (Chemical Treatment):</strong>
        <p style="color: #b45309; margin-top: 2px;">${data.chemical_remedy_hi}</p>
      </div>

      <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 0.5rem; font-size: 0.8rem; color: var(--text-light);">
        <span>स्रोत: ${data.source}</span>
        <button class="btn btn-secondary btn-sm" id="btn-tag-scan-twin">🌾 डिजिटल ट्विन ज़ोन से जोड़ें</button>
      </div>
    </div>
  `;

  document.getElementById('btn-tag-scan-twin')?.addEventListener('click', () => {
    alert('✓ यह रोग स्कैन डिजिटल ट्विन ज़ोन 7 के इतिहास से सफलतापूर्वक जोड़ दिया गया!');
  });
}

// ----------------------------------------------------
// 4. WEATHER & SPRAY WINDOW TAB
// ----------------------------------------------------
function renderWeatherTab(container) {
  const w = state.weather || { temperature: 31.5, humidity: 62, wind_speed: 11.2, rain_prob: 15, spray_safe: true, spray_status_text: 'अनुकूल' };
  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 1.5rem; max-width: 900px; margin: 0 auto;">
      <!-- Hero Weather Bar -->
      <div class="card" style="background: var(--primary-gradient); color: #ffffff;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <div style="font-size: 0.9rem; opacity: 0.9;">हापुड़, उत्तर प्रदेश (लाइव ओपन-मेटियो मौसम)</div>
            <div style="font-size: 3rem; font-weight: 800; margin: 0.25rem 0;">${w.temperature}°C</div>
            <div style="font-size: 1rem; font-weight: 600;">साफ धूप व कृषि हेतु अनुकूल स्थिति</div>
          </div>
          <div style="text-align: right; display: flex; flex-direction: column; gap: 0.5rem;">
            <div>💧 आर्द्रता: <strong>${w.humidity}%</strong></div>
            <div>💨 हवा: <strong>${w.wind_speed} किमी/घंटा</strong></div>
            <div>🌧️ वर्षा संभावना: <strong>${w.rain_prob}%</strong></div>
          </div>
        </div>
      </div>

      <!-- Agrochemical Spray Window Card -->
      <div class="card" style="background: ${w.spray_safe ? '#f0fdf4' : '#fff1f2'}; border-color: ${w.spray_safe ? '#86efac' : '#fca5a5'};">
        <h3 style="font-size: 1.15rem; font-weight: 800; color: ${w.spray_safe ? 'var(--primary)' : 'var(--danger)'};">
          🚜 कीटनाशक छिड़काव खिड़की (Agrochemical Spray Window): ${w.spray_status_text}
        </h3>
        <p style="margin-top: 4px; font-size: 0.9rem; color: ${w.spray_safe ? '#166534' : '#9f1239'};">
          ${w.spray_reason || 'हवा की गति 15 किमी/घंटा से कम और वर्षा की संभावना न्यून होने पर कीटनाशक दवा का बहाव नहीं होता।'}
        </p>
      </div>

      <!-- Practical ICAR Advisory -->
      <div class="card">
        <h3 style="font-size: 1.1rem; font-weight: 800; color: var(--primary); margin-bottom: 0.5rem;">ICAR व्यावहारिक कृषि परामर्श</h3>
        <p style="font-size: 0.92rem; color: var(--text-main); line-height: 1.6;">
          आगामी दिनों में वर्षा का कोई अलर्ट नहीं है। रबी फसलों (गेहूं, चना, सरसों) में आवश्यकतानुसार हल्की सिंचाई करें। सिंचाई उपरांत नाइट्रोजन (यूरिया) का बुरकाव सुबह या शाम को करें।
        </p>
      </div>
    </div>
  `;
}

// ----------------------------------------------------
// 5. CROP GUIDE (49 ICAR CROPS)
// ----------------------------------------------------
async function renderCropsTab(container) {
  if (!state.crops || state.crops.length === 0) {
    const res = await fetch(`${API_BASE}/crops`);
    state.crops = await res.json();
  }

  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 1.5rem;">
      <div class="card">
        <h2 style="font-size: 1.35rem; font-weight: 800; color: var(--primary);">🌱 ICAR प्रमाणित फसल मार्गदर्शिका (Crop Manuals)</h2>
        <p style="font-size: 0.9rem; color: var(--text-muted); margin-top: 2px;">
          भारतीय कृषि अनुसंधान परिषद (ICAR) द्वारा सत्यापित बुवाई, सिंचाई, किस्म व उपज विवरण।
        </p>
      </div>

      <div class="grid-3">
        ${state.crops.map(c => `
          <div class="card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
              <h3 style="font-size: 1.15rem; font-weight: 800; color: var(--primary);">${c.name_hi} (${c.name})</h3>
              <span class="badge badge-info">${c.season}</span>
            </div>
            <div style="font-size: 0.85rem; color: var(--text-muted); display: flex; flex-direction: column; gap: 0.35rem;">
              <div>⏱️ <strong>अवधि:</strong> ${c.duration}</div>
              <div>💧 <strong>पानी:</strong> ${c.water}</div>
              <div>🌾 <strong>औसत उपज:</strong> ${c.yield}</div>
              <div>🏔️ <strong>मिट्टी:</strong> ${c.soil}</div>
            </div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

// ----------------------------------------------------
// 6. MANDI APMC PRICES TAB
// ----------------------------------------------------
async function renderMandiTab(container) {
  if (!state.mandi || state.mandi.length === 0) {
    const res = await fetch(`${API_BASE}/mandi`);
    state.mandi = await res.json();
  }

  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 1.5rem;">
      <div class="card">
        <h2 style="font-size: 1.35rem; font-weight: 800; color: var(--primary);">📈 लाइव एपीएमसी मंडी भाव (Live APMC Mandi Rates)</h2>
        <p style="font-size: 0.9rem; color: var(--text-muted); margin-top: 2px;">निकटवर्ती मंडियों के वास्तविक मॉडल मूल्य व न्यूनतम समर्थन मूल्य (MSP) की तुलना।</p>
      </div>

      <div class="grid-3">
        ${state.mandi.map(m => `
          <div class="card">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <strong style="font-size: 1.05rem; color: var(--text-main);">${m.commodity}</strong>
              <span class="badge ${m.trend === 'up' ? 'badge-success' : 'badge-danger'}">${m.change}</span>
            </div>
            <div style="font-size: 0.85rem; color: var(--text-muted); margin: 4px 0 10px;">${m.mandi}</div>
            <div style="display: flex; justify-content: space-between; align-items: baseline; border-top: 1px solid var(--border-glass); padding-top: 8px;">
              <div>
                <span style="font-size: 0.75rem; color: var(--text-muted);">मॉडल भाव: </span>
                <strong style="font-size: 1.2rem; color: var(--primary);">₹${m.modal_price}</strong> /क्विंटल
              </div>
              ${m.msp > 0 ? `<div style="font-size: 0.75rem; color: var(--text-muted);">MSP: ₹${m.msp}</div>` : ''}
            </div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

// ----------------------------------------------------
// 7. SCHEMES & LOANS TAB
// ----------------------------------------------------
async function renderSchemesTab(container) {
  if (!state.schemes || state.schemes.length === 0) {
    const res = await fetch(`${API_BASE}/schemes`);
    state.schemes = await res.json();
  }

  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 1.5rem;">
      <div class="card">
        <h2 style="font-size: 1.35rem; font-weight: 800; color: var(--primary);">💰 प्रमाणित सरकारी योजनाएं व कृषि लोन (Schemes & Loans)</h2>
        <p style="font-size: 0.9rem; color: var(--text-muted); margin-top: 2px;">पीएम-किसान, केसीसी फसल ऋण, फसल बीमा व सौर पंप सब्सिडी।</p>
      </div>

      <div class="grid-2">
        ${state.schemes.map(s => `
          <div class="card" style="display: flex; flex-direction: column; justify-content: space-between;">
            <div>
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <h3 style="font-size: 1.15rem; font-weight: 800; color: var(--primary);">${s.name_hi}</h3>
                <span class="badge badge-info">${s.category}</span>
              </div>
              <div style="font-size: 0.88rem; color: var(--text-main); margin-bottom: 0.5rem;">
                <strong>लाभ:</strong> ${s.benefit}
              </div>
              <div style="font-size: 0.82rem; color: var(--text-muted);">
                <strong>पात्रता:</strong> ${s.eligibility}
              </div>
            </div>
            <div style="margin-top: 1rem; border-top: 1px solid var(--border-glass); padding-top: 0.75rem;">
              <a href="${s.portal_url}" target="_blank" rel="noopener noreferrer" class="btn btn-secondary btn-sm" style="width: 100%;">
                आधिकारिक पोर्टल पर जाएं ↗
              </a>
            </div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

// ----------------------------------------------------
// 8. EQUIPMENT RENTAL TAB
// ----------------------------------------------------
async function renderRentalTab(container) {
  if (!state.rental || state.rental.length === 0) {
    const res = await fetch(`${API_BASE}/rental`);
    state.rental = await res.json();
  }

  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 1.5rem;">
      <div class="card">
        <h2 style="font-size: 1.35rem; font-weight: 800; color: var(--primary);">🚜 कृषि यंत्र व मशीनरी किराया केंद्र (Equipment Rental)</h2>
        <p style="font-size: 0.9rem; color: var(--text-muted); margin-top: 2px;">निकटवर्ती किसानों से सीधे ट्रैक्टर, रोटावेटर व हार्वेस्टर किराए पर लें या दें।</p>
      </div>

      <div class="grid-3">
        ${state.rental.map(r => `
          <div class="card" style="display: flex; flex-direction: column; justify-content: space-between;">
            <div>
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <strong style="font-size: 1.1rem; color: var(--text-main);">${r.name}</strong>
                <span class="badge badge-success">उपलब्ध</span>
              </div>
              <div style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.5rem;">
                मालिक: <strong>${r.owner}</strong> (${r.distance_km} किमी दूर)
              </div>
              <div style="font-size: 1.2rem; font-weight: 800; color: var(--primary);">
                ₹${r.rate_per_hour} <span style="font-size: 0.8rem; font-weight: normal; color: var(--text-muted);">/घंटा</span>
              </div>
            </div>
            <div style="margin-top: 1rem; border-top: 1px solid var(--border-glass); padding-top: 0.75rem;">
              <button class="btn btn-primary btn-sm" style="width: 100%;" onclick="alert('✓ बुकिंग अनुरोध भेज दिया गया! मालिक आपसे शीघ्र संपर्क करेंगे।')">
                किराए पर बुक करें
              </button>
            </div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

// ----------------------------------------------------
// Data Loaders
// ----------------------------------------------------
async function loadTwinData() {
  try {
    const res = await fetch(`${API_BASE}/twin`);
    if (res.ok) {
      state.twin = await res.json();
    }
  } catch (err) {
    console.warn('Could not load twin data from backend, using demo defaults');
  }
}

async function loadWeatherData() {
  try {
    const res = await fetch(`${API_BASE}/weather`);
    if (res.ok) {
      state.weather = await res.json();
    }
  } catch (err) {
    console.warn('Weather fetch fallback');
  }
}

async function fetchLlmHealth() {
  try {
    const res = await fetch(`${API_BASE}/ai/health`);
    if (res.ok) {
      const data = await res.json();
      state.llmHealth = data;
      const el = document.getElementById('sidebar-llm-status');
      if (el) {
        if (data.status === 'connected') {
          el.innerHTML = `<span style="color: var(--success); font-weight: 700;">🟢 Online (${data.default_model})</span>`;
        } else {
          el.innerHTML = `<span style="color: var(--warning); font-weight: 700;">🟡 ICAR Mode</span>`;
        }
      }
    }
  } catch (e) {
    const el = document.getElementById('sidebar-llm-status');
    if (el) el.innerHTML = `<span style="color: var(--warning); font-weight: 700;">🟡 ICAR Mode</span>`;
  }
}
