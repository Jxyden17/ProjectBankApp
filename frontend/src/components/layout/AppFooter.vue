<template>
  <footer class="border-t border-slate-200 bg-white/60 backdrop-blur">
    <div class="mx-auto flex max-w-7xl flex-col items-center justify-between gap-3 px-4 py-6 text-sm text-slate-500 sm:flex-row sm:px-6 lg:px-8">
      <p>&copy; {{ year }} Project Bank &middot; Student demo application</p>
    </div>
  </footer>
</template>

<script setup>
const year = new Date().getFullYear()
</script>
