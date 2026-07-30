import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const component = (name) => new URL(`../src/components/${name}`, import.meta.url);

test('keeps the original Japan dashboard copy while providing English market copy', async () => {
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

  for (const text of ['Cross-border Intelligence', 'Product Discovery', 'Product Opportunity Radar']) {
    assert.ok(report.includes(text), `missing English market copy: ${text}`);
  }
});
