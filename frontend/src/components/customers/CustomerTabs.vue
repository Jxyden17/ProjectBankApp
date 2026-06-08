<template>
  <div class="flex flex-wrap gap-2 rounded-full border border-slate-200 bg-white/80 p-1">
    <RouterLink
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      class="rounded-full px-4 py-2 text-sm font-semibold transition"
      :class="isActive(tab.to)
        ? 'bg-slate-950 text-white'
        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'"
    >
      {{ $t(tab.labelKey) }}
    </RouterLink>
  </div>
</template>

<script setup>
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()
const tabs = [
  { to: '/customers', labelKey: 'customers.overviewTab' },
  { to: '/customers/approvals', labelKey: 'customers.approvalsTab' },
]

// Marks the tab that matches the current route.
const isActive = (path) => route.path === path
</script>
