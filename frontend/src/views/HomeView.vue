<template>
  <section class="mx-auto max-w-4xl space-y-8">
    <PageHeader
      :label="$t('home.label')"
      :title="$t('home.title', { name: firstName })"
      :description="heroDescription"
    />

    <div class="grid gap-6 md:grid-cols-2">
      <article class="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
        <div class="flex items-start gap-4">
          <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-slate-950 text-base font-semibold text-white">
            {{ profileInitials }}
          </div>
          <div class="min-w-0 flex-1">
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
              {{ $t('home.summaryLabel') }}
            </p>
            <h2 class="mt-3 break-words text-2xl font-semibold text-slate-950">
              {{ auth.currentUserName.value }}
            </h2>
            <div class="mt-4 flex flex-wrap gap-2">
              <span class="rounded-full border border-slate-200 px-3 py-1 text-xs font-semibold text-slate-700">
                {{ roleLabel }}
              </span>
              <span
                class="rounded-full px-3 py-1 text-xs font-semibold"
                :class="approvalBadgeClass"
              >
                {{ approvalLabel }}
              </span>
            </div>
          </div>
        </div>

        <p class="mt-3 text-sm leading-6 text-slate-600">
          {{ summaryDescription }}
        </p>
      </article>

      <article class="rounded-[2rem] border border-slate-200 bg-slate-950 p-8 text-white shadow-[0_20px_60px_-40px_rgba(15,23,42,0.4)]">
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-300">
          {{ $t('home.nextLabel') }}
        </p>
        <h2 class="mt-3 text-2xl font-semibold">{{ nextTitle }}</h2>
        <p class="mt-3 text-sm leading-6 text-slate-300">
          {{ nextDescription }}
        </p>

        <div v-if="actionLinks.length > 0" class="mt-6 flex flex-wrap gap-3">
          <RouterLink
            v-for="(link, index) in actionLinks"
            :key="link.to"
            :to="link.to"
            class="rounded-full px-4 py-2 text-sm font-semibold transition"
            :class="index === 0
              ? 'bg-white text-slate-950 hover:bg-slate-100'
              : 'border border-white/15 text-white hover:bg-white/10'"
          >
            {{ $t(link.labelKey) }}
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
  auth.isEmployee.value
    ? t('home.descriptionEmployee')
    : auth.isRestrictedCustomer.value
      ? t('home.descriptionPending')
      : t('home.descriptionCustomer'),
)
const summaryDescription = computed(() =>
  auth.isEmployee.value
    ? t('home.summaryDescriptionEmployee')
    : auth.isRestrictedCustomer.value
      ? t('home.summaryDescriptionPending')
      : t('home.summaryDescriptionCustomer'),
)
const approvalLabel = computed(() =>
  auth.isEmployee.value
    ? t('home.statusEmployee')
    : auth.currentUser.value?.approved
      ? t('home.statusApproved')
      : t('home.statusPending'),
)
const approvalBadgeClass = computed(() =>
  auth.isEmployee.value || auth.currentUser.value?.approved
    ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-100'
    : 'bg-amber-50 text-amber-800 ring-1 ring-amber-100',
)
const roleLabel = computed(() =>
  auth.isEmployee.value ? t('home.roleEmployee') : t('home.roleCustomer'),
)
const profileInitials = computed(() => {
  const firstInitial = auth.currentUser.value?.firstName?.charAt(0) ?? ''
  const lastInitial = auth.currentUser.value?.lastName?.charAt(0) ?? ''
  const initials = `${firstInitial}${lastInitial}`.trim()

  return initials || 'DB'
})
const nextTitle = computed(() =>
  auth.isEmployee.value
    ? t('home.nextTitleEmployee')
    : auth.isRestrictedCustomer.value
      ? t('home.nextTitlePending')
      : t('home.nextTitleCustomer'),
)
const nextDescription = computed(() =>
  auth.isEmployee.value
    ? t('home.nextDescriptionEmployee')
    : auth.isRestrictedCustomer.value
      ? t('home.nextDescriptionPending')
      : t('home.nextDescriptionCustomer'),
)
const actionLinks = computed(() => {
  if (auth.isRestrictedCustomer.value) {
    return []
  }

  if (auth.isEmployee.value) {
    return [
      { to: '/customers', labelKey: 'nav.customers' },
      { to: '/customers/approvals', labelKey: 'home.approvalsAction' },
      { to: '/accounts', labelKey: 'nav.accountsDirectory' },
    ]
  }

  return [
    { to: '/accounts/me', labelKey: 'nav.accounts' },
    { to: '/transfers', labelKey: 'nav.transfers' },
  ]
})
</script>
