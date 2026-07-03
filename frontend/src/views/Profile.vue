<script setup>
// 个人中心：基本信息维护、积分等级、常用入住人管理
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deleteGuest, getMe, listGuests, updateGuest, updateMe } from '../api/user';
import { auth } from '../store/auth';
import GuestPicker from '../components/GuestPicker.vue';

const me = ref(null);
const guests = ref([]);
const form = ref({ phone: '', password: '', idCard: '' });
const editingGuest = ref(null);   // 行内编辑的 guestId

async function load() {
  me.value = await getMe();
  guests.value = await listGuests();
  form.value = { phone: me.value.phone, password: '', idCard: me.value.idCard || '' };
  // 同步导航栏显示的等级
  auth.user.level = me.value.level;
  localStorage.setItem('user', JSON.stringify(auth.user));
}

async function saveMe() {
  if (!/^\d{11}$/.test(form.value.phone)) return ElMessage.warning('手机号必须为11位数字');
  if (form.value.idCard && form.value.idCard.length !== 18) return ElMessage.warning('身份证号必须为18位');
  await updateMe({ ...form.value, password: form.value.password || null });
  ElMessage.success('已保存');
  await load();
}

async function saveGuest(g) {
  await updateGuest(g);
  editingGuest.value = null;
  ElMessage.success('已保存');
  await load();
}

async function removeGuest(g) {
  await ElMessageBox.confirm(`确定删除入住人「${g.name}」吗？`, '删除确认');
  await deleteGuest(g.guestId);
  ElMessage.success('已删除');
  await load();
}

const nextLevel = (points) =>
  points >= 5000 ? null : points >= 1000 ? { name: 'SVIP', need: 5000 - points } : { name: 'VIP', need: 1000 - points };

onMounted(load);
</script>

<template>
  <div v-if="me" class="profile-page">
    <header class="page-hero">
      <h1>{{ me.username }} 的<span>个人中心</span></h1>
      <p>维护你的基本信息与常用入住人，查看积分与会员等级。</p>
    </header>

    <!-- 积分与等级 -->
    <section class="flat-card block">
      <h3>积分与等级</h3>
      <div class="points-row">
        <span class="big">{{ me.points }}</span>
        <span class="muted">积分</span>
        <span class="pill">{{ me.level }}</span>
        <span v-if="me.level === 'VIP'" class="pill">预订享 95 折</span>
        <span v-if="me.level === 'SVIP'" class="pill">预订享 9 折</span>
      </div>
      <p v-if="nextLevel(me.points)" class="muted">
        再积 {{ nextLevel(me.points).need }} 分升级为 {{ nextLevel(me.points).name }}。
        订单完成后按实付金额 1 元 = 1 积分累计。
      </p>
    </section>

    <!-- 基本信息 -->
    <section class="flat-card block">
      <h3>基本信息</h3>
      <div class="form-grid">
        <el-input v-model="form.phone" placeholder="手机号" maxlength="11" />
        <el-input v-model="form.idCard" placeholder="身份证号（18位）" maxlength="18" />
        <el-input v-model="form.password" type="password" placeholder="新密码（留空则不修改）" show-password />
        <el-button type="primary" round @click="saveMe">保存修改</el-button>
      </div>
    </section>

    <!-- 常用入住人 -->
    <section class="flat-card block">
      <h3>常用入住人</h3>
      <el-table :data="guests" style="width: 100%">
        <el-table-column label="姓名" width="110">
          <template #default="{ row }">
            <el-input v-if="editingGuest === row.guestId" v-model="row.name" size="small" />
            <span v-else>{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="birthDate" label="出生日期" width="120" />
        <el-table-column label="职业" width="110">
          <template #default="{ row }">
            <el-input v-if="editingGuest === row.guestId" v-model="row.occupation" size="small" />
            <span v-else>{{ row.occupation }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="education" label="学历" width="90" />
        <el-table-column prop="incomeLevel" label="收入" width="90" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="editingGuest !== row.guestId" text size="small"
                       @click="editingGuest = row.guestId">编辑</el-button>
            <el-button v-else text type="primary" size="small" @click="saveGuest(row)">保存</el-button>
            <el-button text type="danger" size="small" @click="removeGuest(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 复用 GuestPicker 的新增能力：guests 传空数组，只用“+新增入住人” -->
      <GuestPicker :guests="[]" :model-value="[]" :max="0" @refresh="load" />
    </section>
  </div>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.block {
  padding: 1.5rem;
}

.block h3 {
  margin: 0 0 1rem;
  font-weight: 560;
}

.points-row {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.big {
  font-size: 2.4rem;
  font-weight: 600;
  color: var(--md-sys-color-primary);
  letter-spacing: -0.02em;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.8rem;
  align-items: center;
}
</style>
