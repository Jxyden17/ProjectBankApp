<template>
  <section class="mx-auto max-w-6xl space-y-6">
    <PageHeader
      :label="$t('accountsDirectory.label')"
      :title="$t('accountsDirectory.title')"
      :description="$t('accountsDirectory.description')"
    />

    <FeedbackBanner :message="errorMessage" tone="error" />

    <div
      v-if="isLoading"
      class="rounded-4xl border border-slate-200 bg-white/90 p-8 text-sm font-medium text-slate-600 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
    >
      {{ $t('common.loading') }}
    </div>

    <div
      v-else-if="!errorMessage && accounts.length === 0"
      class="rounded-4xl border border-dashed border-slate-300 bg-white/80 p-8 text-center"
    >
      <h2 class="text-2xl font-semibold text-slate-950">{{ $t('accountsDirectory.emptyTitle') }}</h2>
      <p class="mt-3 text-sm leading-6 text-slate-600">{{ $t('accountsDirectory.emptyDescription') }}</p>
    </div>

    <div v-else class="space-y-4">
      <p class="text-sm font-medium text-slate-500">
        {{ $t('accountsDirectory.resultsLabel', { count: accounts.length }) }}
      </p>

      <RouterLink
        v-for="account in accounts"
        :key="account.id"
        :to="{ name: 'account-detail', params: { accountId: account.id } }"
        class="flex flex-wrap items-center justify-between gap-4 rounded-4xl border border-slate-200 bg-white p-6 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)] transition hover:border-emerald-300 hover:shadow-[0_24px_70px_-40px_rgba(16,185,129,0.45)]"
      >
        <div class="space-y-1">
          <div class="flex flex-wrap items-center gap-3">
            <span class="font-mono text-sm text-slate-600">{{ account.iban }}</span>
            <span
              class="rounded-full px-3 py-1 text-xs font-semibold"
              :class="account.isActive ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'"
            >
              {{ account.isActive ? $t('accountsDirectory.statusActive') : $t('accountsDirectory.statusInactive') }}
            </span>
          </div>
          <p class="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-600">
            {{ accountTypeLabel(account.accountType) }}
          </p>
          <p class="text-sm text-slate-500">
            {{ $t('accountsDirectory.ownerLabel') }}: <span class="font-semibold text-slate-900">{{ account.userId }}</span>
          </p>
        </div>

        <div class="flex items-center gap-5">
          <div class="text-right">
            <p class="text-xs font-medium uppercase tracking-[0.2em] text-slate-400">
              {{ $t('accountsDirectory.balanceLabel') }}
            </p>
            <p class="text-2xl font-semibold text-slate-950">{{ formatCurrency(account.balance) }}</p>
          </div>
          <span class="text-sm font-semibold text-emerald-600">{{ $t('accountsDirectory.detailsButton') }} →</span>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import PageHeader from '../components/ui/PageHeader.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import { useAuth } from '../composables/useAuth'
import * as accountService from '../services/accountService'

const auth = useAuth()
const { t, locale } = useI18n()

const accounts = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

const formatCurrency = (value) =>
  new Intl.NumberFormat(locale.value, { style: 'currency', currency: 'EUR' }).format(Number(value ?? 0))

const accountTypeLabel = (accountType) =>
  accountType === 'SAVINGS' ? t('accounts.savingsType') : t('accounts.checkingType')

const loadAccounts = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const data = await accountService.listAccounts(auth.accessToken.value)
    accounts.value = data ?? []
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

onMounted(loadAccounts)
</script>
