export const API = import.meta.env.VITE_API_BASE || '/api';
export const YEN = '¥';
export const CNY = '￥';

export class ApiError extends Error {
  constructor(message, status = 0, payload = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

export async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(`${API}${path}`, { headers, ...options });
  } catch (error) {
    throw new ApiError('网络连接失败，请确认前后端服务已启动', 0, error);
  }
  const text = await response.text();
  let payload = null;
  try { payload = text ? JSON.parse(text) : null; } catch { payload = null; }
  if (!response.ok) throw new ApiError(payload?.message || payload?.detail || text || `请求失败（${response.status}）`, response.status, payload);
  return payload;
}

export function money(value, unit = CNY) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  return `${unit}${Number(value).toFixed(2)}`;
}

export function currencyMoney(value, currency = 'CNY') {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  const code = /^[A-Z]{3}$/.test(currency || '') ? currency : 'CNY';
  try {
    return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: code, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(value));
  } catch {
    return `${code} ${Number(value).toFixed(2)}`;
  }
}

export function pct(value) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`;
}

export function regionOf(product) {
  return `${product.sourcePlatform || ''} ${product.sourceUrl || ''}`.toLowerCase().match(/jp|japan|co\.jp|tiktok/) ? '日本' : '未知';
}

export function isDemoProduct(product) {
  return `${product?.sourcePlatform || ''}`.toLowerCase().includes('demo');
}

export function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(date);
}

export function searchableText(product) {
  return [
    product.productNameCn,
    product.productNameJp,
    product.keywords,
    product.category,
    product.sourcePlatform,
    product.reason,
    ...(product.domesticLinks || []).map((item) => `${item.platform} ${item.title} ${item.note}`),
  ].filter(Boolean).join(' ').toLowerCase();
}

export function bestLink(product) {
  return (product.domesticLinks || []).slice().sort((a, b) => Number(a.priceCny || 0) - Number(b.priceCny || 0))[0];
}

export function splitText(value) {
  return (value || '').split(/[，,\n]/).map((item) => item.trim()).filter(Boolean);
}

export function joinText(value) {
  return (value || []).join(', ');
}
