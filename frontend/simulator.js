/**
 * IoT Hardware Telemetry Simulator
 * Simulates ESP32 Microcontroller sensor data transmission to Java REST Backend.
 */

window.Simulator = (function() {
  let isAutoStreaming = false;
  let autoStreamInterval = null;

  const drawer = document.getElementById('simulator-drawer');
  const btnToggle = document.getElementById('btn-toggle-sim');
  const btnClose = document.getElementById('btn-close-sim');
  const selectBin = document.getElementById('sim-bin-id');
  const rangeFill = document.getElementById('sim-fill-range');
  const rangeGas = document.getElementById('sim-gas-range');
  const rangeWeight = document.getElementById('sim-weight-range');
  const toggleTilt = document.getElementById('sim-tilt-toggle');
  
  const valFill = document.getElementById('sim-fill-val');
  const valGas = document.getElementById('sim-gas-val');
  const valWeight = document.getElementById('sim-weight-val');
  const codePayload = document.getElementById('sim-payload-code');
  const btnSend = document.getElementById('btn-send-sim-telemetry');
  const btnAuto = document.getElementById('btn-auto-stream-sim');

  function init() {
    setupEventListeners();
    updatePayloadPreview();
  }

  function setupEventListeners() {
    btnToggle.addEventListener('click', () => {
      drawer.classList.toggle('open');
      populateBinDropdown();
    });

    btnClose.addEventListener('click', () => {
      drawer.classList.remove('open');
    });

    rangeFill.addEventListener('input', (e) => {
      valFill.textContent = e.target.value + '%';
      rangeWeight.value = (e.target.value * 0.48).toFixed(1);
      valWeight.textContent = rangeWeight.value + ' kg';
      updatePayloadPreview();
    });

    rangeGas.addEventListener('input', (e) => {
      valGas.textContent = e.target.value + ' PPM';
      updatePayloadPreview();
    });

    rangeWeight.addEventListener('input', (e) => {
      valWeight.textContent = e.target.value + ' kg';
      updatePayloadPreview();
    });

    toggleTilt.addEventListener('change', () => {
      updatePayloadPreview();
    });

    selectBin.addEventListener('change', () => {
      syncSlidersWithSelectedBin();
      updatePayloadPreview();
    });

    btnSend.addEventListener('click', () => {
      transmitTelemetryPayload();
    });

    btnAuto.addEventListener('click', () => {
      toggleAutoStream();
    });
  }

  function populateBinDropdown() {
    if (!window.App || !window.App.bins) return;
    const currentVal = selectBin.value;
    selectBin.innerHTML = '';
    window.App.bins.forEach(bin => {
      const opt = document.createElement('option');
      opt.value = bin.id;
      opt.textContent = `${bin.id} - ${bin.name} (${bin.fillPercent}%)`;
      selectBin.appendChild(opt);
    });
    if (currentVal) selectBin.value = currentVal;
    syncSlidersWithSelectedBin();
  }

  function syncSlidersWithSelectedBin() {
    if (!window.App || !window.App.bins) return;
    const bin = window.App.bins.find(b => b.id === selectBin.value);
    if (bin) {
      rangeFill.value = bin.fillPercent;
      valFill.textContent = bin.fillPercent + '%';
      rangeGas.value = bin.gasPpm || 120;
      valGas.textContent = (bin.gasPpm || 120) + ' PPM';
      rangeWeight.value = bin.weightKg || 15;
      valWeight.textContent = (bin.weightKg || 15) + ' kg';
      toggleTilt.checked = !!bin.tiltAlert;
      updatePayloadPreview();
    }
  }

  function getPayloadObject() {
    return {
      binId: selectBin.value || 'BIN-001',
      fillPercent: parseInt(rangeFill.value, 10),
      weightKg: parseFloat(rangeWeight.value),
      gasPpm: parseInt(rangeGas.value, 10),
      batteryPercent: 92,
      tiltDetected: toggleTilt.checked
    };
  }

  function updatePayloadPreview() {
    const payload = getPayloadObject();
    codePayload.textContent = JSON.stringify(payload, null, 2);
  }

  async function transmitTelemetryPayload() {
    const payload = getPayloadObject();
    btnSend.disabled = true;
    btnSend.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Uploading...';

    try {
      if (window.App && window.App.isBackendOnline) {
        const resp = await fetch('http://localhost:8080/api/telemetry', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (resp.ok) {
          window.App.notifyToast(`Telemetry sent for ${payload.binId}!`, 'success');
        }
      } else if (window.App) {
        window.App.applyLocalTelemetry(payload);
        window.App.notifyToast(`[Offline Sim] Updated ${payload.binId}`, 'info');
      }
      if (window.App) window.App.fetchData();
    } catch (e) {
      console.warn("Telemetry transmission error:", e);
      if (window.App) window.App.applyLocalTelemetry(payload);
    } finally {
      setTimeout(() => {
        btnSend.disabled = false;
        btnSend.innerHTML = '<i class="fa-solid fa-tower-broadcast"></i> Transmit Packet to Java API';
      }, 400);
    }
  }

  function toggleAutoStream() {
    if (isAutoStreaming) {
      clearInterval(autoStreamInterval);
      isAutoStreaming = false;
      btnAuto.classList.remove('btn-success');
      btnAuto.classList.add('btn-outline');
      btnAuto.innerHTML = '<i class="fa-solid fa-play"></i> Start Auto Real-Time Influx';
    } else {
      isAutoStreaming = true;
      btnAuto.classList.remove('btn-outline');
      btnAuto.classList.add('btn-success');
      btnAuto.innerHTML = '<i class="fa-solid fa-stop"></i> Stop Auto Influx';

      autoStreamInterval = setInterval(() => {
        if (!window.App || !window.App.bins) return;
        const randomBin = window.App.bins[Math.floor(Math.random() * window.App.bins.length)];
        const newFill = Math.min(100, randomBin.fillPercent + Math.floor(Math.random() * 8) + 1);
        const newWeight = (newFill * 0.45).toFixed(1);
        const newGas = Math.min(800, (randomBin.gasPpm || 100) + Math.floor(Math.random() * 25));

        const simPayload = {
          binId: randomBin.id,
          fillPercent: newFill,
          weightKg: parseFloat(newWeight),
          gasPpm: newGas,
          batteryPercent: 91,
          tiltDetected: randomBin.tiltAlert || false
        };

        if (window.App.isBackendOnline) {
          fetch('http://localhost:8080/api/telemetry', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(simPayload)
          }).then(() => window.App.fetchData()).catch(() => {});
        } else {
          window.App.applyLocalTelemetry(simPayload);
          window.App.renderUI();
        }
      }, 4000);
    }
  }

  return { init, populateBinDropdown };
})();

document.addEventListener('DOMContentLoaded', () => {
  Simulator.init();
});
