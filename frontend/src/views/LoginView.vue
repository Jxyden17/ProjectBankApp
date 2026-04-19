<template>
  <section class="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white/90 p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
    <PageHeader
      :label="$t('login.label')"
      :title="$t('login.title')"
      :description="$t('login.description')"
    />

    <form class="mt-8 space-y-4" @submit.prevent>
      <TextInput v-model="form.email" :label="$t('login.emailLabel')" type="email" />
      <TextInput v-model="form.password" :label="$t('login.passwordLabel')" type="password" />
      <Button type="submit" class="w-full">{{ $t('login.continueButton') }}</Button>
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
