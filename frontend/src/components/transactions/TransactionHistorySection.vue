<template>
  <section class="space-y-6 rounded-[2rem] border border-slate-200 bg-white/90 p-6 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
    <div class="space-y-2">
      <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
        {{ $t('transactionHistory.label') }}
      </p>
      <h2 class="text-2xl font-semibold text-slate-950">{{ title }}</h2>
      <p class="max-w-3xl text-sm leading-6 text-slate-600">{{ description }}</p>
      <p v-if="scopeLabel" class="text-xs font-medium text-slate-500">
        {{ $t('transactionHistory.scopeLabel') }}: <span class="font-semibold text-slate-700">{{ scopeLabel }}</span>
      </p>
    </div>

    <FeedbackBanner :message="errorMessage" tone="error" />

    <form class="grid gap-4 rounded-[1.75rem] border border-slate-200 bg-slate-50 p-5 lg:grid-cols-6" @submit.prevent="applyFilters">
      <TextInput v-model="filters.startDate" :label="$t('transactionHistory.startDateLabel')" type="date" :trim="false" />
      <TextInput v-model="filters.endDate" :label="$t('transactionHistory.endDateLabel')" type="date" :trim="false" />
      <TextInput v-model="filters.amountEq" :label="$t('transactionHistory.amountEqLabel')" type="number" step="0.01" :trim="false" />
      <TextInput v-model="filters.amountLt" :label="$t('transactionHistory.amountLtLabel')" type="number" step="0.01" :trim="false" />
      <TextInput v-model="filters.amountGt" :label="$t('transactionHistory.amountGtLabel')" type="number" step="0.01" :trim="false" />

      <label v-if="showTransactionTypeFilter" class="block space-y-2 lg:col-span-2">
        <span class="text-sm font-medium text-slate-700">{{ $t('transactionHistory.transactionTypeLabel') }}</span>
        <select
          v-model="filters.transactionType"
          class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
        >
          <option value="">{{ $t('transactionHistory.allTypes') }}</option>
          <option value="TRANSFER">{{ $t('transactionHistory.transferType') }}</option>
          <option value="DEPOSIT">{{ $t('transactionHistory.depositType') }}</option>
          <option value="WITHDRAWAL">{{ $t('transactionHistory.withdrawalType') }}</option>
        </select>
      </label>

      <TextInput
        v-if="showIbanFilter"
        v-model="filters.iban"
        :label="$t('transactionHistory.ibanLabel')"
        class="lg:col-span-2"
      />

      <div class="flex items-end gap-2 lg:col-span-6">
        <Button type="submit" variant="primary" :disabled="isLoading">
          {{ isLoading ? $t('common.loading') : $t('transactionHistory.applyButton') }}
        </Button>
        <Button type="button" variant="secondary" :disabled="isLoading" @click="resetFilters">
          {{ $t('transactionHistory.resetButton') }}
        </Button>
      </div>
    </form>

    <div
      v-if="isLoading"
      class="rounded-[1.5rem] border border-slate-200 bg-white p-8 text-sm font-medium text-slate-600"
    >
      {{ $t('common.loading') }}
    </div>

    <div
      v-else-if="!errorMessage && transactions.length === 0"
      class="rounded-[1.5rem] border border-dashed border-slate-300 bg-white/70 p-8 text-center"
    >
      <p class="text-sm font-semibold uppercase tracking-[0.28em] text-emerald-600">
        {{ $t('transactionHistory.emptyLabel') }}
      </p>
      <h3 class="mt-3 text-2xl font-semibold text-slate-950">
        {{ $t('transactionHistory.emptyTitle') }}
      </h3>
      <p class="mt-3 text-sm leading-6 text-slate-600">
        {{ $t('transactionHistory.emptyDescription') }}
      </p>
    </div>

    <div v-else class="space-y-4">
      <article
        v-for="transaction in transactions"
        :key="transaction.id"
        class="overflow-hidden rounded-[1.5rem] border border-slate-200 bg-white"
      >
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
          <div class="flex flex-wrap items-center gap-3">
            <span
              class="rounded-full px-3 py-1 text-xs font-semibold"
              :class="transactionTypeClass(transaction.transactionType)"
            >
              {{ transaction.transactionType }}
            </span>
            <span class="font-semibold text-slate-950">{{ formatAmount(transaction.amount, transaction.currency) }}</span>
          </div>
          <span class="text-sm font-medium text-slate-500">{{ formatDateTime(transaction.timestamp) }}</span>
        </div>

        <div class="grid gap-4 px-5 py-5 md:grid-cols-2 xl:grid-cols-3">
          <dl class="space-y-3 text-sm">
            <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
              <dt class="font-medium text-slate-500">{{ $t('transactionHistory.fromLabel') }}</dt>
              <dd class="font-semibold text-slate-900">{{ transaction.fromAccountId ?? '-' }}</dd>
            </div>
            <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
              <dt class="font-medium text-slate-500">{{ $t('transactionHistory.toLabel') }}</dt>
              <dd class="font-semibold text-slate-900">{{ transaction.toAccountId ?? '-' }}</dd>
            </div>
            <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
              <dt class="font-medium text-slate-500">{{ $t('transactionHistory.channelLabel') }}</dt>
              <dd class="font-semibold text-slate-900">{{ transaction.channel ?? '-' }}</dd>
            </div>
          </dl>

          <dl class="space-y-3 text-sm">
            <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
              <dt class="font-medium text-slate-500">{{ $t('transactionHistory.initiatedByLabel') }}</dt>
              <dd class="font-semibold text-slate-900">{{ transaction.initiatedByUserId ?? '-' }}</dd>
            </div>
            <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
              <dt class="font-medium text-slate-500">{{ $t('transactionHistory.transactionIdLabel') }}</dt>
              <dd class="font-semibold text-slate-900">#{{ transaction.id }}</dd>
            </div>
            <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
              <dt class="font-medium text-slate-500">{{ $t('transactionHistory.timestampLabel') }}</dt>
              <dd class="font-semibold text-slate-900">{{ formatDateTime(transaction.timestamp) }}</dd>
            </div>
          </dl>

          <div class="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600 xl:col-span-1">
            <p class="font-medium text-slate-500">{{ $t('transactionHistory.descriptionLabel') }}</p>
            <p class="mt-2 leading-6 text-slate-700">{{ transaction.description || '-' }}</p>
          </div>
        </div>
      </article>
    </div>

    <div
      v-if="page.totalPages > 1"
      class="flex items-center justify-between rounded-full border border-slate-200 bg-white px-4 py-3"
    >
      <Button variant="secondary" :disabled="isLoading || page.page === 0" @click="goToPage(page.page - 1)">
        {{ $t('transactionHistory.previousPage') }}
      </Button>
      <span class="text-sm font-semibold text-slate-600">
        {{ $t('transactionHistory.pageLabel', { current: page.page + 1, total: page.totalPages }) }}
      </span>
      <Button variant="secondary" :disabled="isLoading || page.page >= page.totalPages - 1" @click="goToPage(page.page + 1)">
        {{ $t('transactionHistory.nextPage') }}
      </Button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from '../ui/Button.vue'
import FeedbackBanner from '../ui/FeedbackBanner.vue'
import TextInput from '../ui/TextInput.vue'
import { useAuth } from '../../composables/useAuth'
import * as transactionService from '../../services/transactionService'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
  baseParams: {
    type: Object,
    default: () => ({}),
  },
  scopeLabel: {
    type: String,
    default: '',
  },
  showIbanFilter: {
    type: Boolean,
    default: false,
  },
  showTransactionTypeFilter: {
    type: Boolean,
    default: true,
  },
})

const auth = useAuth()
const { locale } = useI18n()

const filters = reactive({
  startDate: '',
  endDate: '',
  amountEq: '',
  amountLt: '',
  amountGt: '',
  iban: '',
  transactionType: '',
})

const transactions = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const page = ref({
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
})

const buildRequestParams = () => ({
  ...props.baseParams,
  startDate: filters.startDate,
  endDate: filters.endDate,
  amountEq: filters.amountEq,
  amountLt: filters.amountLt,
  amountGt: filters.amountGt,
  ...(props.showIbanFilter ? { iban: filters.iban } : {}),
  transactionType: filters.transactionType,
  page: page.value.page,
  size: page.value.size,
  sort: 'timestamp,desc',
})

const formatAmount = (value, currency = 'EUR') =>
  new Intl.NumberFormat(locale.value, { style: 'currency', currency }).format(Number(value ?? 0))

const formatDateTime = (value) =>
  value
    ? new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
    : '-'

const transactionTypeClass = (transactionType) => {
  if (transactionType === 'DEPOSIT') return 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-100'
  if (transactionType === 'WITHDRAWAL') return 'bg-rose-50 text-rose-700 ring-1 ring-rose-100'
  return 'bg-slate-100 text-slate-700 ring-1 ring-slate-200'
}

const loadTransactions = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await transactionService.getTransactions(buildRequestParams(), auth.accessToken.value)
    transactions.value = response?.items ?? []
    page.value = response?.page ?? page.value
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

const applyFilters = async () => {
  page.value.page = 0
  await loadTransactions()
}

const resetFilters = async () => {
  filters.startDate = ''
  filters.endDate = ''
  filters.amountEq = ''
  filters.amountLt = ''
  filters.amountGt = ''
  filters.iban = ''
  filters.transactionType = ''
  await applyFilters()
}

const goToPage = async (nextPage) => {
  page.value.page = nextPage
  await loadTransactions()
}

onMounted(loadTransactions)
</script>
