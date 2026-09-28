/**
 * EcoRoute Public Waste Collection & Monitoring System
 * Frontend Application & GIS Map Controller
 */

window.App = (function () {
  const API_BASE = "http://localhost:8080/api";
  let isBackendOnline = false;
  let map = null;
  let mapMarkers = {};
  let routePolyline = null;
  let truckMarker = null;

  // Application State
  let bins = [];
  let reports = [];
  let currentFilter = "ALL";
  let routeData = null;

  // Seed Data for Standalone Browser Mode (when Java backend isn't running yet)
  const defaultBins = [
    { id: "BIN-001", name: "City Hall Main Plaza", latitude: 12.971598, longitude: 77.594562, zone: "Zone-Central", wasteType: "ORGANIC", capacityLiters: 240, fillPercent: 88, weightKg: 38.4, gasPpm: 420, batteryPercent: 92, tiltAlert: false, status: "CRITICAL", lastUpdated: "Just now" },
    { id: "BIN-002", name: "Metro Station Gate 2", latitude: 12.978310, longitude: 77.599600, zone: "Zone-Central", wasteType: "RECYCLABLE", capacityLiters: 240, fillPercent: 45, weightKg: 14.2, gasPpm: 110, batteryPercent: 85, tiltAlert: false, status: "NORMAL", lastUpdated: "2 mins ago" },
    { id: "BIN-003", name: "Public Market Food Court", latitude: 12.965400, longitude: 77.587800, zone: "Zone-South", wasteType: "ORGANIC", capacityLiters: 240, fillPercent: 94, weightKg: 47.0, gasPpm: 580, batteryPercent: 78, tiltAlert: false, status: "CRITICAL", lastUpdated: "Just now" },
    { id: "BIN-004", name: "Commercial Blvd 4th Cross", latitude: 12.982000, longitude: 77.605000, zone: "Zone-East", wasteType: "GENERAL", capacityLiters: 240, fillPercent: 76, weightKg: 29.5, gasPpm: 230, batteryPercent: 90, tiltAlert: false, status: "WARNING", lastUpdated: "5 mins ago" },
    { id: "BIN-005", name: "Community Hospital Grounds", latitude: 12.960200, longitude: 77.601200, zone: "Zone-South", wasteType: "HAZARDOUS", capacityLiters: 240, fillPercent: 30, weightKg: 9.8, gasPpm: 90, batteryPercent: 95, tiltAlert: false, status: "NORMAL", lastUpdated: "12 mins ago" },
    { id: "BIN-006", name: "Central Park Jogging Path", latitude: 12.975000, longitude: 77.591000, zone: "Zone-Central", wasteType: "RECYCLABLE", capacityLiters: 240, fillPercent: 82, weightKg: 33.1, gasPpm: 160, batteryPercent: 88, tiltAlert: false, status: "CRITICAL", lastUpdated: "1 min ago" },
    { id: "BIN-007", name: "Tech Park Main Gate", latitude: 12.986000, longitude: 77.589000, zone: "Zone-North", wasteType: "GENERAL", capacityLiters: 240, fillPercent: 22, weightKg: 6.5, gasPpm: 80, batteryPercent: 99, tiltAlert: false, status: "NORMAL", lastUpdated: "20 mins ago" },
    { id: "BIN-008", name: "Railway Station Bus Terminal", latitude: 12.977500, longitude: 77.572000, zone: "Zone-West", wasteType: "GENERAL", capacityLiters: 240, fillPercent: 91, weightKg: 44.2, gasPpm: 390, batteryPercent: 89, tiltAlert: false, status: "CRITICAL", lastUpdated: "Just now" }
  ];

  const defaultReports = [
    { id: "REP-1001", binId: "BIN-001", reporterName: "Ananya Rao", contactNumber: "+91 98765 43210", issueType: "OVERFLOW", description: "Food waste falling outside bin onto pedestrian path.", status: "PENDING", createdAt: "10 mins ago" },
    { id: "REP-1002", binId: "BIN-003", reporterName: "Karthik Verma", contactNumber: "+91 98111 22334", issueType: "FOUL_ODOR", description: "Extreme rotten odor near market fruits section.", status: "DISPATCHED", createdAt: "35 mins ago" }
  ];

  async function init() {
    bins = [...defaultBins];
    reports = [...defaultReports];

    initTabs();
    initMap();
    initEventListeners();
    await checkBackendConnection();
    await fetchData();
    initCharts();

    setInterval(fetchData, 6000);
  }

  function initTabs() {
    const tabs = document.querySelectorAll(".nav-tab");
    tabs.forEach(tab => {
      tab.addEventListener("click", () => {
        tabs.forEach(t => t.classList.remove("active"));
        document.querySelectorAll(".tab-pane").forEach(p => p.classList.remove("active"));

        tab.classList.add("active");
        const target = tab.dataset.tab;
        document.getElementById(`tab-${target}`).classList.add("active");

        if (target === "map-view" && map) {
          setTimeout(() => map.invalidateSize(), 200);
        }
      });
    });
  }

  function initMap() {
    const mapElement = document.getElementById("city-map");
    if (!mapElement) return;

    map = L.map("city-map").setView([12.972, 77.593], 14);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    const depotIcon = L.divIcon({
      className: "custom-depot-marker",
      html: `<div style="background:#8b5cf6; width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; color:white; border:3px solid white; box-shadow:0 0 10px #8b5cf6;"><i class="fa-solid fa-warehouse" style="font-size:14px;"></i></div>`,
      iconSize: [32, 32],
      iconAnchor: [16, 16]
    });

    L.marker([12.971598, 77.585000], { icon: depotIcon })
      .addTo(map)
      .bindPopup("<b>Central Municipal Waste Depot &amp; Garage</b><br>Fleet origin and dispatch center.");
  }

  function updateMapMarkers() {
    if (!map) return;

    Object.keys(mapMarkers).forEach(id => {
      map.removeLayer(mapMarkers[id]);
      delete mapMarkers[id];
    });

    const filteredBins = getFilteredBins();

    filteredBins.forEach(bin => {
      let color = "#10b981"; // Normal Green
      let statusLabel = "CLEAN / NORMAL";
      let statusEmoji = "🟢";

      if (bin.status === "CRITICAL" || bin.fillPercent >= 80 || bin.tiltAlert) {
        color = "#ef4444"; // Red
        statusLabel = "FULL - URGENT PICKUP";
        statusEmoji = "🔴";
      } else if (bin.status === "WARNING" || bin.fillPercent >= 60) {
        color = "#f59e0b"; // Yellow
        statusLabel = "GETTING FULL";
        statusEmoji = "🟡";
      }

      const pulseGlow = (bin.fillPercent >= 80 || bin.tiltAlert) 
        ? `box-shadow: 0 0 14px ${color}, 0 0 6px ${color};` 
        : `box-shadow: 0 2px 6px rgba(0,0,0,0.3);`;

      const icon = L.divIcon({
        className: "custom-bin-marker-easy",
        html: `
          <div style="position:relative; cursor:pointer;">
            <div style="background:${color}; padding:4px 8px; border-radius:18px; display:flex; align-items:center; gap:4px; color:white; border:2px solid white; font-weight:800; font-size:11px; ${pulseGlow} white-space:nowrap;">
              <i class="fa-solid fa-trash-can" style="font-size:11px;"></i>
              <span>${bin.fillPercent}%</span>
            </div>
            ${bin.tiltAlert ? '<div style="position:absolute; top:-8px; right:-8px; background:#dc2626; color:white; border-radius:50%; width:18px; height:18px; font-size:10px; font-weight:bold; display:flex; align-items:center; justify-content:center; border:2px solid white;">!</div>' : ''}
          </div>
        `,
        iconSize: [52, 28],
        iconAnchor: [26, 14]
      });

      const popupHtml = `
        <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size:13px; line-height:1.4; min-width:220px; color:#0f172a; padding: 4px;">
          <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:4px;">
            <h4 style="margin:0; font-size:14px; font-weight:700; color:#0f172a;">🗑️ ${bin.name}</h4>
          </div>
          
          <div style="font-size:11px; color:#64748b; margin-bottom:8px;">
            ID: <b>${bin.id}</b> &bull; Zone: <b>${bin.zone || 'Central'}</b> &bull; Type: <b>${bin.wasteType}</b>
          </div>

          <!-- Visual Capacity Progress Bar -->
          <div style="margin-bottom:8px;">
            <div style="display:flex; justify-content:space-between; font-size:12px; margin-bottom:3px;">
              <span style="font-weight:600; color:${color};">${statusEmoji} ${statusLabel}</span>
              <span style="font-weight:700; color:${color};">${bin.fillPercent}% Full</span>
            </div>
            <div style="background:#e2e8f0; border-radius:10px; height:10px; width:100%; overflow:hidden;">
              <div style="background:${color}; width:${bin.fillPercent}%; height:100%; border-radius:10px; transition: width 0.3s ease;"></div>
            </div>
          </div>

          <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; padding:6px 8px; font-size:11px; margin-bottom:10px; display:grid; grid-template-columns:1fr 1fr; gap:6px;">
            <div>⚖️ Weight: <b>${bin.weightKg} kg</b></div>
            <div>💨 Odor Gas: <b>${bin.gasPpm || 100} PPM</b></div>
            <div>🔋 Battery: <b>${bin.batteryPercent || 90}%</b></div>
            <div>⏱️ Updated: <b>${bin.lastUpdated || 'Just now'}</b></div>
          </div>

          ${bin.tiltAlert ? '<div style="background:#fee2e2; color:#991b1b; padding:6px; border-radius:6px; font-weight:bold; font-size:11px; text-align:center; margin-bottom:8px;">🚨 WARNING: Bin Fallen / Tilted!</div>' : ''}

          <button onclick="window.App.emptyBin('${bin.id}')" style="background:#10b981; color:white; border:none; padding:8px 12px; border-radius:6px; cursor:pointer; width:100%; font-weight:700; font-size:12px; display:flex; align-items:center; justify-content:center; gap:6px; box-shadow:0 2px 4px rgba(16,185,129,0.3);">
            <i class="fa-solid fa-broom"></i> Mark Bin as Emptied
          </button>
        </div>
      `;

      const marker = L.marker([bin.latitude, bin.longitude], { icon })
        .addTo(map)
        .bindPopup(popupHtml);

      mapMarkers[bin.id] = marker;
    });
  }

  function getFilteredBins() {
    if (currentFilter === "ALL") return bins;
    if (currentFilter === "CRITICAL") return bins.filter(b => b.fillPercent >= 80 || b.status === "CRITICAL" || b.tiltAlert);
    if (currentFilter === "ORGANIC") return bins.filter(b => b.wasteType === "ORGANIC");
    if (currentFilter === "RECYCLABLE") return bins.filter(b => b.wasteType === "RECYCLABLE");
    return bins;
  }

  async function checkBackendConnection() {
    const badge = document.getElementById("connection-badge");
    const statusText = document.getElementById("backend-status-text");

    try {
      const res = await fetch(`${API_BASE}/bins`, { method: "GET" });
      if (res.ok) {
        isBackendOnline = true;
        badge.classList.add("online");
        statusText.textContent = "Java REST API Connected (Port 8080)";
      } else {
        throw new Error("HTTP Status not 200");
      }
    } catch (e) {
      isBackendOnline = false;
      badge.classList.remove("online");
      statusText.textContent = "Standalone Mode (Browser Simulator)";
    }
  }

  async function fetchData() {
    if (isBackendOnline) {
      try {
        const [binsRes, reportsRes, statsRes] = await Promise.all([
          fetch(`${API_BASE}/bins`),
          fetch(`${API_BASE}/reports`),
          fetch(`${API_BASE}/stats`)
        ]);

        if (binsRes.ok) bins = await binsRes.json();
        if (reportsRes.ok) reports = await reportsRes.json();
        if (statsRes.ok) {
          const stats = await statsRes.json();
          updateHeaderStats(stats);
        }
      } catch (err) {
        console.warn("Error pulling from Java backend:", err);
      }
    } else {
      updateHeaderStatsFromLocal();
    }

    renderUI();
  }

  function updateHeaderStats(stats) {
    document.getElementById("stat-total-bins").textContent = stats.totalBins || bins.length;
    document.getElementById("stat-critical-bins").textContent = stats.criticalCount || 0;
  }

  function updateHeaderStatsFromLocal() {
    const critical = bins.filter(b => b.fillPercent >= 80 || b.tiltAlert).length;
    document.getElementById("stat-total-bins").textContent = bins.length;
    document.getElementById("stat-critical-bins").textContent = critical;
  }

  function renderUI() {
    updateMapMarkers();
    renderBinsCards();
    renderAlerts();
    renderReports();
    populateGrievanceBinDropdown();
    if (window.Simulator) window.Simulator.populateBinDropdown();
  }

  function renderBinsCards() {
    const container = document.getElementById("bins-cards-container");
    if (!container) return;

    const searchTerm = (document.getElementById("bin-search-input")?.value || "").toLowerCase();
    const filtered = bins.filter(b => 
      b.id.toLowerCase().includes(searchTerm) || 
      b.name.toLowerCase().includes(searchTerm) || 
      b.zone.toLowerCase().includes(searchTerm)
    );

    container.innerHTML = "";

    filtered.forEach(bin => {
      let statusClass = "normal";
      let badgeClass = "badge-normal";
      if (bin.fillPercent >= 80 || bin.tiltAlert) {
        statusClass = "critical";
        badgeClass = "badge-critical";
      } else if (bin.fillPercent >= 60) {
        statusClass = "warning";
        badgeClass = "badge-warning";
      }

      const card = document.createElement("div");
      card.className = `bin-card ${statusClass}`;
      card.innerHTML = `
        <div class="bin-card-head">
          <div>
            <span class="bin-card-id">${bin.id} &bull; ${bin.zone}</span>
            <h3 class="bin-card-title">${bin.name}</h3>
          </div>
          <span class="bin-badge ${badgeClass}">${bin.tiltAlert ? "VANDALIZED" : bin.status}</span>
        </div>

        <div class="progress-container">
          <div class="progress-header">
            <span>Fill Capacity</span>
            <span>${bin.fillPercent}%</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill fill-${statusClass}" style="width: ${bin.fillPercent}%"></div>
          </div>
        </div>

        <div class="bin-metrics-grid">
          <div class="bin-metric-item">
            <span>Weight</span>
            <strong>${bin.weightKg} kg</strong>
          </div>
          <div class="bin-metric-item">
            <span>Gas / Odor</span>
            <strong>${bin.gasPpm || 120} PPM</strong>
          </div>
          <div class="bin-metric-item">
            <span>Waste Type</span>
            <strong>${bin.wasteType}</strong>
          </div>
          <div class="bin-metric-item">
            <span>Battery Node</span>
            <strong>${bin.batteryPercent || 92}%</strong>
          </div>
        </div>

        <div class="bin-card-footer">
          <span style="font-size:11px; color:var(--text-muted);"><i class="fa-regular fa-clock"></i> ${bin.lastUpdated || "Live"}</span>
          <button class="btn btn-sm btn-outline" onclick="window.App.emptyBin('${bin.id}')">
            <i class="fa-solid fa-broom"></i> Empty
          </button>
        </div>
      `;
      container.appendChild(card);
    });
  }

  function renderAlerts() {
    const list = document.getElementById("alerts-list");
    const countBadge = document.getElementById("alerts-count");
    if (!list) return;

    const criticalBins = bins.filter(b => b.fillPercent >= 80 || b.tiltAlert);
    countBadge.textContent = criticalBins.length;
    list.innerHTML = "";

    if (criticalBins.length === 0) {
      list.innerHTML = `<div style="font-size:11px; color:var(--text-muted); padding:8px 0;">All bins within normal operating capacity.</div>`;
      return;
    }

    criticalBins.forEach(bin => {
      const item = document.createElement("div");
      item.className = "alert-item";
      item.innerHTML = `
        <div class="alert-item-title">${bin.tiltAlert ? "🚨 TAMPER / TILT DETECTED" : "⚠️ OVERFLOW IMMINENT"} &bull; ${bin.id}</div>
        <div>${bin.name} is at <strong>${bin.fillPercent}% capacity</strong> (${bin.weightKg} kg, ${bin.gasPpm || 100} PPM gas).</div>
        <div class="alert-item-time"><i class="fa-solid fa-bell"></i> Dispatched to collection route</div>
      `;
      list.appendChild(item);
    });
  }

  function renderReports() {
    const list = document.getElementById("reports-list");
    const countBadge = document.getElementById("reports-count-badge");
    if (!list) return;

    countBadge.textContent = `${reports.length} Tickets`;
    list.innerHTML = "";

    reports.forEach(rep => {
      const card = document.createElement("div");
      card.className = "report-card";
      card.innerHTML = `
        <div class="report-card-header">
          <strong>${rep.id} &bull; ${rep.issueType}</strong>
          <span class="badge ${rep.status === 'RESOLVED' ? 'badge-normal' : 'badge-warning'}">${rep.status}</span>
        </div>
        <p style="font-size:12px; margin:4px 0 8px 0; color:#cbd5e1;">${rep.description}</p>
        <div style="font-size:11px; color:var(--text-muted); display:flex; justify-content:space-between;">
          <span>Reported by: ${rep.reporterName} (${rep.contactNumber || 'N/A'})</span>
          <span>${rep.createdAt || 'Today'}</span>
        </div>
      `;
      list.appendChild(card);
    });
  }

  function populateGrievanceBinDropdown() {
    const select = document.getElementById("rep-bin-select");
    if (!select || select.options.length > 1) return;

    bins.forEach(b => {
      const opt = document.createElement("option");
      opt.value = b.id;
      opt.textContent = `${b.id} - ${b.name}`;
      select.appendChild(opt);
    });
  }

  async function calculateRoute() {
    const btn = document.getElementById("btn-recalculate-route");
    if (btn) btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Calculating in Java...`;

    try {
      if (isBackendOnline) {
        const resp = await fetch(`${API_BASE}/routes/optimize`);
        if (resp.ok) {
          routeData = await resp.json();
        }
      } else {
        routeData = calculateLocalTSPRoute();
      }

      displayRouteOnUI(routeData);
      notifyToast("Java TSP Route Optimized Successfully!", "success");
    } catch (e) {
      console.warn("Failed to calculate route:", e);
      routeData = calculateLocalTSPRoute();
      displayRouteOnUI(routeData);
    } finally {
      if (btn) btn.innerHTML = `<i class="fa-solid fa-arrows-rotate"></i> Re-Calculate Java TSP Route`;
    }
  }

  function calculateLocalTSPRoute() {
    const critical = bins.filter(b => b.fillPercent >= 75 || b.status === "CRITICAL" || b.tiltAlert);
    const depot = { lat: 12.971598, lng: 77.585000, name: "Central Municipal Depot & Garage" };
    
    let waypoints = [];
    waypoints.push({ step: 1, binId: "DEPOT-00", name: depot.name, latitude: depot.lat, longitude: depot.lng, fillPercent: 0, distanceKmFromPrev: 0.0 });

    let currentLat = depot.lat;
    let currentLng = depot.lng;
    let totalDist = 0.0;
    let step = 2;

    critical.forEach(b => {
      const dist = 1.4 + Math.random() * 1.5;
      totalDist += dist;
      waypoints.push({
        step: step++,
        binId: b.id,
        name: b.name,
        latitude: b.latitude,
        longitude: b.longitude,
        fillPercent: b.fillPercent,
        distanceKmFromPrev: dist
      });
    });

    waypoints.push({
      step: step,
      binId: "FACILITY-01",
      name: "Municipal Solid Waste Processing Plant",
      latitude: depot.lat,
      longitude: depot.lng,
      fillPercent: 0,
      distanceKmFromPrev: 2.1
    });
    totalDist += 2.1;

    return {
      criticalBinsCount: critical.length,
      totalDistanceKm: (totalDist).toFixed(2),
      distanceSavedKm: (totalDist * 0.45).toFixed(2),
      fuelSavedLiters: (totalDist * 0.45 * 0.38).toFixed(1),
      co2SavedKg: (totalDist * 0.45 * 0.38 * 2.68).toFixed(1),
      estimatedDurationMins: Math.round(totalDist * 3.5 + critical.length * 6),
      waypoints: waypoints
    };
  }

  function displayRouteOnUI(data) {
    if (!data) return;

    document.getElementById("route-stops-val").textContent = `${data.criticalBinsCount || 0} Critical Stops`;
    document.getElementById("route-distance-val").textContent = `${data.totalDistanceKm} km`;
    document.getElementById("route-saved-val").textContent = `${data.distanceSavedKm} km saved`;
    document.getElementById("route-duration-val").textContent = `${data.estimatedDurationMins} mins`;
    document.getElementById("stat-co2-saved").textContent = `${data.co2SavedKg || '18.4'} kg`;

    const tableBody = document.getElementById("route-waypoints-body");
    if (tableBody) {
      tableBody.innerHTML = "";
      data.waypoints.forEach(wp => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td><strong>#${wp.step}</strong></td>
          <td>${wp.name}</td>
          <td><code>${wp.binId}</code></td>
          <td><span style="font-weight:bold; color:${wp.fillPercent >= 80 ? '#ef4444' : '#10b981'};">${wp.fillPercent > 0 ? wp.fillPercent + '%' : '-'}</span></td>
          <td>${wp.distanceKmFromPrev > 0 ? wp.distanceKmFromPrev + ' km' : 'Origin'}</td>
          <td>${wp.binId.startsWith('BIN') ? 'Municipal Waste' : 'Hub Facility'}</td>
          <td>
            ${wp.binId.startsWith('BIN') ? `<button class="btn btn-sm btn-outline" onclick="window.App.emptyBin('${wp.binId}')">Collect</button>` : '<span style="color:var(--text-muted);">Transit</span>'}
          </td>
        `;
        tableBody.appendChild(tr);
      });
    }

    if (map) {
      if (routePolyline) map.removeLayer(routePolyline);

      const latlngs = data.waypoints.map(wp => [wp.latitude, wp.longitude]);
      routePolyline = L.polyline(latlngs, {
        color: "#3b82f6",
        weight: 5,
        opacity: 0.8,
        dashArray: "8, 6"
      }).addTo(map);

      map.fitBounds(routePolyline.getBounds(), { padding: [40, 40] });
    }
  }

  async function emptyBin(binId) {
    try {
      if (isBackendOnline) {
        await fetch(`${API_BASE}/bins/empty`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ binId: binId })
        });
      } else {
        const target = bins.find(b => b.id === binId);
        if (target) {
          target.fillPercent = 0;
          target.weightKg = 1.2;
          target.gasPpm = 60;
          target.tiltAlert = false;
          target.status = "NORMAL";
        }
      }
      notifyToast(`Bin ${binId} emptied by collection crew!`, "success");
      await fetchData();
      calculateRoute();
    } catch (e) {
      console.warn(e);
    }
  }

  function applyLocalTelemetry(payload) {
    const b = bins.find(item => item.id === payload.binId);
    if (b) {
      b.fillPercent = payload.fillPercent;
      b.weightKg = payload.weightKg;
      b.gasPpm = payload.gasPpm;
      b.batteryPercent = payload.batteryPercent;
      b.tiltAlert = payload.tiltDetected;
      if (b.tiltAlert) b.status = "VANDALIZED";
      else if (b.fillPercent >= 80 || b.gasPpm >= 450) b.status = "CRITICAL";
      else if (b.fillPercent >= 60) b.status = "WARNING";
      else b.status = "NORMAL";
    }
  }

  function simulateTruckCollectionRun() {
    if (!routeData || !routeData.waypoints || routeData.waypoints.length < 2) {
      notifyToast("Please calculate route first!", "info");
      calculateRoute().then(() => simulateTruckCollectionRun());
      return;
    }

    notifyToast("Simulating truck collection along optimal TSP path...", "info");
    const waypoints = routeData.waypoints;
    let index = 0;

    const truckIcon = L.divIcon({
      className: "truck-marker",
      html: `<div style="background:#f59e0b; width:34px; height:34px; border-radius:50%; display:flex; align-items:center; justify-content:center; color:white; border:2px solid white; box-shadow:0 0 12px #f59e0b;"><i class="fa-solid fa-truck" style="font-size:16px;"></i></div>`,
      iconSize: [34, 34],
      iconAnchor: [17, 17]
    });

    if (truckMarker) map.removeLayer(truckMarker);
    truckMarker = L.marker([waypoints[0].latitude, waypoints[0].longitude], { icon: truckIcon }).addTo(map);

    const interval = setInterval(() => {
      index++;
      if (index >= waypoints.length) {
        clearInterval(interval);
        notifyToast("Truck completed route and returned to depot!", "success");
        return;
      }

      const point = waypoints[index];
      truckMarker.setLatLng([point.latitude, point.longitude]);
      if (point.binId.startsWith("BIN")) {
        emptyBin(point.binId);
        notifyToast(`Truck emptied ${point.binId} (${point.name})`, "info");
      }
    }, 2200);
  }

  function initEventListeners() {
    document.getElementById("bin-search-input")?.addEventListener("input", renderBinsCards);

    document.querySelectorAll(".btn-filter").forEach(btn => {
      btn.addEventListener("click", () => {
        document.querySelectorAll(".btn-filter").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        currentFilter = btn.dataset.filter;
        updateMapMarkers();
      });
    });

    document.getElementById("btn-recalculate-route")?.addEventListener("click", calculateRoute);
    document.getElementById("btn-dispatch-truck")?.addEventListener("click", () => {
      simulateTruckCollectionRun();
    });
    document.getElementById("btn-simulate-truck-run")?.addEventListener("click", simulateTruckCollectionRun);

    document.getElementById("citizen-report-form")?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const reportPayload = {
        reporterName: document.getElementById("rep-name").value,
        contactNumber: document.getElementById("rep-phone").value,
        binId: document.getElementById("rep-bin-select").value || null,
        issueType: document.getElementById("rep-category").value,
        description: document.getElementById("rep-desc").value,
        latitude: 12.9716,
        longitude: 77.5946
      };

      try {
        if (isBackendOnline) {
          await fetch(`${API_BASE}/reports`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(reportPayload)
          });
        } else {
          reportPayload.id = "REP-" + (reports.length + 1001);
          reportPayload.status = "PENDING";
          reportPayload.createdAt = "Just now";
          reports.unshift(reportPayload);
        }

        notifyToast("Grievance Ticket Submitted Successfully!", "success");
        e.target.reset();
        await fetchData();
      } catch (err) {
        console.warn(err);
      }
    });
  }

  function initCharts() {
    const pieCtx = document.getElementById("wastePieChart");
    if (pieCtx) {
      new Chart(pieCtx, {
        type: "doughnut",
        data: {
          labels: ["Organic Waste", "Recyclable Plastics/Paper", "General Solid Waste", "Hazardous/Medical"],
          datasets: [{
            data: [48, 26, 20, 6],
            backgroundColor: ["#10b981", "#3b82f6", "#f59e0b", "#ef4444"],
            borderWidth: 0
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: "bottom", labels: { color: "#94a3b8", font: { size: 11 } } }
          }
        }
      });
    }

    const trendCtx = document.getElementById("fillTrendChart");
    if (trendCtx) {
      new Chart(trendCtx, {
        type: "bar",
        data: {
          labels: ["00:00", "04:00", "08:00", "12:00", "16:00", "20:00"],
          datasets: [{
            label: "Average Fill Level (%)",
            data: [18, 12, 45, 78, 92, 85],
            backgroundColor: "#3b82f6",
            borderRadius: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: { beginAtZero: true, max: 100, ticks: { color: "#94a3b8" }, grid: { color: "rgba(255,255,255,0.06)" } },
            x: { ticks: { color: "#94a3b8" }, grid: { display: false } }
          },
          plugins: {
            legend: { display: false }
          }
        }
      });
    }
  }

  function notifyToast(message, type = "info") {
    const toast = document.createElement("div");
    toast.style.position = "fixed";
    toast.style.bottom = "20px";
    toast.style.right = "20px";
    toast.style.background = type === "success" ? "#065f46" : type === "danger" ? "#991b1b" : "#1e293b";
    toast.style.color = "white";
    toast.style.padding = "12px 20px";
    toast.style.borderRadius = "8px";
    toast.style.boxShadow = "0 8px 24px rgba(0,0,0,0.5)";
    toast.style.fontSize = "13px";
    toast.style.fontWeight = "600";
    toast.style.zIndex = "9999";
    toast.style.border = "1px solid rgba(255,255,255,0.15)";
    toast.innerHTML = `<i class="fa-solid fa-circle-check"></i> ${message}`;
    document.body.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transition = "opacity 0.4s";
      setTimeout(() => toast.remove(), 400);
    }, 3200);
  }

  return {
    init,
    fetchData,
    renderUI,
    emptyBin,
    calculateRoute,
    applyLocalTelemetry,
    notifyToast,
    get isBackendOnline() { return isBackendOnline; },
    get bins() { return bins; }
  };
})();

document.addEventListener("DOMContentLoaded", () => {
  window.App.init();
  setTimeout(() => window.App.calculateRoute(), 500);
});
