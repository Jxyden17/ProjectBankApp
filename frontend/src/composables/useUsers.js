import { computed, reactive, ref } from 'vue'
import * as userService from '../services/userService'

const formatJson = (value) => JSON.stringify(value, null, 2)

const INITIAL_RESPONSE = formatJson({ message: 'No request sent yet.' })

const DEFAULT_FORM = {
  firstName: 'Jane',
  lastName: 'Doe',
  email: 'jane.doe@example.com',
}

export const useUsers = () => {
  const form = reactive({ ...DEFAULT_FORM })

  const isLoading = ref(false)
  const isCreating = ref(false)
  const responseBody = ref(INITIAL_RESPONSE)
  const feedbackMessage = ref('')
  const feedbackTone = ref('success')
  const lastRequest = ref('idle')

  const responseLabel = computed(() => {
    if (lastRequest.value === 'get') return 'GET /api/users'
    if (lastRequest.value === 'post') return 'POST /api/users'
    return 'Waiting'
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
      feedbackMessage.value = error.message ?? 'Request failed.'
      responseBody.value = formatJson(error.data ?? { error: feedbackMessage.value })
    }
  }

  const loadUsers = async () => {
    isLoading.value = true
    await run('get', userService.getUsers, 'Loaded example users successfully.')
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
      'Created example user successfully.',
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
