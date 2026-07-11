export const API = import.meta.env.VITE_API_BASE || '/api';
export const YEN = '¥';
export const CNY = '￥';

export async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
  const response = await fetch(`${API}${path}`, {
    headers,
    ...options,
  });
  if (!response.ok) throw new Error(await response.text());
  return response.json();
}

export function money(value, unit = CNY) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  return `${unit}${Number(value).toFixed(2)}`;
}

export function pct(value) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`;
}

export function regionOf(product) {
  return `${product.sourcePlatform || ''} ${product.sourceUrl || ''}`.toLowerCase().match(/jp|japan|co\.jp|tiktok/) ? '日本' : '未知';
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
