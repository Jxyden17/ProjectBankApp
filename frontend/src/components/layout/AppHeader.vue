<template>
  <header class="sticky top-0 z-50 border-b border-white/60 bg-white/75 backdrop-blur-xl">
    <nav class="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
      <RouterLink to="/" class="flex items-center gap-3">
        <span class="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-sm font-semibold tracking-[0.2em] text-white">
          PB
        </span>
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.35em] text-emerald-600">Project Bank</p>
          <p class="text-sm font-medium text-slate-600">Student demo application</p>
        </div>
      </RouterLink>

      <div class="hidden items-center gap-2 rounded-full border border-slate-200 bg-white/80 p-1 md:flex">
        <RouterLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          class="rounded-full px-4 py-2 text-sm font-medium transition"
          :class="isActive(link.to)
            ? 'bg-slate-950 text-white shadow-sm'
            : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'"
        >
          {{ link.label }}
        </RouterLink>
      </div>

      <RouterLink to="/login">
        <Button variant="primary">Sign in</Button>
      </RouterLink>
    </nav>

    <div class="border-t border-slate-200/80 px-4 py-3 md:hidden">
      <div class="flex flex-wrap gap-2">
        <RouterLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          class="rounded-full px-3 py-2 text-sm font-medium transition"
          :class="isActive(link.to)
            ? 'bg-slate-950 text-white'
            : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:text-slate-950'"
        >
          {{ link.label }}
        </RouterLink>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRoute } from 'vue-router'
import Button from '../ui/Button.vue'

defineProps({
  links: {
    type: Array,
    default: () => [
      { label: 'Home', to: '/' },
      { label: 'Accounts', to: '/accounts' },
      { label: 'Transfers', to: '/transfers' },
    ],
  },
})

const route = useRoute()
const isActive = (path) => route.path === path
</script>
