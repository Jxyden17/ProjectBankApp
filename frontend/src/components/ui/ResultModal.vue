<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-[100] flex items-end justify-center bg-slate-950/45 px-4 py-6 backdrop-blur-sm sm:items-center"
      role="presentation"
      @click.self="$emit('close')"
    >
      <section
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        :class="['w-full max-w-md rounded-3xl border p-6 shadow-[0_24px_80px_-32px_rgba(15,23,42,0.55)]', toneClasses.panel]"
      >
        <div class="space-y-2">
          <p :class="['text-xs font-semibold uppercase tracking-[0.28em]', toneClasses.label]">
            {{ label }}
          </p>
          <h2 :id="titleId" class="text-2xl font-semibold text-slate-950">
            {{ title }}
          </h2>
          <p class="text-sm leading-6 text-slate-700">
            {{ message }}
          </p>
        </div>

        <div class="mt-6 flex justify-end">
          <Button variant="secondary" @click="$emit('close')">
            {{ closeLabel }}
          </Button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'
import Button from './Button.vue'

defineEmits(['close'])

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  tone: {
    type: String,
    default: 'success',
    validator: (value) => ['success', 'error'].includes(value),
  },
  label: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  message: {
    type: String,
    required: true,
  },
  closeLabel: {
    type: String,
    required: true,
  },
})

const toneClasses = computed(() => ({
  success: {
    panel: 'border-emerald-200 bg-emerald-50',
    label: 'text-emerald-700',
  },
  error: {
    panel: 'border-red-200 bg-red-50',
    label: 'text-red-700',
  },
}[props.tone]))

const titleId = `result-modal-${Math.random().toString(36).slice(2)}`
</script>
