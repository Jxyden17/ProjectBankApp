<template>
  <section class="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white/90 p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
    <PageHeader
      :label="$t('register.label')"
      :title="$t('register.title')"
      :description="$t('register.description')"
    />

    <FeedbackBanner class="mt-6" :message="errorMessage" tone="error" />

    <form class="mt-8 space-y-4" @submit.prevent="submitRegistration">
      <div class="grid gap-4 sm:grid-cols-2">
        <TextInput v-model="form.firstName" :label="$t('register.firstNameLabel')" required />
        <TextInput v-model="form.lastName" :label="$t('register.lastNameLabel')" required />
      </div>
      <TextInput v-model="form.email" :label="$t('register.emailLabel')" type="email" required />
      <TextInput
        v-model="form.password"
        :label="$t('register.passwordLabel')"
        type="password"
        :trim="false"
        required
      />
      <TextInput v-model="form.phoneNumber" :label="$t('register.phoneNumberLabel')" required />
      <TextInput v-model="form.bsnNumber" :label="$t('register.bsnNumberLabel')" required />

      <Button type="submit" class="w-full" :disabled="isSubmitting">
        {{ isSubmitting ? $t('common.creating') : $t('register.continueButton') }}
      </Button>
    </form>

    <p class="mt-6 text-sm text-slate-600">
      {{ $t('register.loginPrompt') }}
      <RouterLink class="font-semibold text-emerald-700 hover:text-emerald-600" to="/login">
        {{ $t('register.loginLink') }}
      </RouterLink>
    </p>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import PageHeader from '../components/ui/PageHeader.vue'
import TextInput from '../components/ui/TextInput.vue'
import Button from '../components/ui/Button.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import { useAuth } from '../composables/useAuth'

const auth = useAuth()
const router = useRouter()

const form = reactive({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  phoneNumber: '',
  bsnNumber: '',
})

const isSubmitting = ref(false)
const errorMessage = ref('')

const submitRegistration = async () => {
  isSubmitting.value = true
  errorMessage.value = ''

  try {
    await auth.register({
      firstName: form.firstName,
      lastName: form.lastName,
      email: form.email,
      password: form.password,
      phoneNumber: form.phoneNumber,
      bsnNumber: form.bsnNumber,
    })

    await router.push({
      name: 'login',
      query: {
        registered: '1',
        email: form.email,
      },
    })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSubmitting.value = false
  }
}
</script>
