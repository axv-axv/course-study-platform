<template>
  <div class="progress-ring" :style="{ width: size + 'px', height: size + 'px' }">
    <svg :width="size" :height="size" viewBox="0 0 100 100">
      <circle class="track" cx="50" cy="50" :r="radius" fill="none" :stroke-width="strokeWidth" />
      <circle
        class="bar"
        cx="50"
        cy="50"
        :r="radius"
        fill="none"
        :stroke-width="strokeWidth"
        :stroke="color"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        :style="{ transition: 'stroke-dashoffset 0.6s ease' }"
        transform="rotate(-90 50 50)"
      />
    </svg>
    <div class="center" :style="{ fontSize: fontSize + 'px' }">
      <slot>{{ displayText }}</slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    /** 0-100 */
    value: number
    size?: number
    strokeWidth?: number
    color?: string
    displayText?: string
    fontSize?: number
  }>(),
  {
    size: 80,
    strokeWidth: 8,
    color: '#4f6bff',
    displayText: '',
    fontSize: 14
  }
)

const radius = 42
const circumference = 2 * Math.PI * radius

const clamped = computed(() => Math.max(0, Math.min(100, props.value || 0)))
const dashOffset = computed(() => circumference * (1 - clamped.value / 100))
</script>

<style scoped lang="scss">
.progress-ring {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;

  .track {
    stroke: rgba(148, 163, 184, 0.18);
  }

  .bar {
    transition: stroke-dashoffset 0.6s ease;
  }

  .center {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    color: $color-text-2;
  }
}
</style>
