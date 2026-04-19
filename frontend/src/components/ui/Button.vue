<template>
  <button
    :type="type"
    :disabled="disabled"
    :class="[base, variants[variant]]"
  >
    <slot />
  </button>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'success', 'secondary'].includes(v),
  },
  type: {
    type: String,
    default: 'button',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const base =
  'rounded-full px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed'

const variants = {
  primary:
    'bg-slate-950 text-white hover:bg-slate-800 disabled:bg-slate-300',
  success:
    'bg-emerald-600 text-white hover:bg-emerald-500 disabled:bg-emerald-300',
  secondary:
    'border border-slate-300 text-slate-700 hover:border-slate-400 hover:text-slate-950',
}
</script>
