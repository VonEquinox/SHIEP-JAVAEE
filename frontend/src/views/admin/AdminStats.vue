<script setup>
// 后台：三张统计报表（需求文档 3.6），汇总来自存储过程，明细来自视图
import { onMounted, ref } from 'vue';
import { statsBooking, statsGuest, statsOccupancy } from '../../api/order';
import StatTable from '../../components/StatTable.vue';

const range = ref(['2026-06-01', '2026-07-31']);
const group = ref('city');
const dim = ref('age');

const booking = ref({ summary: [], detail: [] });
const guest = ref({ summary: [], detail: [] });
const occupancy = ref({ summary: [], detail: [] });

const groupNames = { city: '城市', district: '区域', hotel: '酒店', price: '价格区间' };
const dimNames = { age: '年龄段', gender: '性别', occupation: '职业', education: '受教育程度', income: '收入状况' };

async function load() {
  const [start, end] = range.value;
  [booking.value, guest.value, occupancy.value] = await Promise.all([
    statsBooking({ start, end, group: group.value }),
    statsGuest({ start, end, dim: dim.value }),
    statsOccupancy({ start, end }),
  ]);
}

onMounted(load);
</script>

<template>
  <div class="page">
    <div class="head">
      <h2>统计报表</h2>
      <el-date-picker v-model="range" type="daterange" start-placeholder="开始"
                      end-placeholder="结束" value-format="YYYY-MM-DD" @change="load" />
    </div>

    <!-- 报表一：客房预订情况 -->
    <StatTable title="客房预订情况" :summary="booking.summary" :detail="booking.detail"
               :summary-cols="[
                 { prop: 'group_name', label: groupNames[group] },
                 { prop: 'order_count', label: '订单数' },
                 { prop: 'room_count', label: '间数' },
                 { prop: 'night_count', label: '间夜数' },
                 { prop: 'amount', label: '金额（元）' },
               ]">
      <template #controls>
        <el-radio-group v-model="group" size="small" @change="load">
          <el-radio-button v-for="(label, key) in groupNames" :key="key" :value="key">
            按{{ label }}
          </el-radio-button>
        </el-radio-group>
      </template>
    </StatTable>

    <!-- 报表二：入住人画像偏好 -->
    <StatTable title="入住人画像偏好" :summary="guest.summary" :detail="guest.detail"
               :summary-cols="[
                 { prop: 'dim_value', label: dimNames[dim] },
                 { prop: 'type_name', label: '偏好房型' },
                 { prop: 'room_count', label: '预订间数' },
               ]">
      <template #controls>
        <el-radio-group v-model="dim" size="small" @change="load">
          <el-radio-button v-for="(label, key) in dimNames" :key="key" :value="key">
            {{ label }}
          </el-radio-button>
        </el-radio-group>
      </template>
    </StatTable>

    <!-- 报表三：客房入住率（存储过程 sp_occupancy_rate） -->
    <StatTable title="各类客房入住率" :summary="occupancy.summary" :detail="occupancy.detail"
               :summary-cols="[
                 { prop: 'hotel_name', label: '酒店' },
                 { prop: 'city', label: '城市' },
                 { prop: 'type_name', label: '房型' },
                 { prop: 'sold_nights', label: '已售间夜' },
                 { prop: 'total_nights', label: '总间夜' },
                 { prop: 'occupancy_rate', label: '入住率（%）' },
               ]" />
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.head h2 {
  margin: 0;
  font-weight: 560;
  letter-spacing: -0.02em;
}
</style>
