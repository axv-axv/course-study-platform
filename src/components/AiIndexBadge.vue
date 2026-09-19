<template>
  <el-tooltip :content="tip" placement="top">
    <span class="ai-badge" :class="status.toLowerCase()">
      <el-icon :size="12">
        <Loading v-if="status === 'PROCESSING'" class="spin" />
        <CircleCheck v-else-if="status === 'INDEXED'" />
        <CircleClose v-else-if="status === 'FAILED'" />
        <CirclePlus v-else />
      </el-icon>
      <span class="text">{{ label }}</span>
    </span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AiIndexStatus } from '@/types'
import { aiIndexStatusText } from '@/utils/format'

const props = defineProps<{ status: AiIndexStatus; error?: string | null }>()

const label = computed(() => aiIndexStatusText[props.status] || props.status)
const tip = computed(() => (props.status === 'FAILED' && props.error ? `索引失败：${props.error}` : `AI 索引状态：${label.value}`))
</script>

<style scoped lang="scss">
.ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;

  &.not_indexed {
    background: rgba(148, 163, 184, 0.14);
    color: #64748b;
  }

  &.processing {
    background: rgba(245, 158, 11, 0.14);
    color: #d97706;
  }

  &.indexed {
    background: rgba(34, 197, 94, 0.14);
    color: #16a34a;
  }

  &.failed {
    background: rgba(239, 68, 68, 0.14);
    color: #dc2626;
  }
}

.spin {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  to {
    transform: rotate(360deg);
  }
}
</style>
