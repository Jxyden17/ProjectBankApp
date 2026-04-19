<template>
  <label class="block space-y-2">
    <span class="text-sm font-medium text-slate-700">{{ label }}</span>
    <input
      :type="type"
      :value="modelValue"
      :required="required"
      :placeholder="placeholder"
      class="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
      @input="$emit('update:modelValue', $event.target.value.trim())"
    >
  </label>
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  label: {
    type: String,
    required: true,
  },
  type: {
    type: String,
    default: 'text',
  },
  required: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: '',
  },
})

defineEmits(['update:modelValue'])
</script>
