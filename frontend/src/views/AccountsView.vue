<template>
  <section class="rounded-[2rem] border border-slate-200 bg-white/85 p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
    <PageHeader
      :label="$t('accounts.label')"
      :title="$t('accounts.title')"
      :description="$t('accounts.description')"
    />
  </section>
</template>

<script setup>
import PageHeader from '../components/ui/PageHeader.vue'
</script>
