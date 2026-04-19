<template>
  <div class="flex min-h-screen flex-col bg-slate-50 text-slate-900">
    <AppHeader />
    <main class="mx-auto w-full max-w-5xl flex-1 px-4 pb-16 pt-8 sm:px-6 lg:px-8">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
import AppHeader from './components/layout/AppHeader.vue'
import AppFooter from './components/layout/AppFooter.vue'
</script>
