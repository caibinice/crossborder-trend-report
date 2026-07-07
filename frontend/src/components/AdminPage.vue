<template>
  <section v-if="!token" class="admin-card">
    <h1>后台管理登录</h1><p>默认账号密码：admin / admin</p>
    <form class="login" @submit.prevent="doLogin"><label>账号<input v-model="login.username" /></label><label>密码<input type="password" v-model="login.password" /></label><button>登录</button></form>
    <p v-if="message" class="notice">{{ message }}</p>
  </section>
  <section v-else-if="!settings" class="admin-card">加载后台配置中...</section>
  <section v-else class="admin">
    <div class="admin-head"><div><p class="eyebrow">Admin Console</p><h1>数据源与检索配置</h1><p>配置国内/国外数据源、商品检索品类、区域、检索频率和利润估算参数。</p></div><button class="ghost" @click="logout">退出登录</button></div>
    <section class="admin-grid">
      <label>国外检索数据源<textarea rows="4" :value="joinText(settings.foreignSources)" @input="settings.foreignSources = splitText($event.target.value)" /><small>例如：TikTok/Apify, Amazon/Rainforest, Amazon/Keepa</small></label>
      <label>国内检索数据源<textarea rows="4" :value="joinText(settings.domesticSources)" @input="settings.domesticSources = splitText($event.target.value)" /><small>例如：1688, Taobao, Pinduoduo</small></label>
      <label>商品检索品类<textarea rows="5" :value="joinText(settings.categories)" @input="settings.categories = splitText($event.target.value)" /><small>逗号或换行分隔；报表会按这些品类过滤。</small></label>
      <label>检索区域<textarea rows="5" :value="joinText(settings.regions)" @input="settings.regions = splitText($event.target.value)" /><small>例如：日本、美国、东南亚；当前 demo 主要生成日本。</small></label>
      <label>检索频率 Cron<input v-model="settings.frequencyCron" /><small>Spring cron，例如每天 08:30：0 30 8 * * *</small></label>
      <label>每日报表商品数<input type="number" v-model.number="settings.maxProducts" /></label>
      <label>日元汇率<input type="number" step="0.001" v-model="settings.jpyCnyRate" /></label>
      <label>默认物流成本<input type="number" v-model="settings.defaultShippingCny" /></label>
      <label class="check"><input type="checkbox" v-model="settings.smartMode" />启用智能模式：自动按利润和热度综合排序</label>
    </section>
    <button @click="save">保存配置</button><p v-if="message" class="notice">{{ message }}</p>
    <h2>数据源接入状态</h2>
    <section class="source-list"><article v-for="source in sources" :key="source.name"><strong>{{ source.name }}</strong><span :class="source.configured ? 'ok' : 'warn'">{{ source.configured ? '已配置' : '未配置' }}</span><p>{{ source.useCase }}</p><a :href="source.docsUrl" target="_blank" rel="noreferrer">查看文档</a><small>{{ source.note }}</small></article></section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { api, joinText, splitText } from '../lib.js';

const token = ref(localStorage.getItem('adminToken') || '');
const login = reactive({ username: 'admin', password: 'admin' });
const settings = ref(null);
const sources = ref([]);
const message = ref('');

async function adminApi(path, options = {}) {
  return api(path, { ...options, headers: { ...(options.headers || {}), Authorization: `Bearer ${token.value}` } });
}
async function doLogin() {
  message.value = '';
  try {
    const data = await api('/admin/login', { method: 'POST', body: JSON.stringify(login) });
    token.value = data.token;
    localStorage.setItem('adminToken', data.token);
    message.value = '登录成功';
    await load();
  } catch (error) { message.value = '账号或密码错误'; }
}
async function load() {
  if (!token.value) return;
  const [settingsData, sourceData] = await Promise.all([adminApi('/admin/settings'), api('/datasources')]);
  settings.value = settingsData;
  sources.value = sourceData;
}
async function save() {
  try {
    settings.value = await adminApi('/admin/settings', { method: 'PUT', body: JSON.stringify(settings.value) });
    message.value = '配置已保存，动态定时任务会按新 Cron 触发；点击报表页按钮可立即按新配置生成。';
  } catch (error) { message.value = error.message; }
}
function logout() {
  localStorage.removeItem('adminToken');
  token.value = '';
  settings.value = null;
}
onMounted(() => load().catch((error) => { message.value = error.message; }));
</script>
