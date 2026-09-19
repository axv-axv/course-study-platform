<template>
  <div class="empty-state" :style="{ padding: `${padding}px` }">
    <div class="empty-icon">
      <el-icon :size="46"><component :is="icon" /></el-icon>
    </div>
    <p class="empty-text">{{ text }}</p>
    <p v-if="subText" class="empty-sub">{{ subText }}</p>
    <div v-if="$slots.default" class="empty-action">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    text?: string
    subText?: string
    icon?: string
    padding?: number | string
  }>(),
  {
    text: '暂无数据',
    subText: '',
    icon: 'Box',
    padding: 48
  }
)
</script>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;

  .empty-icon {
    width: 88px;
    height: 88px;
    border-radius: 50%;
    background: $gradient-soft;
    color: $color-primary;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
  }

  .empty-text {
    font-size: 15px;
    font-weight: 600;
    color: $color-text-2;
  }

  .empty-sub {
    font-size: 13px;
    color: $color-text-3;
    margin-top: 6px;
    max-width: 320px;
  }

  .empty-action {
    margin-top: 18px;
  }
}
</style>
