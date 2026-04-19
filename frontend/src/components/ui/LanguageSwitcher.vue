<template>
  <label class="flex items-center gap-2 text-sm text-slate-600">
    <span class="sr-only">{{ $t('common.language') }}</span>
    <select
      :value="current"
      class="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 outline-none transition hover:border-slate-300 focus:border-emerald-500"
      @change="onChange"
    >
      <option
        v-for="option in available"
        :key="option.code"
        :value="option.code"
      >
        {{ option.label }}
      </option>
    </select>
  </label>
</template>

<script setup>
import { useLocale } from '../../composables/useLocale'

const { current, available, setLocale } = useLocale()

const onChange = (event) => {
  setLocale(event.target.value)
}
</script>
