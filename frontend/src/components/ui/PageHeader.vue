<template>
  <div class="space-y-3">
    <p class="text-xs font-semibold uppercase tracking-[0.3em] text-emerald-600">{{ label }}</p>
    <h1 class="text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">{{ title }}</h1>
    <p v-if="description || $slots.default" class="text-sm leading-7 text-slate-600">
      <slot>{{ description }}</slot>
    </p>
  </div>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
})
</script>
