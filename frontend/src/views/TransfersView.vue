<template>
  <section class="rounded-[2rem] border border-slate-200 bg-white/85 p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
    <PageHeader
      label="Transfers"
      title="Transfer funds"
      description="This route is ready for a transfer form with source account selection, IBAN fields, amount validation, and a call to the backend transfer endpoint."
    />
  </section>
</template>

<script setup>
import PageHeader from '../components/ui/PageHeader.vue'
</script>
