<template>
  <section class="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white/90 p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
    <PageHeader
      label="Authentication"
      title="Sign in"
      description="This placeholder page is ready for your future login form, token storage, and authenticated route handling."
    />

    <form class="mt-8 space-y-4" @submit.prevent>
      <TextInput v-model="form.email" label="Email" type="email" />
      <TextInput v-model="form.password" label="Password" type="password" />
      <Button type="submit" class="w-full">Continue</Button>
    </form>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import PageHeader from '../components/ui/PageHeader.vue'
import TextInput from '../components/ui/TextInput.vue'
import Button from '../components/ui/Button.vue'

const form = reactive({
  email: '',
  password: '',
})
</script>
