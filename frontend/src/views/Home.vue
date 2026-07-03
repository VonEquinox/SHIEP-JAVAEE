<script setup>
import { computed, onMounted, ref } from 'vue';
import { getCities, searchHotels } from '../api/hotel';
import { useCardExpand } from '../composables/useCardExpand';
import HotelCard from '../components/HotelCard.vue';
import FlatSelect from '../components/FlatSelect.vue';
import FlatDateRange from '../components/FlatDateRange.vue';

const cities = ref({});
const hotels = ref([]);
const query = ref({ city: '', district: '', name: '', price: [0, 3000], dates: null });
const { expandedId, expand, collapse } = useCardExpand();

const cityOptions = computed(() => Object.keys(cities.value));
const districtOptions = computed(() => cities.value[query.value.city] || []);

async function search() {
  const [priceMin, priceMax] = query.value.price;
  hotels.value = await searchHotels({
    city: query.value.city || undefined,
    district: query.value.district || undefined,
    name: query.value.name || undefined,
    priceMin,
    priceMax,
  });
}

onMounted(async () => {
  cities.value = await getCities();
  await search();
});
</script>

<template>
  <div class="home-shell">
    <header class="page-hero" data-home-hero data-push-away>
      <h1>预订全国的<span>酒店客房</span></h1>
      <p>按城市、区域、价格与入住日期检索，点开卡片即可查看房型并完成预订。</p>
    </header>

    <!-- 搜索条：全部平铺控件，展开时把下方内容（酒店网格）推走 -->
    <div class="search-bar flat-card" data-home-hint data-push-away>
      <div class="search-fields">
        <FlatSelect v-model="query.city" placeholder="城市" :options="cityOptions"
                    class="f-city" @change="query.district = ''" />
        <FlatSelect v-model="query.district" placeholder="区域" :options="districtOptions"
                    class="f-district" :disabled="!query.city" />
        <el-input v-model="query.name" placeholder="酒店名" clearable class="f-name" />
        <div class="price-filter">
          <span class="muted">¥{{ query.price[0] }} - ¥{{ query.price[1] }}</span>
          <el-slider v-model="query.price" range :max="3000" :step="50" />
        </div>
        <FlatDateRange v-model="query.dates" class="f-dates" />
        <el-button type="primary" round @click="search">搜索</el-button>
      </div>
    </div>

    <!-- 酒店卡片网格：点击原地展开为详情（博客 post-grid 同款交互） -->
    <div class="hotel-grid" data-expand-grid>
      <HotelCard
        v-for="h in hotels"
        :key="h.hotelId"
        :hotel="h"
        :dates="query.dates"
        :expanded="expandedId === h.hotelId"
        @open="expand(h.hotelId, $event)"
        @close="collapse"
      />
    </div>

    <el-empty v-if="!hotels.length" description="没有符合条件的酒店" />
  </div>
</template>

<style scoped>
.home-shell {
  display: flex;
  flex-direction: column;
  gap: 2.3rem;
}

.search-bar {
  padding: 1rem 1.2rem;
  border-radius: 26px;
}

.search-fields {
  display: grid;
  grid-template-columns: 130px 130px 170px 1fr 300px auto;
  gap: 0.8rem;
  align-items: start;
}

@media (max-width: 1100px) {
  .search-fields {
    grid-template-columns: 1fr 1fr;
  }
}

.price-filter {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0 0.4rem;
  font-size: 0.85rem;
  height: 2.4rem;
}

.price-filter .el-slider {
  flex: 1;
}

.price-filter .muted {
  white-space: nowrap;
}

.hotel-grid {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 1.2rem;
  align-items: stretch;
}

@media (min-width: 860px) {
  .hotel-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1240px) {
  .hotel-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

/* 阅读态：网格切块级布局；未展开的兄弟卡片必须隐藏（退场动画由幽灵完成），
   否则会残留并堆叠在展开卡片上方 */
html[data-reading='true'] .hotel-grid {
  display: block;
}

html[data-reading='true'] .hotel-grid [data-expand-card]:not([data-expanded='true']) {
  display: none;
}

html[data-reading='true'] .search-bar,
html[data-reading='true'] .page-hero {
  display: none;
}
</style>
