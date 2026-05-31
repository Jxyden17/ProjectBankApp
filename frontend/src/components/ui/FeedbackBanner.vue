<template>
  <p
    v-if="message"
    class="rounded-2xl border px-4 py-3 text-sm"
    :class="tones[tone]"
  >
    {{ message }}
  </p>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: '',
  },
  tone: {
    type: String,
    default: 'success',
    validator: (v) => ['success', 'error'].includes(v),
  },
})

const tones = {
  success: 'border-emerald-300 bg-emerald-50 text-emerald-800',
  error: 'border-rose-300 bg-rose-50 text-rose-800',
}
</script>
