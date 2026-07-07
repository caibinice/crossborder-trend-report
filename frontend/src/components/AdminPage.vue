
<template>
  <section v-if="!token" class="admin-login-page">
    <form class="admin-login-card" @submit.prevent="doLogin">
      <h1>若依管理后台</h1>
      <p>跨境选品系统 · 默认账号 admin / admin</p>
      <label>账号<input v-model="login.username" /></label>
      <label>密码<input type="password" v-model="login.password" /></label>
      <button>登录</button>
      <p v-if="message" class="notice">{{ message }}</p>
    </form>
  </section>
  <section v-else class="ruoyi-layout">
    <aside class="ruoyi-sidebar">
      <div class="ruoyi-logo">RuoYi Admin</div>
      <nav>
        <button v-for="menu in flatMenus" :key="menu.menuKey" :class="['ruoyi-menu', activeMenu === menu.menuKey ? 'active' : '', menu.parentId === 0 ? 'root' : 'child']" @click="selectMenu(menu)">
          <span>{{ menu.parentId === 0 ? '▸' : '•' }}</span>{{ menu.title }}
        </button>
      </nav>
    </aside>
    <main class="ruoyi-main">
      <header class="ruoyi-topbar">
        <div><strong>{{ activeTitle }}</strong><small>首页 / {{ activeTitle }}</small></div>
        <div class="ruoyi-user"><span>{{ profile?.nickname || '系统管理员' }}</span><button class="ghost" @click="logout">退出</button></div>
      </header>
      <div class="ruoyi-tags"><span>首页</span><span>{{ activeTitle }}</span></div>

      <section v-if="activeMenu === 'dashboard'" class="admin-panel">
        <h2>工作台</h2>
        <div class="admin-kpis"><article><span>用户</span><b>{{ users.length }}</b></article><article><span>角色</span><b>{{ roles.length }}</b></article><article><span>市场</span><b>{{ markets.length }}</b></article><article><span>品类</span><b>{{ categories.length }}</b></article></div>
        <p class="muted">这是按 RuoYi-Vue 风格搭建的管理后台骨架，后续可继续补充部门、岗位、字典、日志、权限等完整若依模块。</p>
      </section>

      <section v-if="activeMenu === 'users'" class="admin-panel"><CrudHeader title="用户管理" @add="newUser"/><DataTable :rows="users" :columns="userColumns"><template #actions="{row}"><button @click="editUser(row)">编辑</button><button class="danger" @click="deleteUser(row)">删除</button></template></DataTable><UserForm v-if="editingUser" :model="editingUser" :roles="roles" @save="saveUser" @cancel="editingUser=null" /></section>
      <section v-if="activeMenu === 'roles'" class="admin-panel"><CrudHeader title="角色管理" @add="newRole"/><DataTable :rows="roles" :columns="roleColumns"><template #actions="{row}"><button @click="editRole(row)">编辑</button><button class="danger" @click="deleteRole(row)">删除</button></template></DataTable><RoleForm v-if="editingRole" :model="editingRole" @save="saveRole" @cancel="editingRole=null" /></section>
      <section v-if="activeMenu === 'menus'" class="admin-panel"><h2>菜单管理</h2><DataTable :rows="flatMenus" :columns="menuColumns" /></section>

      <section v-if="activeMenu === 'sources'" class="admin-panel"><h2>数据源配置</h2><SettingsForm v-if="settings" :settings="settings" section="sources" @save="saveSettings"/><h3>接入状态</h3><div class="source-list"><article v-for="source in sources" :key="source.name"><strong>{{ source.name }}</strong><span :class="source.configured ? 'ok' : 'warn'">{{ source.configured ? '已配置' : '未配置' }}</span><p>{{ source.useCase }}</p><a :href="source.docsUrl" target="_blank">查看文档</a><small>{{ source.note }}</small></article></div></section>
      <section v-if="activeMenu === 'markets'" class="admin-panel"><CrudHeader title="市场配置" @add="newMarket"/><DataTable :rows="markets" :columns="marketColumns"><template #actions="{row}"><button @click="editMarket(row)">编辑</button></template></DataTable><MarketForm v-if="editingMarket" :model="editingMarket" @save="saveMarket" @cancel="editingMarket=null" /></section>
      <section v-if="activeMenu === 'categories'" class="admin-panel"><CrudHeader title="品类配置" @add="newCategory"/><DataTable :rows="categories" :columns="categoryColumns"><template #actions="{row}"><button @click="editCategory(row)">编辑</button></template></DataTable><CategoryForm v-if="editingCategory" :model="editingCategory" :markets="markets" @save="saveCategory" @cancel="editingCategory=null" /></section>
      <section v-if="activeMenu === 'schedules'" class="admin-panel"><h2>采集频率配置</h2><SettingsForm v-if="settings" :settings="settings" section="schedule" @save="saveSettings"/></section>

      <section v-if="activeMenu === 'dailyReports'" class="admin-panel"><h2>日报记录</h2><DataTable :rows="reports" :columns="reportColumns" /></section>
      <section v-if="activeMenu === 'productPool'" class="admin-panel"><h2>商品池</h2><DataTable :rows="productPool" :columns="productColumns" /></section>
      <section v-if="!knownMenu" class="admin-panel"><h2>{{ activeTitle }}</h2><p class="muted">该模块为若依菜单占位，后续可继续扩展。</p></section>
      <p v-if="message" class="notice">{{ message }}</p>
    </main>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue';
import { api, joinText, splitText } from '../lib.js';

const token = ref(localStorage.getItem('adminToken') || '');
const login = reactive({ username: 'admin', password: 'admin' });
const message = ref('');
const profile = ref(null);
const menus = ref([]);
const users = ref([]);
const roles = ref([]);
const markets = ref([]);
const categories = ref([]);
const settings = ref(null);
const sources = ref([]);
const reports = ref([]);
const activeMenu = ref('dashboard');
const editingUser = ref(null), editingRole = ref(null), editingMarket = ref(null), editingCategory = ref(null);
const userColumns = [{key:'id',label:'ID'},{key:'username',label:'用户名'},{key:'nickname',label:'昵称'},{key:'roleKey',label:'角色'},{key:'status',label:'状态'},{key:'email',label:'邮箱'}];
const roleColumns = [{key:'id',label:'ID'},{key:'roleKey',label:'角色标识'},{key:'roleName',label:'角色名称'},{key:'status',label:'状态'},{key:'remark',label:'备注'}];
const menuColumns = [{key:'id',label:'ID'},{key:'title',label:'菜单名称'},{key:'menuKey',label:'标识'},{key:'path',label:'路径'},{key:'component',label:'组件'},{key:'status',label:'状态'}];
const marketColumns = [{key:'id',label:'ID'},{key:'marketKey',label:'市场标识'},{key:'marketName',label:'市场名称'},{key:'region',label:'区域'},{key:'enabled',label:'启用'},{key:'note',label:'说明'}];
const categoryColumns = [{key:'id',label:'ID'},{key:'categoryName',label:'品类'},{key:'marketKey',label:'市场'},{key:'enabled',label:'启用'},{key:'keywords',label:'关键词'},{key:'note',label:'说明'}];
const reportColumns = [{key:'reportDate',label:'日期'},{key:'title',label:'标题'},{key:'sourceMode',label:'数据源'},{key:'summary',label:'摘要'}];
const productColumns = [{key:'rank',label:'排名'},{key:'productNameCn',label:'商品'},{key:'category',label:'品类'},{key:'heatScore',label:'热度'},{key:'estimatedProfitCny',label:'利润'}];
const flatMenus = computed(() => flattenMenus(menus.value));
const activeTitle = computed(() => flatMenus.value.find(m => m.menuKey === activeMenu.value)?.title || '工作台');
const knownMenu = computed(() => ['dashboard','users','roles','menus','sources','markets','categories','schedules','dailyReports','productPool'].includes(activeMenu.value));
const productPool = computed(() => (reports.value[0]?.products || []).map(p => ({...p, rank: `#${p.rank}`})));
function flattenMenus(items){ return items.flatMap(m => [m, ...(m.children ? flattenMenus(m.children) : [])]); }
async function adminApi(path, options = {}) { return api(path, { ...options, headers: { ...(options.headers || {}), Authorization: `Bearer ${token.value}` } }); }
async function doLogin(){ try{ const data=await api('/admin/login',{method:'POST',body:JSON.stringify(login)}); token.value=data.token; localStorage.setItem('adminToken',data.token); await loadAll(); }catch(e){ message.value='账号或密码错误'; }}
function logout(){ localStorage.removeItem('adminToken'); token.value=''; }
function selectMenu(menu){ if(menu.component !== 'Layout') activeMenu.value = menu.menuKey; }
async function loadAll(){ if(!token.value) return; const [p,m,u,r,ma,c,s,ds,rs]=await Promise.all([adminApi('/admin/profile'),adminApi('/admin/menus'),adminApi('/admin/users'),adminApi('/admin/roles'),adminApi('/admin/markets'),adminApi('/admin/categories'),adminApi('/admin/settings'),api('/datasources'),api('/reports')]); profile.value=p; menus.value=m; users.value=u; roles.value=r; markets.value=ma; categories.value=c; settings.value=s; sources.value=ds; reports.value=rs; }
async function saveSettings(next){ settings.value=await adminApi('/admin/settings',{method:'PUT',body:JSON.stringify(next)}); message.value='配置已保存'; }
function newUser(){ editingUser.value={id:0,username:'',nickname:'',roleKey:'operator',status:'enabled',email:''}; } function editUser(row){ editingUser.value={...row}; } async function saveUser(row){ await adminApi(row.id?`/admin/users/${row.id}`:'/admin/users',{method:row.id?'PUT':'POST',body:JSON.stringify(row)}); editingUser.value=null; users.value=await adminApi('/admin/users'); } async function deleteUser(row){ if(row.username==='admin') return; await adminApi(`/admin/users/${row.id}`,{method:'DELETE'}); users.value=await adminApi('/admin/users'); }
function newRole(){ editingRole.value={id:0,roleKey:'',roleName:'',status:'enabled',remark:''}; } function editRole(row){ editingRole.value={...row}; } async function saveRole(row){ await adminApi(row.id?`/admin/roles/${row.id}`:'/admin/roles',{method:row.id?'PUT':'POST',body:JSON.stringify(row)}); editingRole.value=null; roles.value=await adminApi('/admin/roles'); } async function deleteRole(row){ if(row.roleKey==='admin') return; await adminApi(`/admin/roles/${row.id}`,{method:'DELETE'}); roles.value=await adminApi('/admin/roles'); }
function newMarket(){ editingMarket.value={id:0,marketKey:'',marketName:'',region:'',enabled:false,note:''}; } function editMarket(row){ editingMarket.value={...row}; } async function saveMarket(row){ await adminApi(row.id?`/admin/markets/${row.id}`:'/admin/markets',{method:row.id?'PUT':'POST',body:JSON.stringify(row)}); editingMarket.value=null; markets.value=await adminApi('/admin/markets'); }
function newCategory(){ editingCategory.value={id:0,categoryName:'',marketKey:'jp',enabled:true,keywords:'',note:''}; } function editCategory(row){ editingCategory.value={...row}; } async function saveCategory(row){ await adminApi(row.id?`/admin/categories/${row.id}`:'/admin/categories',{method:row.id?'PUT':'POST',body:JSON.stringify(row)}); editingCategory.value=null; categories.value=await adminApi('/admin/categories'); }
onMounted(()=>loadAll().catch(e=>message.value=e.message));

const CrudHeader=defineComponent({props:['title'],emits:['add'],template:`<div class="crud-head"><h2>{{title}}</h2><button @click="$emit('add')">新增</button></div>`});
const DataTable=defineComponent({props:['rows','columns'],template:`<div class="admin-table"><table><thead><tr><th v-for="c in columns" :key="c.key">{{c.label}}</th><th v-if="$slots.actions">操作</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id || row.menuKey || row.reportDate"><td v-for="c in columns" :key="c.key">{{row[c.key]}}</td><td v-if="$slots.actions"><slot name="actions" :row="row" /></td></tr></tbody></table></div>`});
const SimpleForm=defineComponent({props:['model','fields'],emits:['save','cancel'],template:`<div class="modal"><form class="modal-card" @submit.prevent="$emit('save', model)"><label v-for="f in fields" :key="f.key">{{f.label}}<select v-if="f.options" v-model="model[f.key]"><option v-for="o in f.options" :key="o.value || o" :value="o.value || o">{{o.label || o}}</option></select><input v-else-if="f.type==='checkbox'" type="checkbox" v-model="model[f.key]"/><input v-else v-model="model[f.key]" /></label><div><button>保存</button><button type="button" class="ghost" @click="$emit('cancel')">取消</button></div></form></div>`});
const UserForm=defineComponent({components:{SimpleForm},props:['model','roles'],emits:['save','cancel'],computed:{fields(){return[{key:'username',label:'用户名'},{key:'nickname',label:'昵称'},{key:'roleKey',label:'角色',options:this.roles.map(r=>({value:r.roleKey,label:r.roleName}))},{key:'status',label:'状态',options:['enabled','disabled']},{key:'email',label:'邮箱'}]}},template:`<SimpleForm :model="model" :fields="fields" @save="$emit('save',$event)" @cancel="$emit('cancel')"/>`});
const RoleForm=defineComponent({components:{SimpleForm},props:['model'],emits:['save','cancel'],data(){return{fields:[{key:'roleKey',label:'角色标识'},{key:'roleName',label:'角色名称'},{key:'status',label:'状态',options:['enabled','disabled']},{key:'remark',label:'备注'}]}},template:`<SimpleForm :model="model" :fields="fields" @save="$emit('save',$event)" @cancel="$emit('cancel')"/>`});
const MarketForm=defineComponent({components:{SimpleForm},props:['model'],emits:['save','cancel'],data(){return{fields:[{key:'marketKey',label:'市场标识'},{key:'marketName',label:'市场名称'},{key:'region',label:'区域'},{key:'enabled',label:'启用',type:'checkbox'},{key:'note',label:'说明'}]}},template:`<SimpleForm :model="model" :fields="fields" @save="$emit('save',$event)" @cancel="$emit('cancel')"/>`});
const CategoryForm=defineComponent({components:{SimpleForm},props:['model','markets'],emits:['save','cancel'],computed:{fields(){return[{key:'categoryName',label:'品类名称'},{key:'marketKey',label:'市场',options:this.markets.map(m=>({value:m.marketKey,label:m.marketName}))},{key:'enabled',label:'启用',type:'checkbox'},{key:'keywords',label:'关键词'},{key:'note',label:'说明'}]}},template:`<SimpleForm :model="model" :fields="fields" @save="$emit('save',$event)" @cancel="$emit('cancel')"/>`});
const SettingsForm=defineComponent({props:['settings','section'],emits:['save'],setup(props,{emit}){const draft=reactive(JSON.parse(JSON.stringify(props.settings)));function save(){emit('save',draft)}return{draft,save,joinText,splitText}},template:`<section class="settings-form"><template v-if="section==='sources'"><label>国外检索数据源<textarea :value="joinText(draft.foreignSources)" @input="draft.foreignSources=splitText($event.target.value)" /></label><label>国内检索数据源<textarea :value="joinText(draft.domesticSources)" @input="draft.domesticSources=splitText($event.target.value)" /></label></template><template v-else><label>检索频率 Cron<input v-model="draft.frequencyCron" /></label><label>每日报表商品数<input type="number" v-model.number="draft.maxProducts" /></label><label>日元汇率<input type="number" step="0.001" v-model="draft.jpyCnyRate" /></label><label>默认物流成本<input type="number" v-model="draft.defaultShippingCny" /></label><label class="check"><input type="checkbox" v-model="draft.smartMode" />启用智能模式</label></template><button @click="save">保存配置</button></section>`});
</script>
