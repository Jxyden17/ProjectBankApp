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
  success: 'border-emerald-400/30 bg-emerald-400/10 text-emerald-100',
  error: 'border-rose-400/30 bg-rose-400/10 text-rose-100',
}
</script>
