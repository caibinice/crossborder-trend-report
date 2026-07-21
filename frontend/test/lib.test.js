import assert from 'node:assert/strict';
import { afterEach, test } from 'node:test';
import { api } from '../src/lib.js';

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
});

test('keeps authorization and adds JSON content type for request bodies', async () => {
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } });
  };

  await api('/admin/settings', {
    method: 'PUT',
    headers: { Authorization: 'Bearer test-token' },
    body: JSON.stringify({ sourceMode: 'external' }),
  });

  const headers = new Headers(request.options.headers);
  assert.equal(request.url, '/api/admin/settings');
  assert.equal(headers.get('Authorization'), 'Bearer test-token');
  assert.equal(headers.get('Content-Type'), 'application/json');
});

test('preserves an explicitly supplied content type', async () => {
  let requestHeaders;
  globalThis.fetch = async (_url, options) => {
    requestHeaders = new Headers(options.headers);
    return new Response('{}', { status: 200 });
  };

  await api('/plain', { method: 'POST', headers: { 'content-type': 'text/plain' }, body: 'value' });

  assert.equal(requestHeaders.get('Content-Type'), 'text/plain');
});
