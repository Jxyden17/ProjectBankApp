import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import * as userService from '../services/userService'

const formatJson = (value) => JSON.stringify(value, null, 2)

const DEFAULT_FORM = {
  firstName: 'Jane',
  lastName: 'Doe',
  email: 'jane.doe@example.com',
}

export const useUsers = () => {
  const { t } = useI18n()

  const form = reactive({ ...DEFAULT_FORM })

  const isLoading = ref(false)
  const isCreating = ref(false)
  const responseBody = ref(formatJson({ message: t('home.response.initial') }))
  const feedbackMessage = ref('')
  const feedbackTone = ref('success')
  const lastRequest = ref('idle')

  const responseLabel = computed(() => {
    if (lastRequest.value === 'get') return 'GET /api/users'
    if (lastRequest.value === 'post') return 'POST /api/users'
    return t('common.waiting')
  })

  const run = async (requestType, fn, successMessage) => {
    feedbackMessage.value = ''
    lastRequest.value = requestType

    try {
      const data = await fn()
      responseBody.value = formatJson(data)
      feedbackTone.value = 'success'
      feedbackMessage.value = successMessage
    } catch (error) {
      feedbackTone.value = 'error'
      feedbackMessage.value = error.message ?? t('home.response.errorFallback')
      responseBody.value = formatJson(error.data ?? { error: feedbackMessage.value })
    }
  }

  const loadUsers = async () => {
    isLoading.value = true
    await run('get', userService.getUsers, t('home.response.successGet'))
    isLoading.value = false
  }

  const createUser = async () => {
    isCreating.value = true
    await run(
      'post',
      () => userService.createUser({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
      }),
      t('home.response.successPost'),
    )
    isCreating.value = false
  }

  const resetForm = () => {
    form.firstName = DEFAULT_FORM.firstName
    form.lastName = DEFAULT_FORM.lastName
    form.email = DEFAULT_FORM.email
  }

  return {
    form,
    isLoading,
    isCreating,
    responseBody,
    feedbackMessage,
    feedbackTone,
    responseLabel,
    loadUsers,
    createUser,
    resetForm,
  }
}
