<template>
  <label class="block space-y-2">
    <span class="text-sm font-medium text-slate-700">{{ label }}</span>
    <input
      :type="type"
      :value="modelValue"
      :required="required"
      :placeholder="placeholder"
      class="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
      @input="handleInput"
    >
  </label>
</template>

<script setup>
const props = defineProps({
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
  trim: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const handleInput = (event) => {
  const nextValue = props.trim ? event.target.value.trim() : event.target.value
  emit('update:modelValue', nextValue)
}
</script>
