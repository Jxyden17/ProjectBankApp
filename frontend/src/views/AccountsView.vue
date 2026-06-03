<template>
  <section class="mx-auto max-w-5xl space-y-6">
    <PageHeader
      :label="$t('accounts.label')"
      :title="$t('accounts.title')"
      :description="$t('accounts.description')"
    />

    <FeedbackBanner :message="errorMessage" tone="error" />

    <div
      v-if="isLoading"
      class="rounded-[2rem] border border-slate-200 bg-white/90 p-8 text-sm font-medium text-slate-600 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
    >
      {{ $t('common.loading') }}
    </div>

    <div
      v-else-if="accounts.length === 0"
      class="rounded-[2rem] border border-dashed border-slate-300 bg-white/80 p-8 text-center"
    >
      <h2 class="text-2xl font-semibold text-slate-950">{{ $t('accounts.emptyTitle') }}</h2>
      <p class="mt-3 text-sm leading-6 text-slate-600">{{ $t('accounts.emptyDescription') }}</p>
    </div>

    <div v-else class="space-y-6">
      <article class="rounded-[2rem] border border-slate-200 bg-slate-950 p-8 text-white shadow-[0_20px_60px_-40px_rgba(15,23,42,0.4)]">
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-300">
          {{ $t('accounts.combinedBalanceLabel') }}
        </p>
        <p class="mt-3 text-4xl font-semibold tracking-tight">{{ formatCurrency(combinedBalance) }}</p>
      </article>

      <div class="grid gap-5 md:grid-cols-2">
        <article
          v-for="account in accounts"
          :key="account.id"
          class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
        >
          <div class="flex items-center justify-between">
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
              {{ accountTypeLabel(account.accountType) }}
            </p>
            <span class="font-mono text-xs text-slate-500">{{ account.iban }}</span>
          </div>
          <p class="mt-4 text-3xl font-semibold text-slate-950">{{ formatCurrency(account.balance) }}</p>

          <dl class="mt-5 space-y-3 text-sm">
            <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
              <dt class="font-medium text-slate-500">{{ $t('accounts.absoluteLimitLabel') }}</dt>
              <dd class="font-semibold text-slate-900">{{ formatCurrency(account.absoluteTransferLimit) }}</dd>
            </div>
            <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
              <dt class="font-medium text-slate-500">{{ $t('accounts.dailyLimitLabel') }}</dt>
              <dd class="font-semibold text-slate-900">{{ formatCurrency(account.dailyTransferLimit) }}</dd>
            </div>
          </dl>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '../components/ui/PageHeader.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import { useAuth } from '../composables/useAuth'
import * as accountService from '../services/accountService'

const auth = useAuth()
const { t, locale } = useI18n()

const accounts = ref([])
const combinedBalance = ref(0)
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
    const overview = await accountService.getOwnAccounts(auth.accessToken.value)
    accounts.value = overview?.accounts ?? []
    combinedBalance.value = overview?.combinedBalance ?? 0
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

onMounted(loadAccounts)
</script>
