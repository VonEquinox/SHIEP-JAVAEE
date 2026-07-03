<script setup>
// 入住人选择器：从常用入住人勾选（复选，多输入形式之一），可现场新增。
// 平铺原则：新增表单不用浮层，点击“+ 新增入住人”后表单就地向下滚出，把下方内容推走。
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { addGuest } from '../api/user';
import FlatSelect from './FlatSelect.vue';
import FlatDateRange from './FlatDateRange.vue';

const props = defineProps({
  guests: Array,           // 备选入住人列表（父组件加载）
  modelValue: Array,       // 已选 guestId 数组
  max: Number,             // 该房型可住人数上限
});
const emit = defineEmits(['update:modelValue', 'refresh']);

const adding = ref(false);
const empty = { name: '', idCard: '', gender: '男', birthDate: '', occupation: '', education: '', incomeLevel: '' };
const form = ref({ ...empty });

function toggle(guestId, checked) {
  const next = checked
    ? [...props.modelValue, guestId]
    : props.modelValue.filter((id) => id !== guestId);
  if (next.length > props.max) {
    ElMessage.warning(`该房型最多入住 ${props.max} 人`);
    return;
  }
  emit('update:modelValue', next);
}

async function submitAdd() {
  if (!form.value.name) return ElMessage.warning('请填写姓名');
  if (form.value.idCard && form.value.idCard.length !== 18) return ElMessage.warning('身份证号必须为18位');
  await addGuest(form.value);
  adding.value = false;
  form.value = { ...empty };
  emit('refresh');
  ElMessage.success('已添加');
}
</script>

<template>
  <div class="guest-picker">
    <div class="gp-row">
      <el-checkbox
        v-for="g in guests"
        :key="g.guestId"
        :model-value="modelValue.includes(g.guestId)"
        @update:model-value="toggle(g.guestId, $event)"
      >
        {{ g.name }}<span class="muted">（{{ g.gender }}）</span>
      </el-checkbox>
      <button type="button" class="gp-add" :class="{ open: adding }" @click="adding = !adding">
        {{ adding ? '× 收起' : '+ 新增入住人' }}
      </button>
    </div>

    <!-- 新增表单：就地向下滚出（平铺，无浮层） -->
    <el-collapse-transition>
      <div v-show="adding" class="gp-form">
        <el-input v-model="form.name" placeholder="姓名（必填）" />
        <el-input v-model="form.idCard" placeholder="身份证号（18位）" maxlength="18" />
        <div class="gp-gender">
          <span class="muted">性别</span>
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </div>
        <FlatDateRange v-model="form.birthDate" mode="single" placeholder="出生日期" />
        <FlatSelect v-model="form.occupation" placeholder="职业"
                    :options="['学生', '程序员', '教师', '医生', '销售', '经理', '企业主', '其他']" />
        <FlatSelect v-model="form.education" placeholder="受教育程度"
                    :options="['小学', '初中', '高中', '大专', '本科', '硕士', '博士']" />
        <FlatSelect v-model="form.incomeLevel" placeholder="收入状况"
                    :options="['较低', '中等', '较高']" />
        <div class="gp-actions">
          <el-button size="small" @click="adding = false">取消</el-button>
          <el-button size="small" type="primary" @click="submitAdd">保存</el-button>
        </div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<style scoped>
.guest-picker {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.gp-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.2rem 1rem;
  align-items: center;
}

.gp-add {
  border: 0;
  background: transparent;
  color: var(--md-sys-color-primary);
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.3rem 0;
}

/* 新增表单：网格平铺 */
.gp-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.7rem;
  align-items: start;
  padding: 1rem;
  margin-top: 0.6rem;
  border-radius: 16px;
  border: var(--flat-border);
}

.gp-gender {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  font-size: 0.9rem;
}

.gp-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  grid-column: 1 / -1;
}
</style>
