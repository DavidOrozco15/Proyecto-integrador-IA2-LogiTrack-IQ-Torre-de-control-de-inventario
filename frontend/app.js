/**
 * LogiTrack IQ - Frontend Application
 * Torre de Control de Inventario
 */

const API_BASE = '/api';
const TOKEN_KEY = 'logitrack_token';
const USER_KEY = 'logitrack_user';

const ENDPOINTS = {
    login: `${API_BASE}/auth/login`,
    kpis: `${API_BASE}/kpis`,
    productosRiesgo: `${API_BASE}/productos/riesgo`,
    ordenes: `${API_BASE}/ordenes`,
    resumen: `${API_BASE}/panel/resumen`,
    bodegas: `${API_BASE}/bodegas`,
    bodegasStock: `${API_BASE}/bodegas/stock`,
    proveedores: `${API_BASE}/proveedores`,
    ordenPdf: (id) => `${API_BASE}/ordenes/${id}/pdf`,
    ordenEstado: (id) => `${API_BASE}/ordenes/${id}/estado`,
};

let state = {
    token: null,
    user: null,
    currentPage: 'dashboard',
    currentOrderFilter: 'BORRADOR',
    data: {
        kpis: null,
        productosRiesgo: [],
        ordenes: [],
        resumen: null,
        bodegas: [],
    }
};

// ========================================
// Utilities
// ========================================

function formatCurrency(v) {
    if (v == null) return '--';
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(v);
}

function formatNumber(v) {
    if (v == null) return '--';
    return new Intl.NumberFormat('es-CO').format(v);
}

function formatDate(d) {
    if (!d) return '--';
    try {
        return new Date(d).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch { return d; }
}

function formatDateOnly(d) {
    if (!d) return '--';
    try {
        return new Date(d).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch { return d; }
}

function getEstadoClass(e) {
    return { BORRADOR: 'badge-warning', APROBADA: 'badge-info', RECIBIDA: 'badge-success', CANCELADA: 'badge-danger', PENDIENTE: 'badge-info', EN_RIESGO: 'badge-danger', CRITICO: 'badge-danger', SIN_CONSUMO: 'badge-secondary' }[e] || 'badge-secondary';
}

function getSeverityClass(s) {
    return { ALTA: 'badge-danger', MEDIA: 'badge-warning', BAJA: 'badge-info' }[s] || 'badge-secondary';
}

function getAccionClass(t) {
    return { REVISAR_ORDEN: 'badge-primary', REVISAR_PRODUCTO: 'badge-warning', REVISAR_BODEGA: 'badge-info' }[t] || 'badge-secondary';
}

function getOccupancyClass(pct) {
    if (pct >= 90) return 'red';
    if (pct >= 70) return 'yellow';
    return 'green';
}

function getOccupancyLabel(pct) {
    if (pct >= 90) return 'CRITICA';
    if (pct >= 70) return 'ALTA';
    return 'NORMAL';
}

// ========================================
// Auth
// ========================================

function getToken() {
    if (!state.token) state.token = sessionStorage.getItem(TOKEN_KEY);
    return state.token;
}

function setToken(t) {
    state.token = t;
    t ? sessionStorage.setItem(TOKEN_KEY, t) : sessionStorage.removeItem(TOKEN_KEY);
}

function getUser() {
    if (!state.user) {
        const s = sessionStorage.getItem(USER_KEY);
        if (s) try { state.user = JSON.parse(s); } catch { sessionStorage.removeItem(USER_KEY); }
    }
    return state.user;
}

function setUser(u) {
    state.user = u;
    u ? sessionStorage.setItem(USER_KEY, JSON.stringify(u)) : sessionStorage.removeItem(USER_KEY);
}

function clearAuth() { setToken(null); setUser(null); }
function isAuthenticated() { return !!getToken(); }
function getUserRole() { return getUser()?.rol || null; }
function hasRole(...r) { return r.includes(getUserRole()); }

// ========================================
// API Client
// ========================================

async function apiRequest(url, opts = {}) {
    const token = getToken();
    const headers = { 'Content-Type': 'application/json', ...opts.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(url, { ...opts, headers });

    if (res.status === 401) { clearAuth(); window.location.href = 'login.html'; throw new Error('Sesion expirada'); }
    if (res.status === 403) throw new Error('No tiene permisos para realizar esta accion');
    if (res.status === 404) throw new Error('Recurso no encontrado');

    const ct = res.headers.get('content-type');
    let data;
    if (ct && ct.includes('application/json')) data = await res.json();
    else if (ct && ct.includes('application/pdf')) data = await res.blob();
    else data = await res.text();

    if (!res.ok) throw new Error(data?.message || data?.error || `Error ${res.status}`);
    return data;
}

// ========================================
// Auth Functions
// ========================================

async function login(username, password) {
    const r = await apiRequest(ENDPOINTS.login, { method: 'POST', body: JSON.stringify({ username, password }) });
    setToken(r.token);
    setUser({ id: r.userId, username: r.username, email: r.email, rol: r.rol });
    return r;
}

function logout() { clearAuth(); window.location.href = 'login.html'; }

// ========================================
// Data Fetching
// ========================================

async function fetchKPIs() {
    const d = await apiRequest(ENDPOINTS.kpis);
    state.data.kpis = d;
    return d;
}

async function fetchProductosRiesgo() {
    const d = await apiRequest(ENDPOINTS.productosRiesgo);
    state.data.productosRiesgo = d || [];
    return state.data.productosRiesgo;
}

async function fetchOrdenes(estado) {
    const url = estado ? `${ENDPOINTS.ordenes}?estado=${estado}` : ENDPOINTS.ordenes;
    const d = await apiRequest(url);
    state.data.ordenes = d || [];
    return state.data.ordenes;
}

async function fetchResumen() {
    try {
        const d = await apiRequest(ENDPOINTS.resumen);
        state.data.resumen = d;
        return d;
    } catch (e) {
        if (e.message.includes('404')) { state.data.resumen = null; return null; }
        throw e;
    }
}

async function fetchBodegasStock() {
    try {
        const d = await apiRequest(ENDPOINTS.bodegasStock);
        state.data.bodegas = d || [];
        return state.data.bodegas;
    } catch {
        state.data.bodegas = [];
        return [];
    }
}

async function fetchAllDashboardData() {
    await Promise.allSettled([
        fetchKPIs(),
        fetchProductosRiesgo(),
        fetchOrdenes(state.currentOrderFilter),
        fetchResumen(),
        fetchBodegasStock(),
    ]);
}

// ========================================
// PDF Functions - POST first, then GET
// ========================================

async function generateAndOpenPdf(orderId) {
    const btn = document.querySelector(`.btn-pdf[data-id="${orderId}"]`);
    const originalHtml = btn?.innerHTML;
    if (btn) { btn.innerHTML = '<span class="btn-loader" style="display:block"></span>'; btn.disabled = true; }

    try {
        // Step 1: POST to generate PDF bytes and save them
        await apiRequest(ENDPOINTS.ordenPdf(orderId), { method: 'POST' });
        // Step 2: GET to retrieve the generated PDF
        const blob = await apiRequest(ENDPOINTS.ordenPdf(orderId), { method: 'GET' });
        openPdfInModal(blob, orderId);
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        if (btn) { btn.innerHTML = originalHtml; btn.disabled = false; }
    }
}

function openPdfInModal(blob, orderId) {
    const url = URL.createObjectURL(blob);
    const frame = document.getElementById('pdfFrame');
    const modal = document.getElementById('pdfModal');
    const title = document.getElementById('pdfOrderId');
    const dlBtn = document.getElementById('downloadPdfBtn');

    frame.src = url;
    title.textContent = `#${orderId}`;

    dlBtn.onclick = () => {
        const a = document.createElement('a');
        a.href = url;
        a.download = `orden-${orderId}.pdf`;
        a.click();
    };

    modal.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
}

function closePdfModal() {
    const modal = document.getElementById('pdfModal');
    const frame = document.getElementById('pdfFrame');
    const url = frame.src;
    modal.classList.add('hidden');
    frame.src = 'about:blank';
    document.body.style.overflow = '';
    if (url && url !== 'about:blank') {
        setTimeout(() => URL.revokeObjectURL(url), 200);
    }
}

// ========================================
// Order Actions
// ========================================

async function aprobarOrden(orderId) {
    return apiRequest(ENDPOINTS.ordenEstado(orderId), { method: 'PATCH', body: JSON.stringify({ estado: 'APROBADA' }) });
}

// ========================================
// UI Rendering
// ========================================

function animateValue(el, end) {
    if (end == null || isNaN(end)) { el.textContent = '--'; return; }
    const start = parseInt(el.textContent) || 0;
    if (start === end) { el.textContent = formatNumber(end); return; }
    const duration = 800;
    const startTime = performance.now();
    function step(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        el.textContent = formatNumber(Math.round(start + (end - start) * eased));
        if (progress < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
}

function renderKPIs() {
    const kpis = state.data.kpis;
    if (!kpis) {
        ['kpiProductosRiesgo', 'kpiProductosQuiebre', 'kpiOrdenesAprobar', 'kpiBodegasCriticas']
            .forEach(id => { const e = document.getElementById(id); if (e) e.textContent = '--'; });
        return;
    }

    const elR = document.getElementById('kpiProductosRiesgo');
    const elQ = document.getElementById('kpiProductosQuiebre');
    const elO = document.getElementById('kpiOrdenesAprobar');
    const elB = document.getElementById('kpiBodegasCriticas');

    if (elR) animateValue(elR, kpis.productosEnRiesgo || 0);
    if (elQ) animateValue(elQ, kpis.productosEnQuiebre || 0);

    if (elO) {
        const o = kpis.ordenesPorAprobar || {};
        elO.textContent = `${formatNumber(o.cantidad || 0)}`;
        elO.title = `Monto total: ${formatCurrency(o.montoTotal)}`;
    }

    if (elB) {
        const bodegas = kpis.ocupacionPorBodega || [];
        const criticas = bodegas.filter(b => b.porcentaje >= 90).length;
        animateValue(elB, criticas);
    }
}

function renderMovimientosAyer() {
    const m = state.data.kpis?.movimientosAyer || {};
    const elE = document.getElementById('movEntradas');
    const elS = document.getElementById('movSalidas');
    const elT = document.getElementById('movTransferencias');
    const elD = document.getElementById('ayerDate');

    if (elE) animateValue(elE, m.entrada || 0);
    if (elS) animateValue(elS, m.salida || 0);
    if (elT) animateValue(elT, m.transferencia || 0);

    if (elD && state.data.kpis?.calculadoEn) {
        const d = new Date(state.data.kpis.calculadoEn);
        const ayer = new Date(d);
        ayer.setDate(ayer.getDate() - 1);
        elD.textContent = ayer.toLocaleDateString('es-CO', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    }
}

function renderOcupacion() {
    const tbody = document.getElementById('ocupacionBody');
    if (!tbody) return;

    const bodegas = state.data.bodegas || [];

    if (bodegas.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state" style="padding:32px"><p>No hay datos de ocupacion disponibles</p></td></tr>`;
        return;
    }

    tbody.innerHTML = bodegas.map((b, i) => {
        const pct = b.porcentajeOcupacion || 0;
        const cls = getOccupancyClass(pct);
        const label = getOccupancyLabel(pct);
        return `
            <tr style="animation: slideUp 0.3s ease ${i * 0.05}s both">
                <td><strong>${b.bodegaNombre || `Bodega ${b.bodegaId}`}</strong></td>
                <td>${formatNumber(b.stockTotal)}</td>
                <td>${formatNumber(b.capacidad)}</td>
                <td>
                    <div class="progress-bar">
                        <div class="progress-track">
                            <div class="progress-fill ${cls}" style="width:${Math.min(pct, 100)}%"></div>
                        </div>
                        <span class="badge badge-${cls === 'red' ? 'danger' : cls === 'yellow' ? 'warning' : 'success'}">${pct.toFixed(1)}%</span>
                    </div>
                </td>
                <td><span class="badge badge-${cls === 'red' ? 'danger' : cls === 'yellow' ? 'warning' : 'success'}">${label}</span></td>
            </tr>`;
    }).join('');
}

function renderResumen() {
    const c = document.getElementById('resumenContent');
    const fEl = document.getElementById('resumenFecha');
    const r = state.data.resumen;
    if (!c) return;

    if (!r) {
        c.innerHTML = `
            <div class="empty-state">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                    <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
                <p>No hay resumen publicado para hoy</p>
                <small>El flujo n8n publica automaticamente a las 6:00 AM</small>
            </div>`;
        if (fEl) fEl.textContent = 'Sin publicar';
        return;
    }

    if (fEl) { fEl.textContent = formatDateOnly(r.fecha); fEl.className = 'badge badge-info'; }

    let alertasHtml = '';
    if (r.alertas?.length) {
        alertasHtml = `<div class="resumen-section"><h4>Alertas</h4><div class="alertas-list">${r.alertas.map(a => `
            <div class="alerta-item">
                <div class="alerta-header"><span class="badge ${getSeverityClass(a.severidad)}">${a.severidad}</span><strong>${a.titulo}</strong></div>
                <p class="alerta-detalle">${a.detalle}</p>
                <div class="alerta-refs">
                    ${a.productoId ? `<span class="ref">Producto: #${a.productoId}</span>` : ''}
                    ${a.ordenId ? `<span class="ref">Orden: #${a.ordenId}</span>` : ''}
                    ${a.bodegaId ? `<span class="ref">Bodega: #${a.bodegaId}</span>` : ''}
                </div>
            </div>`).join('')}</div></div>`;
    }

    let accionesHtml = '';
    if (r.accionesSugeridas?.length) {
        accionesHtml = `<div class="resumen-section"><h4>Acciones Sugeridas</h4><div class="acciones-list">${r.accionesSugeridas.map(a => `
            <div class="accion-item">
                <span class="badge ${getAccionClass(a.tipo)}">${a.tipo}</span>
                <span>${a.descripcion}</span>
                ${a.ordenId ? `<span class="ref">Orden: #${a.ordenId}</span>` : ''}
                ${a.productoId ? `<span class="ref">Producto: #${a.productoId}</span>` : ''}
                ${a.bodegaId ? `<span class="ref">Bodega: #${a.bodegaId}</span>` : ''}
            </div>`).join('')}</div></div>`;
    }

    c.innerHTML = `
        <div class="resumen-card">
            <div class="resumen-narrativa"><h4>Narrativa</h4><p>${r.narrativa}</p></div>
            ${alertasHtml}${accionesHtml}
        </div>`;
}

function renderOrdenes() {
    const tbody = document.getElementById('ordenesBody');
    const emptyEl = document.getElementById('ordenesEmpty');
    const table = document.getElementById('ordenesTable');
    const ordenes = state.data.ordenes || [];
    const isAdmin = hasRole('ADMIN');

    if (!tbody || !emptyEl || !table) return;

    if (ordenes.length === 0) {
        tbody.innerHTML = '';
        table.style.display = 'none';
        emptyEl.classList.remove('hidden');
        return;
    }

    table.style.display = 'table';
    emptyEl.classList.add('hidden');

    tbody.innerHTML = ordenes.map((o, i) => `
        <tr data-id="${o.id}" style="animation: slideUp 0.3s ease ${i * 0.04}s both">
            <td><strong>#${o.id}</strong></td>
            <td>${o.producto?.nombre || `Producto #${o.productoId}`}</td>
            <td>${o.proveedor?.nombre || (o.proveedorId ? `#${o.proveedorId}` : '--')}</td>
            <td>${o.bodegaDestino?.nombre || `Bodega #${o.bodegaDestinoId}`}</td>
            <td>${formatNumber(o.cantidad)}</td>
            <td><strong>${formatCurrency(o.total)}</strong></td>
            <td>${formatDate(o.fechaCreacion)}</td>
            <td><span class="badge ${getEstadoClass(o.estado)}">${o.estado}</span></td>
            <td>
                <div class="actions-cell">
                    <button class="action-btn action-btn-view" data-action="pdf" data-id="${o.id}" title="${o.pdfBytes ? 'Ver PDF' : 'Generar PDF'}">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                            <polyline points="14 2 14 8 20 8"></polyline>
                        </svg>
                    </button>
                    ${isAdmin && o.estado === 'BORRADOR' ? `
                        <button class="action-btn action-btn-success" data-action="aprobar" data-id="${o.id}" title="Aprobar orden">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        </button>` : ''}
                    ${isAdmin && o.estado === 'APROBADA' ? `
                        <button class="action-btn action-btn-success" data-action="recibir" data-id="${o.id}" title="Recibir orden">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                <polyline points="7 10 12 15 17 10"></polyline>
                                <line x1="12" y1="15" x2="12" y2="3"></line>
                            </svg>
                        </button>` : ''}
                    ${isAdmin && (o.estado === 'BORRADOR' || o.estado === 'APROBADA') ? `
                        <button class="action-btn action-btn-danger" data-action="cancelar" data-id="${o.id}" title="Cancelar orden">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>` : ''}
                </div>
            </td>
        </tr>`).join('');

    tbody.querySelectorAll('.action-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const action = btn.dataset.action;
            const id = parseInt(btn.dataset.id);
            if (action === 'pdf') generateAndOpenPdf(id);
            else if (action === 'aprobar') handleAprobarOrden(id);
            else if (action === 'recibir') handleRecibirOrden(id);
            else if (action === 'cancelar') handleCancelarOrden(id);
        });
    });
}

function renderProductosRiesgo() {
    const tbody = document.getElementById('riesgoBody');
    const emptyEl = document.getElementById('riesgoEmpty');
    const table = document.getElementById('riesgoTable');
    const productos = state.data.productosRiesgo || [];

    if (!tbody || !emptyEl || !table) return;

    if (productos.length === 0) {
        tbody.innerHTML = '';
        table.style.display = 'none';
        emptyEl.classList.remove('hidden');
        return;
    }

    table.style.display = 'table';
    emptyEl.classList.add('hidden');

    tbody.innerHTML = productos.map((p, i) => `
        <tr style="animation: slideUp 0.3s ease ${i * 0.04}s both">
            <td><strong>${p.nombre}</strong></td>
            <td>${p.proveedor || '--'}</td>
            <td>${formatNumber(p.stockTotal)}</td>
            <td>${p.consumoDiarioPromedio ? p.consumoDiarioPromedio.toFixed(2) : '--'}</td>
            <td>${p.puntoReorden ? Math.ceil(p.puntoReorden) : '--'}</td>
            <td>${p.diasCobertura ? p.diasCobertura.toFixed(1) : (p.estado === 'SIN_CONSUMO' ? 'SIN_CONSUMO' : '--')}</td>
            <td><span class="badge ${getEstadoClass(p.estado)}">${p.estado}</span></td>
            <td>${p.bodegaDestinoId ? `#${p.bodegaDestinoId}` : '--'}</td>
        </tr>`).join('');
}

function renderBodegas() {
    const tbody = document.getElementById('bodegasBody');
    if (!tbody) return;

    const bodegas = state.data.bodegas || [];

    if (bodegas.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state" style="padding:32px"><p>No hay datos de bodegas disponibles</p></td></tr>`;
        return;
    }

    tbody.innerHTML = bodegas.map((b, i) => {
        const pct = b.porcentajeOcupacion || 0;
        const cls = getOccupancyClass(pct);
        const label = getOccupancyLabel(pct);
        return `
            <tr style="animation: slideUp 0.3s ease ${i * 0.05}s both">
                <td><strong>${b.bodegaNombre || `Bodega ${b.bodegaId}`}</strong></td>
                <td>${formatNumber(b.stockTotal)}</td>
                <td>${formatNumber(b.capacidad)}</td>
                <td>
                    <div class="progress-bar">
                        <div class="progress-track">
                            <div class="progress-fill ${cls}" style="width:${Math.min(pct, 100)}%"></div>
                        </div>
                        <span class="badge badge-${cls === 'red' ? 'danger' : cls === 'yellow' ? 'warning' : 'success'}">${pct.toFixed(1)}%</span>
                    </div>
                </td>
                <td><span class="badge badge-${cls === 'red' ? 'danger' : cls === 'yellow' ? 'warning' : 'success'}">${label}</span></td>
            </tr>`;
    }).join('');
}

// ========================================
// Event Handlers
// ========================================

async function handleLogin(e) {
    e.preventDefault();
    const form = e.target;
    const username = form.username.value.trim();
    const password = form.password.value;
    const btn = document.getElementById('loginBtn');
    const errorEl = document.getElementById('loginError');

    if (!username || !password) { showError(errorEl, 'Ingrese usuario y contrasena'); return; }

    btn.classList.add('loading');
    btn.querySelector('.btn-text').classList.add('hidden');
    btn.querySelector('.btn-loader').classList.remove('hidden');
    btn.disabled = true;
    errorEl.classList.add('hidden');

    try {
        await login(username, password);
        window.location.href = 'dashboard.html';
    } catch (error) {
        showError(errorEl, error.message);
    } finally {
        btn.classList.remove('loading');
        btn.querySelector('.btn-text').classList.remove('hidden');
        btn.querySelector('.btn-loader').classList.add('hidden');
        btn.disabled = false;
    }
}

function showError(el, msg) { el.textContent = msg; el.classList.remove('hidden'); }

async function handleAprobarOrden(orderId) {
    const confirmed = await showConfirm('Aprobar Orden', `Esta seguro de aprobar la orden #${orderId}? Esta accion no se puede deshacer.`);
    if (!confirmed) return;
    try {
        await aprobarOrden(orderId);
        showToast('Orden aprobada correctamente', 'success');
        await refreshDashboard();
    } catch (error) { showToast(error.message, 'error'); }
}

async function handleRecibirOrden(orderId) {
    const confirmed = await showConfirm('Recibir Orden', `Esta seguro de recibir la orden #${orderId}? Esta accion registrara una entrada de inventario.`);
    if (!confirmed) return;
    try {
        await apiRequest(ENDPOINTS.ordenEstado(orderId), { method: 'PATCH', body: JSON.stringify({ estado: 'RECIBIDA' }) });
        showToast('Orden recibida. Entrada de inventario registrada.', 'success');
        await refreshDashboard();
    } catch (error) { showToast(error.message, 'error'); }
}

async function handleCancelarOrden(orderId) {
    const confirmed = await showConfirm('Cancelar Orden', `Esta seguro de cancelar la orden #${orderId}? Esta accion no se puede deshacer.`);
    if (!confirmed) return;
    try {
        await apiRequest(ENDPOINTS.ordenEstado(orderId), { method: 'PATCH', body: JSON.stringify({ estado: 'CANCELADA' }) });
        showToast('Orden cancelada', 'success');
        await refreshDashboard();
    } catch (error) { showToast(error.message, 'error'); }
}

function showConfirm(title, message) {
    return new Promise((resolve) => {
        const modal = document.getElementById('confirmModal');
        const titleEl = document.getElementById('confirmTitle');
        const msgEl = document.getElementById('confirmMessage');
        const okBtn = document.getElementById('confirmOk');
        const cancelBtn = document.getElementById('confirmCancel');

        titleEl.textContent = title;
        msgEl.textContent = message;
        modal.classList.remove('hidden');
        document.body.style.overflow = 'hidden';

        const cleanup = () => {
            modal.classList.add('hidden');
            document.body.style.overflow = '';
            okBtn.removeEventListener('click', onOk);
            cancelBtn.removeEventListener('click', onCancel);
        };

        const onOk = () => { cleanup(); resolve(true); };
        const onCancel = () => { cleanup(); resolve(false); };

        okBtn.addEventListener('click', onOk);
        cancelBtn.addEventListener('click', onCancel);
        modal.querySelector('.modal-overlay').onclick = () => { cleanup(); resolve(false); };
    });
}

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            ${type === 'success' ? '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>' :
              type === 'error' ? '<circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>' :
              '<circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>'}
        </svg>
        <span>${message}</span>`;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.3s ease forwards';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

async function refreshDashboard() {
    await fetchAllDashboardData();
    renderDashboard();
}

function renderDashboard() {
    renderKPIs();
    renderMovimientosAyer();
    renderOcupacion();
    renderResumen();
    renderOrdenes();
    renderProductosRiesgo();
    renderBodegas();
}

// ========================================
// Navigation
// ========================================

function navigateTo(page) {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.toggle('active', item.dataset.page === page);
    });
    document.querySelectorAll('.page').forEach(p => {
        p.classList.toggle('active', p.id === `page-${page}`);
    });

    const titles = { dashboard: 'Dashboard', ordenes: 'Ordenes de Compra', productos: 'Productos en Riesgo', bodegas: 'Bodegas' };
    const titleEl = document.getElementById('pageTitle');
    if (titleEl) titleEl.textContent = titles[page] || 'Dashboard';

    state.currentPage = page;
    document.getElementById('sidebar')?.classList.remove('open');
}

// ========================================
// Initialization
// ========================================

function initLogin() {
    if (isAuthenticated()) { window.location.href = 'dashboard.html'; return; }
    const form = document.getElementById('loginForm');
    if (form) form.addEventListener('submit', handleLogin);
}

function initDashboard() {
    if (!isAuthenticated()) { window.location.href = 'login.html'; return; }

    const user = getUser();
    if (user) {
        const nameEl = document.getElementById('userName');
        const roleEl = document.getElementById('userRole');
        if (nameEl) nameEl.textContent = user.username;
        if (roleEl) { roleEl.textContent = user.rol; roleEl.className = `role-badge ${user.rol}`; }
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', logout);

    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.getElementById('sidebar');
    if (menuToggle && sidebar) {
        menuToggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    }

    document.addEventListener('click', (e) => {
        if (window.innerWidth <= 768 && sidebar && menuToggle) {
            if (!sidebar.contains(e.target) && !menuToggle.contains(e.target)) {
                sidebar.classList.remove('open');
            }
        }
    });

    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => { e.preventDefault(); navigateTo(item.dataset.page); });
    });

    // Order filter buttons
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.preventDefault();
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.currentOrderFilter = btn.dataset.status;
            await fetchOrdenes(state.currentOrderFilter);
            renderOrdenes();
        });
    });

    // PDF Modal
    const closePdfModalBtn = document.getElementById('closePdfModal');
    const closePdfBtn = document.getElementById('closePdfBtn');
    const pdfModal = document.getElementById('pdfModal');
    [closePdfModalBtn, closePdfBtn].forEach(btn => { if (btn) btn.addEventListener('click', closePdfModal); });
    if (pdfModal) pdfModal.querySelector('.modal-overlay').addEventListener('click', closePdfModal);

    loadDashboard();
}

async function loadDashboard() {
    try {
        document.body.classList.add('loading');
        await fetchAllDashboardData();
        renderDashboard();
    } catch (error) {
        console.error('Error loading dashboard:', error);
        if (error.message.includes('Sesion expirada') || error.message.includes('401')) {
            window.location.href = 'login.html';
        }
    } finally {
        document.body.classList.remove('loading');
    }
}

// ========================================
// Exports
// ========================================

window.LogiTrack = {
    apiRequest, login, logout, getToken, getUser, getUserRole, hasRole,
    fetchKPIs, fetchProductosRiesgo, fetchOrdenes, fetchResumen,
    generateAndOpenPdf, aprobarOrden, navigateTo, state,
};
