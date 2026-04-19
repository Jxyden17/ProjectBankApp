<template>
  <section class="mx-auto max-w-4xl space-y-8">
    <PageHeader :label="$t('home.label')" :title="$t('home.title')">
      <i18n-t keypath="home.description" tag="span">
        <template #endpoint>
          <code class="rounded bg-slate-200 px-2 py-1 text-slate-900">{{ usersEndpoint }}</code>
        </template>
      </i18n-t>
    </PageHeader>

    <div class="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
      <div class="space-y-6">
        <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div class="flex items-center justify-between gap-4">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">{{ $t('home.loadCard.method') }}</p>
              <h2 class="mt-2 text-xl font-semibold text-slate-950">{{ $t('home.loadCard.title') }}</h2>
            </div>
            <Button :disabled="isLoading" @click="loadUsers">
              {{ isLoading ? $t('common.loading') : $t('home.loadCard.button') }}
            </Button>
          </div>
          <p class="mt-4 text-sm leading-6 text-slate-600">
            {{ $t('home.loadCard.description') }}
          </p>
        </article>

        <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">{{ $t('home.createCard.method') }}</p>
            <h2 class="mt-2 text-xl font-semibold text-slate-950">{{ $t('home.createCard.title') }}</h2>
          </div>

          <form class="mt-5 space-y-4" @submit.prevent="createUser">
            <TextInput v-model="form.firstName" :label="$t('home.form.firstName')" required />
            <TextInput v-model="form.lastName" :label="$t('home.form.lastName')" required />
            <TextInput v-model="form.email" :label="$t('home.form.email')" type="email" required />

            <div class="flex flex-wrap gap-3">
              <Button type="submit" variant="success" :disabled="isCreating">
                {{ isCreating ? $t('common.creating') : $t('home.createCard.button') }}
              </Button>
              <Button variant="secondary" @click="resetForm">{{ $t('common.reset') }}</Button>
            </div>
          </form>
        </article>
      </div>

      <article class="rounded-3xl border border-slate-200 bg-slate-950 p-6 text-white shadow-sm">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-300">{{ $t('home.response.label') }}</p>
            <h2 class="mt-2 text-xl font-semibold">{{ $t('home.response.title') }}</h2>
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
