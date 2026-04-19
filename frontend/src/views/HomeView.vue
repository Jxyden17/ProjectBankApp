<template>
  <section class="mx-auto max-w-4xl space-y-8">
    <PageHeader label="API Test" title="Example users endpoint">
      Use this page to test the backend at
      <code class="rounded bg-slate-200 px-2 py-1 text-slate-900">{{ usersEndpoint }}</code>.
    </PageHeader>

    <div class="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
      <div class="space-y-6">
        <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div class="flex items-center justify-between gap-4">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">GET</p>
              <h2 class="mt-2 text-xl font-semibold text-slate-950">Load users</h2>
            </div>
            <Button :disabled="isLoading" @click="loadUsers">
              {{ isLoading ? 'Loading...' : 'Load users' }}
            </Button>
          </div>
          <p class="mt-4 text-sm leading-6 text-slate-600">
            Fetch the current example users from the Spring backend.
          </p>
        </article>

        <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">POST</p>
            <h2 class="mt-2 text-xl font-semibold text-slate-950">Create user</h2>
          </div>

          <form class="mt-5 space-y-4" @submit.prevent="createUser">
            <TextInput v-model="form.firstName" label="First name" required />
            <TextInput v-model="form.lastName" label="Last name" required />
            <TextInput v-model="form.email" label="Email" type="email" required />

            <div class="flex flex-wrap gap-3">
              <Button type="submit" variant="success" :disabled="isCreating">
                {{ isCreating ? 'Creating...' : 'Create user' }}
              </Button>
              <Button variant="secondary" @click="resetForm">Reset</Button>
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

        <FeedbackBanner class="mt-5" :message="feedbackMessage" :tone="feedbackTone" />

        <pre class="mt-5 min-h-80 overflow-x-auto rounded-2xl bg-black/30 p-4 text-sm leading-6 text-slate-200">{{ responseBody }}</pre>
      </article>
    </div>
  </section>
</template>

<script setup>
import PageHeader from '../components/ui/PageHeader.vue'
import Button from '../components/ui/Button.vue'
import TextInput from '../components/ui/TextInput.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import { usersEndpoint } from '../services/userService'
import { useUsers } from '../composables/useUsers'

const {
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
} = useUsers()
</script>
