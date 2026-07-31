import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const component = (name) => new URL(`../src/components/${name}`, import.meta.url);

test('uses Chinese dashboard copy for all markets and keeps translated and original product names', async () => {
  const [report, card, table, theme] = await Promise.all([
    readFile(component('ReportPage.vue'), 'utf8'),
    readFile(component('ProductCard.vue'), 'utf8'),
    readFile(component('ProductTable.vue'), 'utf8'),
    readFile(component('ThemeToggle.vue'), 'utf8'),
  ]);

  for (const text of [
    '跨境趋势情报',
    '市场雷达',
    '选品驾驶舱',
    '商品机会雷达',
    '等待首份真实商品日报',
    '采集最新商品',
    '验证敏感操作',
  ]) {
    assert.ok(report.includes(text), `missing original Japan copy: ${text}`);
  }
  for (const text of ['真实目录', '销量指数', '源站售价', '中文采购搜索']) {
    assert.ok(card.includes(text), `missing original Japan product copy: ${text}`);
  }
  assert.ok(table.includes('没有匹配商品'));
  assert.ok(theme.includes('切换浅色模式'));

  assert.ok(report.includes("name: '美国市场'"));
  assert.ok(report.includes("name: '东南亚市场'"));
  assert.ok(report.includes('function tr(chinese) { return chinese; }'));
  assert.ok(card.includes('product.productNameCn'));
  assert.ok(card.includes('product.productNameJp'));
  assert.ok(card.includes('原始商品名：'));
});

test('loads product thumbnails eagerly for mobile browsers', async () => {
  const [card, styles] = await Promise.all([
    readFile(component('ProductCard.vue'), 'utf8'),
    readFile(new URL('../src/style.css', import.meta.url), 'utf8'),
  ]);

  assert.ok(card.includes('loading="eager"'));
  assert.ok(card.includes('referrerpolicy="no-referrer"'));
  assert.match(styles, /\.product-media img \{[^}]*display: block;/);
});
