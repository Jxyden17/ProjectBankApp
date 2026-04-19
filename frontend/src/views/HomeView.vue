<template>
  <section class="mx-auto max-w-4xl space-y-8">
    <PageHeader
      :label="$t('home.label')"
      :title="$t('home.title', { name: firstName })"
      :description="heroDescription"
    />

    <div class="grid gap-6 md:grid-cols-2">
      <article class="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
          {{ $t('home.summaryLabel') }}
        </p>
        <h2 class="mt-3 text-2xl font-semibold text-slate-950">
          {{ auth.currentUserName.value }}
        </h2>
        <p class="mt-3 text-sm leading-6 text-slate-600">
          {{ summaryDescription }}
        </p>

        <dl class="mt-6 space-y-4 text-sm">
          <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
            <dt class="font-medium text-slate-500">{{ $t('home.roleLabel') }}</dt>
            <dd class="font-semibold text-slate-900">{{ auth.currentUser.value?.role ?? '-' }}</dd>
          </div>
          <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
            <dt class="font-medium text-slate-500">{{ $t('home.statusLabel') }}</dt>
            <dd class="font-semibold" :class="approvalTone">
              {{ approvalLabel }}
            </dd>
          </div>
        </dl>
      </article>

      <article class="rounded-[2rem] border border-slate-200 bg-slate-950 p-8 text-white shadow-[0_20px_60px_-40px_rgba(15,23,42,0.4)]">
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-300">
          {{ $t('home.nextLabel') }}
        </p>
        <h2 class="mt-3 text-2xl font-semibold">{{ nextTitle }}</h2>
        <p class="mt-3 text-sm leading-6 text-slate-300">
          {{ nextDescription }}
        </p>

        <div v-if="!auth.isRestrictedCustomer.value" class="mt-6 flex flex-wrap gap-3">
          <RouterLink
            to="/accounts"
            class="rounded-full bg-white px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-slate-100"
          >
            {{ $t('nav.accounts') }}
          </RouterLink>
          <RouterLink
            to="/transfers"
            class="rounded-full border border-white/15 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/10"
          >
            {{ $t('nav.transfers') }}
          </RouterLink>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import PageHeader from '../components/ui/PageHeader.vue'
import { useAuth } from '../composables/useAuth'

const auth = useAuth()
const { t } = useI18n()

const firstName = computed(() => auth.currentUser.value?.firstName ?? '')
const heroDescription = computed(() =>
  auth.isRestrictedCustomer.value ? t('home.descriptionPending') : t('home.description'),
)
const summaryDescription = computed(() =>
  auth.isRestrictedCustomer.value
    ? t('home.summaryDescriptionPending')
    : t('home.summaryDescription'),
)
const approvalLabel = computed(() =>
  auth.currentUser.value?.approved ? t('home.statusApproved') : t('home.statusPending'),
)
const approvalTone = computed(() =>
  auth.currentUser.value?.approved ? 'text-emerald-700' : 'text-amber-700',
)
const nextTitle = computed(() =>
  auth.isRestrictedCustomer.value ? t('home.nextTitlePending') : t('home.nextTitle'),
)
const nextDescription = computed(() =>
  auth.isRestrictedCustomer.value ? t('home.nextDescriptionPending') : t('home.nextDescription'),
)
</script>
