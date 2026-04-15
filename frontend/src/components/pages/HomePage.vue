<template>
  <section class="mx-auto max-w-4xl space-y-8">
    <div class="space-y-3">
      <p class="text-xs font-semibold uppercase tracking-[0.3em] text-emerald-600">API Test</p>
      <h1 class="text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">Example users endpoint</h1>
      <p class="text-sm text-slate-600">
        Use this page to test the backend at
        <code class="rounded bg-slate-200 px-2 py-1 text-slate-900">{{ exampleUsersEndpoint }}</code>.
      </p>
    </div>

    <div class="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
      <div class="space-y-6">
        <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div class="flex items-center justify-between gap-4">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">GET</p>
              <h2 class="mt-2 text-xl font-semibold text-slate-950">Load users</h2>
            </div>
            <button
              type="button"
              class="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="isLoadingUsers"
              @click="loadUsers"
            >
              {{ isLoadingUsers ? 'Loading...' : 'Load users' }}
            </button>
          </div>
          <p class="mt-4 text-sm leading-6 text-slate-600">Fetch the current example users from the Spring backend.</p>
        </article>

        <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">POST</p>
            <h2 class="mt-2 text-xl font-semibold text-slate-950">Create user</h2>
          </div>

          <form class="mt-5 space-y-4" @submit.prevent="createUser">
            <label class="block space-y-2">
              <span class="text-sm font-medium text-slate-700">First name</span>
              <input
                v-model.trim="form.firstName"
                type="text"
                required
                class="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
              >
            </label>

            <label class="block space-y-2">
              <span class="text-sm font-medium text-slate-700">Last name</span>
              <input
                v-model.trim="form.lastName"
                type="text"
                required
                class="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
              >
            </label>

            <label class="block space-y-2">
              <span class="text-sm font-medium text-slate-700">Email</span>
              <input
                v-model.trim="form.email"
                type="email"
                required
                class="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
              >
            </label>

            <div class="flex flex-wrap gap-3">
              <button
                type="submit"
                class="rounded-full bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:bg-emerald-300"
                :disabled="isCreatingUser"
              >
                {{ isCreatingUser ? 'Creating...' : 'Create user' }}
              </button>
              <button
                type="button"
                class="rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-950"
                @click="resetForm"
              >
                Reset
              </button>
            </div>
          </form>
        </article>
      </div>

      <article class="rounded-3xl border border-slate-200 bg-slate-950 p-6 text-white shadow-sm">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-300">Response</p>
            <h2 class="mt-2 text-xl font-semibold">Latest result</h2>
          </div>
          <span class="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium text-slate-300">
            {{ responseLabel }}
          </span>
        </div>

        <p
          v-if="feedbackMessage"
          class="mt-5 rounded-2xl border px-4 py-3 text-sm"
          :class="feedbackTone === 'error'
            ? 'border-rose-400/30 bg-rose-400/10 text-rose-100'
            : 'border-emerald-400/30 bg-emerald-400/10 text-emerald-100'"
        >
          {{ feedbackMessage }}
        </p>

        <pre class="mt-5 min-h-80 overflow-x-auto rounded-2xl bg-black/30 p-4 text-sm leading-6 text-slate-200">{{ responseBody }}</pre>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { exampleUsersEndpoint } from '../../config'

const form = reactive({
  firstName: 'Jane',
  lastName: 'Doe',
  email: 'jane.doe@example.com',
})

const isLoadingUsers = ref(false)
const isCreatingUser = ref(false)
const responseBody = ref('{\n  "message": "No request sent yet."\n}')
const feedbackMessage = ref('')
const feedbackTone = ref('success')
const lastRequest = ref('idle')

const responseLabel = computed(() => {
  if (lastRequest.value === 'get') {
    return 'GET /api/users'
  }

  if (lastRequest.value === 'post') {
    return 'POST /api/users'
  }

  return 'Waiting'
})

const formatJson = (value) => JSON.stringify(value, null, 2)

const updateResponse = async (requestType, requestFactory) => {
  feedbackMessage.value = ''
  lastRequest.value = requestType

  try {
    const response = await requestFactory()
    const data = await response.json()

    responseBody.value = formatJson(data)

    if (!response.ok) {
      throw new Error(data.message ?? `Request failed with status ${response.status}`)
    }

    feedbackTone.value = 'success'
    feedbackMessage.value = `${requestType === 'get' ? 'Loaded' : 'Created'} example users successfully.`
  } catch (error) {
    feedbackTone.value = 'error'
    feedbackMessage.value = error instanceof Error ? error.message : 'Request failed.'
    responseBody.value = formatJson({
      error: feedbackMessage.value,
    })
  }
}

const loadUsers = async () => {
  isLoadingUsers.value = true

  await updateResponse('get', () =>
    fetch(exampleUsersEndpoint, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    })
  )

  isLoadingUsers.value = false
}

const createUser = async () => {
  isCreatingUser.value = true

  const payload = {
    firstName: form.firstName,
    lastName: form.lastName,
    email: form.email,
  }

  await updateResponse('post', () =>
    fetch(exampleUsersEndpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(payload),
    })
  )

  isCreatingUser.value = false
}

const resetForm = () => {
  form.firstName = 'Jane'
  form.lastName = 'Doe'
  form.email = 'jane.doe@example.com'
}
</script>
