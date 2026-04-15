<template>
  <div class="min-h-screen bg-slate-50 text-slate-900">
    <AppNavbar />
    <main class="mx-auto max-w-5xl px-4 pb-16 pt-8 sm:px-6 lg:px-8">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
import AppNavbar from './components/organisms/AppNavbar.vue'
</script>
