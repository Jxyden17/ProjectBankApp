<template>
  <section class="mx-auto max-w-xl rounded-[2rem] border border-slate-200 bg-white/90 p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
    <PageHeader
      :label="$t('login.label')"
      :title="$t('login.title')"
      :description="$t('login.description')"
    />

    <FeedbackBanner class="mt-6" :message="infoMessage" tone="success" />
    <FeedbackBanner class="mt-4" :message="errorMessage" tone="error" />

    <form class="mt-8 space-y-4" @submit.prevent="submitLogin">
      <TextInput v-model="form.email" :label="$t('login.emailLabel')" type="email" required />
      <TextInput
        v-model="form.password"
        :label="$t('login.passwordLabel')"
        type="password"
        :trim="false"
        required
      />
      <Button type="submit" class="w-full" :disabled="isSubmitting">
        {{ isSubmitting ? $t('common.loading') : $t('login.continueButton') }}
      </Button>
    </form>

    <p class="mt-6 text-sm text-slate-600">
      {{ $t('login.registerPrompt') }}
      <RouterLink class="font-semibold text-emerald-700 hover:text-emerald-600" to="/register">
        {{ $t('login.registerLink') }}
      </RouterLink>
    </p>
  </section>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import PageHeader from '../components/ui/PageHeader.vue'
import TextInput from '../components/ui/TextInput.vue'
import Button from '../components/ui/Button.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import { useAuth } from '../composables/useAuth'

const form = reactive({
  email: '',
  password: '',
})

const auth = useAuth()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const isSubmitting = ref(false)
const errorMessage = ref('')
const infoMessage = ref('')

watch(
  () => route.query,
  (query) => {
    form.email = typeof query.email === 'string' ? query.email : form.email
    infoMessage.value = query.registered === '1' ? t('login.registrationSuccess') : ''
  },
  { immediate: true },
)

const resolveRedirectTarget = () => {
  if (typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')) {
    return route.query.redirect
  }

  return '/'
}

const submitLogin = async () => {
  isSubmitting.value = true
  errorMessage.value = ''
  infoMessage.value = ''

  try {
    await auth.login({
      email: form.email,
      password: form.password,
    })
    await router.push(resolveRedirectTarget())
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSubmitting.value = false
  }
}
</script>
