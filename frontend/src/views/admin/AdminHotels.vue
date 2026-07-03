<script setup>
// 后台：酒店与房型维护（客房情况维护，需求文档 3.7）
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deleteRoomType, getHotelRooms, saveHotel, saveRoomType, searchHotels } from '../../api/hotel';

const hotels = ref([]);
const current = ref(null);     // 正在编辑房型的酒店
const rooms = ref([]);
const hotelForm = ref(null);   // 酒店编辑表单（null = 收起）
const roomForm = ref(null);    // 房型编辑表单

const emptyHotel = { hotelName: '', city: '', district: '', address: '', star: 3, phone: '' };
const emptyRoom = { typeName: '', price: 0, capacity: 2, totalCount: 1 };

async function load() {
  hotels.value = await searchHotels({});
}

async function openRooms(h) {
  current.value = h;
  rooms.value = await getHotelRooms(h.hotelId);
}

async function submitHotel() {
  const f = hotelForm.value;
  if (!f.hotelName || !f.city || !f.district) return ElMessage.warning('酒店名、城市、区域必填');
  await saveHotel(f);
  hotelForm.value = null;
  ElMessage.success('已保存');
  await load();
}

async function submitRoom() {
  const f = roomForm.value;
  if (!f.typeName) return ElMessage.warning('房型名必填');
  await saveRoomType({ ...f, hotelId: current.value.hotelId });
  roomForm.value = null;
  ElMessage.success('已保存');
  await openRooms(current.value);
}

async function removeRoom(r) {
  await ElMessageBox.confirm(`确定删除房型「${r.typeName}」吗？`, '删除确认');
  await deleteRoomType(r.roomTypeId);
  ElMessage.success('已删除');
  await openRooms(current.value);
}

onMounted(load);
</script>

<template>
  <div class="page">
    <div class="head">
      <h2>酒店与房型</h2>
      <el-button type="primary" round @click="hotelForm = { ...emptyHotel }">+ 新增酒店</el-button>
    </div>

    <!-- 酒店编辑表单：从按钮下方展开 -->
    <el-collapse-transition>
      <div v-if="hotelForm" class="flat-card form">
        <el-input v-model="hotelForm.hotelName" placeholder="酒店名" />
        <el-input v-model="hotelForm.city" placeholder="城市" />
        <el-input v-model="hotelForm.district" placeholder="区域" />
        <el-input v-model="hotelForm.address" placeholder="地址" />
        <div class="star-row">
          星级 <el-rate v-model="hotelForm.star" />
        </div>
        <el-input v-model="hotelForm.phone" placeholder="电话" />
        <div class="form-actions">
          <el-button @click="hotelForm = null">取消</el-button>
          <el-button type="primary" @click="submitHotel">保存</el-button>
        </div>
      </div>
    </el-collapse-transition>

    <el-table :data="hotels" style="width: 100%">
      <el-table-column prop="hotelName" label="酒店" min-width="160" />
      <el-table-column prop="city" label="城市" width="80" />
      <el-table-column prop="district" label="区域" width="100" />
      <el-table-column prop="star" label="星级" width="70" />
      <el-table-column prop="minPrice" label="最低价" width="90" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button text size="small" @click="hotelForm = { ...row }">编辑</el-button>
          <el-button text type="primary" size="small" @click="openRooms(row)">房型维护</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 房型维护：抽屉自右侧生长 -->
    <el-drawer :model-value="!!current" :title="current?.hotelName + ' · 房型维护'"
               size="46%" @close="current = null; roomForm = null">
      <div class="drawer-body">
        <el-button type="primary" round size="small" @click="roomForm = { ...emptyRoom }">+ 新增房型</el-button>

        <el-collapse-transition>
          <div v-if="roomForm" class="flat-card form">
            <el-input v-model="roomForm.typeName" placeholder="房型名（如 标准间）" />
            <el-input-number v-model="roomForm.price" :min="0" :step="10" /> 元/晚
            <el-input-number v-model="roomForm.capacity" :min="1" :max="10" /> 可住人数
            <el-input-number v-model="roomForm.totalCount" :min="0" /> 客房总数
            <div class="form-actions">
              <el-button @click="roomForm = null">取消</el-button>
              <el-button type="primary" @click="submitRoom">保存</el-button>
            </div>
          </div>
        </el-collapse-transition>

        <el-table :data="rooms">
          <el-table-column prop="typeName" label="房型" />
          <el-table-column prop="price" label="价格" width="90" />
          <el-table-column prop="capacity" label="可住" width="70" />
          <el-table-column prop="totalCount" label="总数" width="70" />
          <el-table-column prop="availableCount" label="可订" width="70" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button text size="small" @click="roomForm = { ...row }">编辑</el-button>
              <el-button text type="danger" size="small" @click="removeRoom(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head h2 {
  margin: 0;
  font-weight: 560;
  letter-spacing: -0.02em;
}

.form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.8rem;
  padding: 1.2rem;
  align-items: center;
}

.star-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--md-sys-color-on-surface-variant);
  font-size: 0.9rem;
}

.form-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  grid-column: 1 / -1;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  align-items: flex-start;
}

.drawer-body .el-table {
  width: 100%;
}
</style>
