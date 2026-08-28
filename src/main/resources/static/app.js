/**
 * LogiTrack IQ - Frontend Application
 * Torre de Control de Inventario
 */

// ========================================
// Configuration & Constants
// ========================================

const API_BASE = '/api';
const TOKEN_KEY = 'logitrack_token';
const USER_KEY = 'logitrack_user';

const ENDPOINTS = {
    login: `${API_BASE}/auth/login`,
    kpis: `${API_BASE}/kpis`,
    productosRiesgo: `${API_BASE}/productos/riesgo`,
    ordenesBorrador: `${API_BASE}/ordenes?estado=BORRADOR`,
    resumen: `${API_BASE}/panel/resumen`,
    ocupacion: `${API_BASE}/bodegas/criticas`,
    proveedores: `${API_BASE}/proveedores`,
    ordenPdf: (id) => `${API_BASE}/ordenes/${id}/pdf`,
    ordenEstado: (id) => `${API_BASE}/ordenes/${id}/estado`,
};

// ========================================
// State Management
// ========================================

let state = {
    token: null,
    user: null,
    currentPage: 'dashboard',
    data: {
        kpis: null,
        productosRiesgo: [],
        ordenesBorrador: [],
        resumen: null,
        ocupacion: [],
    }
};

// ========================================
// Utility Functions
// ========================================

function formatCurrency(value) {
    if (value == null) return '--';
    return new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
    }).format(value);
}

function formatNumber(value) {
    if (value == null) return '--';
    return new Intl.NumberFormat('es-CO').format(value);
}

function formatDate(dateString) {
    if (!dateString) return '--';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('es-CO', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    } catch {
        return dateString;
    }
}

function formatDateOnly(dateString) {
    if (!dateString) return '--';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('es-CO', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
        });
    } catch {
        return dateString;
    }
}

function getEstadoClass(estado) {
    const classes = {
        'BORRADOR': 'badge-warning',
        'APROBADA': 'badge-info',
        'RECIBIDA': 'badge-success',
        'CANCELADA': 'badge-danger',
        'PENDIENTE': 'badge-info',
        'EN_RIESGO': 'badge-danger',
        'CRITICO': 'badge-danger',
        'SIN_CONSUMO': 'badge-secondary',
    };
    return classes[estado] || 'badge-secondary';
}

function getSeverityClass(severidad) {
    const classes = {
        'ALTA': 'badge-danger',
        'MEDIA': 'badge-warning',
        'BAJA': 'badge-info',
    };
    return classes[severidad] || 'badge-secondary';
}

function getAccionClass(tipo) {
    const classes = {
        'REVISAR_ORDEN': 'badge-primary',
        'REVISAR_PRODUCTO': 'badge-warning',
        'REVISAR_BODEGA': 'badge-info',
    };
    return classes[tipo] || 'badge-secondary';
}

// ========================================
// Auth & Token Management
// ========================================

function getToken() {
    if (!state.token) {
        state.token = sessionStorage.getItem(TOKEN_KEY);
    }
    return state.token;
}

function setToken(token) {
    state.token = token;
    if (token) {
        sessionStorage.setItem(TOKEN_KEY, token);
    } else {
        sessionStorage.removeItem(TOKEN_KEY);
    }
}

function getUser() {
    if (!state.user) {
        const userStr = sessionStorage.getItem(USER_KEY);
        if (userStr) {
            try {
                state.user = JSON.parse(userStr);
            } catch {
                sessionStorage.removeItem(USER_KEY);
            }
        }
    }
    return state.user;
}

function setUser(user) {
    state.user = user;
    if (user) {
        sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    } else {
        sessionStorage.removeItem(USER_KEY);
    }
}

function clearAuth() {
    setToken(null);
    setUser(null);
    state.user = null;
}

function isAuthenticated() {
    return !!getToken();
}

function getUserRole() {
    const user = getUser();
    return user?.rol || null;
}

function hasRole(...roles) {
    const role = getUserRole();
    return roles.includes(role);
}

// ========================================
// API Client
// ========================================

async function apiRequest(url, options = {}) {
    const token = getToken();
    
    const defaultHeaders = {
        'Content-Type': 'application/json',
    };
    
    if (token) {
        defaultHeaders['Authorization'] = `Bearer ${token}`;
    }
    
    const config = {
        headers: { ...defaultHeaders, ...options.headers },
        ...options,
    };
    
    try {
        const response = await fetch(url, config);
        
        if (response.status === 401) {
            clearAuth();
            window.location.href = 'login.html';
            throw new Error('Sesión expirada');
        }
        
        if (response.status === 403) {
            throw new Error('No tiene permisos para realizar esta acción');
        }
        
        if (response.status === 404) {
            throw new Error('Recurso no encontrado');
        }
        
        const contentType = response.headers.get('content-type');
        let data;
        
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else if (contentType && contentType.includes('application/pdf')) {
            data = await response.blob();
        } else {
            data = await response.text();
        }
        
        if (!response.ok) {
            const message = data?.message || data?.error || `Error ${response.status}`;
            throw new Error(message);
        }
        
        return data;
    } catch (error) {
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            throw new Error('No se puede conectar al servidor');
        }
        throw error;
    }
}

// ========================================
// Login Functions
// ========================================

async function login(username, password) {
    const response = await apiRequest(ENDPOINTS.login, {
        method: 'POST',
        body: JSON.stringify({ username, password }),
    });
    
    const { token, userId, username: userName, email, rol } = response;
    
    setToken(token);
    setUser({ id: userId, username: userName, email, rol });
    
    return response;
}

function logout() {
    clearAuth();
    window.location.href = 'login.html';
}

// ========================================
// Data Fetching
// ========================================

async function fetchKPIs() {
    const data = await apiRequest(ENDPOINTS.kpis);
    state.data.kpis = data;
    return data;
}

async function fetchProductosRiesgo() {
    const data = await apiRequest(ENDPOINTS.productosRiesgo);
    state.data.productosRiesgo = data || [];
    return state.data.productosRiesgo;
}

async function fetchOrdenesBorrador() {
    const data = await apiRequest(ENDPOINTS.ordenesBorrador);
    state.data.ordenesBorrador = data || [];
    return state.data.ordenesBorrador;
}

async function fetchResumen() {
    try {
        const data = await apiRequest(ENDPOINTS.resumen);
        state.data.resumen = data;
        return data;
    } catch (error) {
        if (error.message.includes('404')) {
            state.data.resumen = null;
            return null;
        }
        throw error;
    }
}

async function fetchOcupacion() {
    try {
        const data = await apiRequest(ENDPOINTS.ocupacion);
        state.data.ocupacion = data || [];
        return state.data.ocupacion;
    } catch {
        state.data.ocupacion = [];
        return [];
    }
}

async function fetchAllDashboardData() {
    await Promise.allSettled([
        fetchKPIs(),
        fetchProductosRiesgo(),
        fetchOrdenesBorrador(),
        fetchResumen(),
        fetchOcupacion(),
    ]);
}

// ========================================
// PDF Functions
// ========================================

async function generateOrderPdf(orderId) {
    const blob = await apiRequest(ENDPOINTS.ordenPdf(orderId), {
        method: 'POST',
    });
    return blob;
}

async function viewOrderPdf(orderId) {
    const blob = await apiRequest(ENDPOINTS.ordenPdf(orderId), {
        method: 'GET',
    });
    return blob;
}

function openPdfInModal(blob, orderId) {
    const url = URL.createObjectURL(blob);
    const frame = document.getElementById('pdfFrame');
    const modal = document.getElementById('pdfModal');
    const title = document.getElementById('pdfOrderId');
    const downloadBtn = document.getElementById('downloadPdfBtn');
    
    frame.src = url;
    title.textContent = `#${orderId}`;
    
    downloadBtn.onclick = () => {
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
    
    modal.classList.add('hidden');
    frame.src = 'about:blank';
    document.body.style.overflow = '';
    
    // Clean up object URL after a delay
    setTimeout(() => URL.revokeObjectURL(frame.src), 100);
}

// ========================================
// Order Actions
// ========================================

async function aprobarOrden(orderId) {
    const data = await apiRequest(ENDPOINTS.ordenEstado(orderId), {
        method: 'PATCH',
        body: JSON.stringify({ estado: 'APROBADA' }),
    });
    return data;
}

// ========================================
// UI Rendering Functions
// ========================================

function renderKPIs() {
    const kpis = state.data.kpis;
    
    if (!kpis) {
        ['kpiProductosRiesgo', 'kpiProductosQuiebre', 'kpiOrdenesAprobar', 'kpiBodegasCriticas']
            .forEach(id => {
                const el = document.getElementById(id);
                if (el) el.textContent = '--';
            });
        return;
    }
    
    const elRiesgo = document.getElementById('kpiProductosRiesgo');
    const elQuiebre = document.getElementById('kpiProductosQuiebre');
    const elOrdenes = document.getElementById('kpiOrdenesAprobar');
    const elBodegas = document.getElementById('kpiBodegasCriticas');
    
    if (elRiesgo) elRiesgo.textContent = formatNumber(kpis.productosEnRiesgo);
    if (elQuiebre) elQuiebre.textContent = formatNumber(kpis.productosEnQuiebre);
    
    if (elOrdenes) {
        const ordenes = kpis.ordenesPorAprobar || {};
        elOrdenes.textContent = `${formatNumber(ordenes.cantidad)} (${formatCurrency(ordenes.montoTotal)})`;
    }
    
    if (elBodegas) elBodegas.textContent = formatNumber(
        kpis.ocupacionPorBodega?.filter(b => b.porcentaje >= 90).length || 0
    );
}

function renderMovimientosAyer() {
    const kpis = state.data.kpis;
    const movimientos = kpis?.movimientosAyer || {};
    
    const elEntradas = document.getElementById('movEntradas');
    const elSalidas = document.getElementById('movSalidas');
    const elTransferencias = document.getElementById('movTransferencias');
    const elDate = document.getElementById('ayerDate');
    
    if (elEntradas) elEntradas.textContent = formatNumber(movimientos.entrada);
    if (elSalidas) elSalidas.textContent = formatNumber(movimientos.salida);
    if (elTransferencias) elTransferencias.textContent = formatNumber(movimientos.transferencia);
    
    if (elDate && kpis?.calculadoEn) {
        const date = new Date(kpis.calculadoEn);
        const ayer = new Date(date);
        ayer.setDate(ayer.getDate() - 1);
        elDate.textContent = ayer.toLocaleDateString('es-CO', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
        });
    }
}

function renderOcupacion() {
    const tbody = document.getElementById('ocupacionBody');
    if (!tbody) return;
    
    const ocupacion = state.data.ocupacion || [];
    
    if (ocupacion.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="empty-state" style="padding: 32px;">
                    <p>No hay datos de ocupación disponibles</p>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = ocupacion.map(b => {
        const porcentaje = b.porcentajeOcupacion || 0;
        const isCritica = porcentaje >= 90;
        const badgeClass = isCritica ? 'badge-danger' : (porcentaje >= 70 ? 'badge-warning' : 'badge-success');
        
        return `
            <tr>
                <td><strong>${b.bodegaNombre || `Bodega ${b.bodegaId}`}</strong></td>
                <td>${formatNumber(b.stockTotal)}</td>
                <td>${formatNumber(b.capacidad)}</td>
                <td>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <div style="flex: 1; height: 8px; background: var(--color-bg); border-radius: 4px; overflow: hidden;">
                            <div style="width: ${Math.min(porcentaje, 100)}%; height: 100%; background: ${isCritica ? 'var(--color-danger)' : (porcentaje >= 70 ? 'var(--color-warning)' : 'var(--color-success)')}; transition: width 0.3s ease;"></div>
                        </div>
                        <span class="badge ${badgeClass}" style="font-size: 11px; white-space: nowrap;">${porcentaje.toFixed(1)}%</span>
                    </div>
                </td>
                <td><span class="badge ${badgeClass}">${isCritica ? 'CRÍTICA' : (porcentaje >= 70 ? 'ALTA' : 'NORMAL')}</span></td>
            </tr>
        `;
    }).join('');
}

function renderResumen() {
    const container = document.getElementById('resumenContent');
    const fechaEl = document.getElementById('resumenFecha');
    const resumen = state.data.resumen;
    
    if (!container) return;
    
    if (!resumen) {
        container.innerHTML = `
            <div class="empty-state">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                    <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
                <p>No hay resumen publicado para hoy</p>
                <small>El flujo n8n publica automáticamente a las 6:00 AM</small>
            </div>
        `;
        if (fechaEl) fechaEl.textContent = 'Sin publicar';
        return;
    }
    
    if (fechaEl) {
        fechaEl.textContent = formatDateOnly(resumen.fecha);
        fechaEl.className = 'badge badge-info';
    }
    
    let alertasHtml = '';
    if (resumen.alertas && resumen.alertas.length > 0) {
        alertasHtml = resumen.alertas.map(a => `
            <div class="alerta-item">
                <div class="alerta-header">
                    <span class="badge ${getSeverityClass(a.severidad)}">${a.severidad}</span>
                    <strong>${a.titulo}</strong>
                </div>
                <p class="alerta-detalle">${a.detalle}</p>
                <div class="alerta-refs">
                    ${a.productoId ? `<span class="ref">Producto: #${a.productoId}</span>` : ''}
                    ${a.ordenId ? `<span class="ref">Orden: #${a.ordenId}</span>` : ''}
                    ${a.bodegaId ? `<span class="ref">Bodega: #${a.bodegaId}</span>` : ''}
                </div>
            </div>
        `).join('');
    }
    
    let accionesHtml = '';
    if (resumen.accionesSugeridas && resumen.accionesSugeridas.length > 0) {
        accionesHtml = resumen.accionesSugeridas.map(a => `
            <div class="accion-item">
                <span class="badge ${getAccionClass(a.tipo)}">${a.tipo}</span>
                <span>${a.descripcion}</span>
                ${a.ordenId ? `<span class="ref">Orden: #${a.ordenId}</span>` : ''}
                ${a.productoId ? `<span class="ref">Producto: #${a.productoId}</span>` : ''}
                ${a.bodegaId ? `<span class="ref">Bodega: #${a.bodegaId}</span>` : ''}
            </div>
        `).join('');
    }
    
    container.innerHTML = `
        <div class="resumen-card">
            <div class="resumen-narrativa">
                <h4>Narrativa</h4>
                <p>${resumen.narrativa}</p>
            </div>
            ${alertasHtml ? `
                <div class="resumen-section">
                    <h4>Alertas</h4>
                    <div class="alertas-list">${alertasHtml}</div>
                </div>
            ` : ''}
            ${accionesHtml ? `
                <div class="resumen-section">
                    <h4>Acciones Sugeridas</h4>
                    <div class="acciones-list">${accionesHtml}</div>
                </div>
            ` : ''}
        </div>
    `;
}

function renderOrdenesBorrador() {
    const tbody = document.getElementById('ordenesBody');
    const emptyEl = document.getElementById('ordenesEmpty');
    const table = document.getElementById('ordenesTable');
    const ordenes = state.data.ordenesBorrador || [];
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
    
    tbody.innerHTML = ordenes.map(o => `
        <tr data-id="${o.id}">
            <td><strong>#${o.id}</strong></td>
            <td>${o.producto?.nombre || `Producto #${o.productoId}`}</td>
            <td>${o.proveedor?.nombre || (o.proveedorId ? `#${o.proveedorId}` : '—')}</td>
            <td>${o.bodegaDestino?.nombre || `Bodega #${o.bodegaDestinoId}`}</td>
            <td>${formatNumber(o.cantidad)}</td>
            <td>${formatCurrency(o.precioUnitario)}</td>
            <td><strong>${formatCurrency(o.total)}</strong></td>
            <td>${formatDate(o.fechaCreacion)}</td>
            <td><span class="badge ${getEstadoClass(o.estado)}">${o.estado}</span></td>
            <td>
                <div class="actions-cell">
                    <button class="btn btn-ghost btn-sm btn-pdf" data-id="${o.id}" title="Ver PDF">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                            <polyline points="14 2 14 8 20 8"></polyline>
                        </svg>
                    </button>
                    ${isAdmin && o.estado === 'BORRADOR' ? `
                        <button class="btn btn-ghost btn-sm btn-aprobar" data-id="${o.id}" title="Aprobar">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        </button>
                    ` : ''}
                </div>
            </td>
        </tr>
    `).join('');
    
    // Add event listeners
    tbody.querySelectorAll('.btn-pdf').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const id = parseInt(btn.dataset.id);
            handleViewPdf(id);
        });
    });
    
    tbody.querySelectorAll('.btn-aprobar').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const id = parseInt(btn.dataset.id);
            handleAprobarOrden(id);
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
    
    tbody.innerHTML = productos.map(p => `
        <tr>
            <td><strong>${p.nombre}</strong></td>
            <td>${p.proveedor || '—'}</td>
            <td>${formatNumber(p.stockTotal)}</td>
            <td>${p.consumoDiarioPromedio ? p.consumoDiarioPromedio.toFixed(2) : '—'}</td>
            <td>${p.puntoReorden ? Math.ceil(p.puntoReorden) : '—'}</td>
            <td>${p.diasCobertura ? p.diasCobertura.toFixed(1) : (p.estado === 'SIN_CONSUMO' ? 'SIN_CONSUMO' : '—')}</td>
            <td><span class="badge ${getEstadoClass(p.estado)}">${p.estado}</span></td>
            <td>${p.bodegaDestinoId ? `#${p.bodegaDestinoId}` : '—'}</td>
            <td>
                <button class="btn btn-ghost btn-sm btn-pdf" data-id="${p.productoId}" title="Ver stock">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="16"></line>
                        <line x1="8" y1="12" x2="16" y2="12"></line>
                    </svg>
                </button>
            </td>
        </tr>
    `).join('');
}

function renderBodegas() {
    const tbody = document.getElementById('bodegasBody');
    if (!tbody) return;
    
    const ocupacion = state.data.ocupacion || [];
    
    if (ocupacion.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-state" style="padding: 32px;">
                    <p>No hay datos de bodegas disponibles</p>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = ocupacion.map(b => {
        const porcentaje = b.porcentajeOcupacion || 0;
        const isCritica = porcentaje >= 90;
        const badgeClass = isCritica ? 'badge-danger' : (porcentaje >= 70 ? 'badge-warning' : 'badge-success');
        
        return `
            <tr>
                <td><strong>${b.bodegaNombre || `Bodega ${b.bodegaId}`}</strong></td>
                <td>—</td>
                <td>${formatNumber(b.stockTotal)}</td>
                <td>${formatNumber(b.capacidad)}</td>
                <td>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <div style="flex: 1; height: 8px; background: var(--color-bg); border-radius: 4px; overflow: hidden;">
                            <div style="width: ${Math.min(porcentaje, 100)}%; height: 100%; background: ${isCritica ? 'var(--color-danger)' : (porcentaje >= 70 ? 'var(--color-warning)' : 'var(--color-success)')}; transition: width 0.3s ease;"></div>
                        </div>
                        <span class="badge ${badgeClass}" style="font-size: 11px; white-space: nowrap;">${porcentaje.toFixed(1)}%</span>
                    </div>
                </td>
                <td><span class="badge ${badgeClass}">${isCritica ? 'CRÍTICA' : (porcentaje >= 70 ? 'ALTA' : 'NORMAL')}</span></td>
            </tr>
        `;
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
    
    if (!username || !password) {
        showError(errorEl, 'Ingrese usuario y contraseña');
        return;
    }
    
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

function showError(el, message) {
    el.textContent = message;
    el.classList.remove('hidden');
}

async function handleViewPdf(orderId) {
    const btn = document.querySelector(`.btn-pdf[data-id="${orderId}"]`);
    if (!btn) return;
    
    const originalHtml = btn.innerHTML;
    btn.innerHTML = '<span class="btn-loader"></span>';
    btn.disabled = true;
    
    try {
        const blob = await viewOrderPdf(orderId);
        openPdfInModal(blob, orderId);
    } catch (error) {
        alert(error.message);
    } finally {
        btn.innerHTML = originalHtml;
        btn.disabled = false;
    }
}

async function handleAprobarOrden(orderId) {
    const confirmed = await showConfirm(
        'Aprobar Orden',
        `¿Está seguro de aprobar la orden #${orderId}? Esta acción no se puede deshacer.`
    );
    
    if (!confirmed) return;
    
    try {
        await aprobarOrden(orderId);
        showToast('Orden aprobada correctamente', 'success');
        await refreshDashboard();
    } catch (error) {
        alert(error.message);
    }
}

function showConfirm(title, message) {
    return new Promise((resolve) => {
        const modal = document.getElementById('confirmModal');
        const titleEl = document.getElementById('confirmTitle');
        const messageEl = document.getElementById('confirmMessage');
        const okBtn = document.getElementById('confirmOk');
        const cancelBtn = document.getElementById('confirmCancel');
        
        titleEl.textContent = title;
        messageEl.textContent = message;
        
        modal.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
        
        const cleanup = () => {
            modal.classList.add('hidden');
            document.body.style.overflow = '';
            okBtn.removeEventListener('click', onOk);
            cancelBtn.removeEventListener('click', onCancel);
        };
        
        const onOk = () => {
            cleanup();
            resolve(true);
        };
        
        const onCancel = () => {
            cleanup();
            resolve(false);
        };
        
        okBtn.addEventListener('click', onOk);
        cancelBtn.addEventListener('click', onCancel);
        
        // Close on overlay click
        modal.querySelector('.modal-overlay').onclick = () => {
            cleanup();
            resolve(false);
        };
    });
}

function showToast(message, type = 'info') {
    // Simple toast implementation
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        bottom: 24px;
        right: 24px;
        padding: 12px 20px;
        background: ${type === 'success' ? 'var(--color-success)' : type === 'error' ? 'var(--color-danger)' : 'var(--color-primary)'};
        color: white;
        border-radius: var(--radius);
        box-shadow: var(--shadow-lg);
        z-index: 300;
        animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
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
    renderOrdenesBorrador();
    renderProductosRiesgo();
    renderBodegas();
}

// ========================================
// Page Navigation
// ========================================

function navigateTo(page) {
    // Update nav items
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.toggle('active', item.dataset.page === page);
    });
    
    // Update pages
    document.querySelectorAll('.page').forEach(p => {
        p.classList.toggle('active', p.id === `page-${page}`);
    });
    
    // Update title
    const titles = {
        dashboard: 'Dashboard',
        ordenes: 'Órdenes de Compra',
        productos: 'Productos en Riesgo',
        bodegas: 'Bodegas',
    };
    const titleEl = document.getElementById('pageTitle');
    if (titleEl) titleEl.textContent = titles[page] || 'Dashboard';
    
    state.currentPage = page;
    
    // Close sidebar on mobile
    document.getElementById('sidebar')?.classList.remove('open');
}

// ========================================
// Initialization
// ========================================

function initLogin() {
    // Check if already logged in
    if (isAuthenticated()) {
        window.location.href = 'dashboard.html';
        return;
    }
    
    const form = document.getElementById('loginForm');
    if (form) {
        form.addEventListener('submit', handleLogin);
    }
}

function initDashboard() {
    // Check auth
    if (!isAuthenticated()) {
        window.location.href = 'login.html';
        return;
    }
    
    // Set user info in header
    const user = getUser();
    if (user) {
        const nameEl = document.getElementById('userName');
        const roleEl = document.getElementById('userRole');
        if (nameEl) nameEl.textContent = user.username;
        if (roleEl) {
            roleEl.textContent = user.rol;
            roleEl.className = `role-badge ${user.rol}`;
        }
    }
    
    // Logout button
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', logout);
    }
    
    // Menu toggle
    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.getElementById('sidebar');
    if (menuToggle && sidebar) {
        menuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }
    
    // Close sidebar on overlay click (mobile)
    document.addEventListener('click', (e) => {
        if (window.innerWidth <= 768) {
            if (!sidebar.contains(e.target) && !menuToggle.contains(e.target)) {
                sidebar.classList.remove('open');
            }
        }
    });
    
    // Navigation
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(item.dataset.page);
        });
    });
    
    // PDF Modal close
    const closePdfModalBtn = document.getElementById('closePdfModal');
    const closePdfBtn = document.getElementById('closePdfBtn');
    const pdfModal = document.getElementById('pdfModal');
    
    [closePdfModalBtn, closePdfBtn].forEach(btn => {
        if (btn) btn.addEventListener('click', closePdfModal);
    });
    
    if (pdfModal) {
        pdfModal.querySelector('.modal-overlay').addEventListener('click', closePdfModal);
    }
    
    // Initial load
    loadDashboard();
}

async function loadDashboard() {
    try {
        showLoading(true);
        await fetchAllDashboardData();
        renderDashboard();
    } catch (error) {
        console.error('Error loading dashboard:', error);
        if (error.message.includes('Sesión expirada') || error.message.includes('401')) {
            window.location.href = 'login.html';
        }
    } finally {
        showLoading(false);
    }
}

function showLoading(show) {
    // Could add a global loading indicator here
    document.body.classList.toggle('loading', show);
}

// ========================================
// Add toast styles dynamically
// ========================================

const toastStyles = document.createElement('style');
toastStyles.textContent = `
@keyframes slideIn {
    from { transform: translateX(100%); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
}
@keyframes slideOut {
    from { transform: translateX(0); opacity: 1; }
    to { transform: translateX(100%); opacity: 0; }
}
.toast {
    animation: slideIn 0.3s ease;
}
`;
document.head.appendChild(toastStyles);

// ========================================
// Alerta/Accion Styles
// ========================================

const extraStyles = document.createElement('style');
extraStyles.textContent = `
.resumen-card {
    padding: 16px;
}
.resumen-narrativa h4,
.resumen-section h4 {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 8px;
}
.resumen-narrativa p {
    color: var(--color-text);
    line-height: 1.6;
}
.resumen-section {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid var(--color-border);
}
.alertas-list,
.acciones-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}
.alerta-item {
    padding: 12px;
    background: var(--color-bg);
    border-radius: var(--radius);
    border-left: 3px solid var(--color-danger);
}
.alerta-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
}
.alerta-detalle {
    font-size: 13px;
    color: var(--color-text);
    margin: 0;
}
.alerta-refs {
    display: flex;
    gap: 8px;
    margin-top: 8px;
}
.alerta-refs .ref {
    font-size: 11px;
    color: var(--color-text-muted);
}
.accion-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    background: var(--color-bg);
    border-radius: var(--radius);
}
.accion-item .ref {
    font-size: 11px;
    color: var(--color-text-muted);
}
.actions-cell {
    display: flex;
    gap: 4px;
}
.btn-loading {
    position: relative;
    color: transparent !important;
}
.btn-loading .btn-loader {
    display: block !important;
}
`;
document.head.appendChild(extraStyles);

// ========================================
// Export for global access
// ========================================

window.LogiTrack = {
    apiRequest,
    login,
    logout,
    getToken,
    getUser,
    getUserRole,
    hasRole,
    fetchKPIs,
    fetchProductosRiesgo,
    fetchOrdenesBorrador,
    fetchResumen,
    fetchOcupacion,
    generateOrderPdf,
    viewOrderPdf,
    aprobarOrden,
    navigateTo,
    state,
};