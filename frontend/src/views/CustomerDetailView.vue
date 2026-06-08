<template>
  <section class="mx-auto max-w-3xl space-y-6">
    <RouterLink
      to="/customers"
      class="inline-flex items-center gap-2 text-sm font-semibold text-emerald-600 transition hover:text-emerald-700"
    >
      ← {{ $t('customerDetail.backButton') }}
    </RouterLink>

    <PageHeader :label="$t('customerDetail.label')" :title="$t('customerDetail.title')" />

    <FeedbackBanner :message="errorMessage" tone="error" />

    <div
      v-if="isLoading"
      class="rounded-[2rem] border border-slate-200 bg-white/90 p-8 text-sm font-medium text-slate-600 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
    >
      {{ $t('common.loading') }}
    </div>

    <article
      v-else-if="customer"
      class="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
    >
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span class="font-mono text-sm text-slate-600">#{{ customer.id }}</span>
        <span
          class="rounded-full px-3 py-1 text-xs font-semibold"
          :class="customer.approved ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-800'"
        >
          {{ customer.approved ? $t('home.statusApproved') : $t('home.statusPending') }}
        </span>
      </div>

      <p class="mt-2 text-xs font-semibold uppercase tracking-[0.2em] text-emerald-600">
        {{ customer.role }}
      </p>
      <h2 class="mt-4 text-4xl font-semibold tracking-tight text-slate-950">
        {{ customer.firstName }} {{ customer.lastName }}
      </h2>
      <p class="mt-3 break-all text-sm font-medium text-slate-600">{{ customer.email }}</p>

      <dl class="mt-6 grid gap-3 text-sm sm:grid-cols-2">
        <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
          <dt class="font-medium text-slate-500">{{ $t('customerDetail.phoneNumberLabel') }}</dt>
          <dd class="font-semibold text-slate-900">{{ customer.phoneNumber || '-' }}</dd>
        </div>
        <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
          <dt class="font-medium text-slate-500">{{ $t('customerDetail.bsnNumberLabel') }}</dt>
          <dd class="font-semibold text-slate-900">{{ customer.bsnNumber || '-' }}</dd>
        </div>
        <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
          <dt class="font-medium text-slate-500">{{ $t('customerDetail.roleLabel') }}</dt>
          <dd class="font-semibold text-slate-900">{{ customer.role }}</dd>
        </div>
        <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
          <dt class="font-medium text-slate-500">{{ $t('customerDetail.statusLabel') }}</dt>
          <dd class="font-semibold" :class="customer.approved ? 'text-emerald-700' : 'text-amber-800'">
            {{ customer.approved ? $t('home.statusApproved') : $t('home.statusPending') }}
          </dd>
        </div>
        <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
          <dt class="font-medium text-slate-500">{{ $t('customerDetail.approvedByLabel') }}</dt>
          <dd class="font-semibold text-slate-900">{{ customer.approvedByUserId ?? '-' }}</dd>
        </div>
        <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
          <dt class="font-medium text-slate-500">{{ $t('customerDetail.approvedAtLabel') }}</dt>
          <dd class="font-semibold text-slate-900">{{ formatDateTime(customer.approvedAt) }}</dd>
        </div>
        <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
          <dt class="font-medium text-slate-500">{{ $t('customerDetail.createdAtLabel') }}</dt>
          <dd class="font-semibold text-slate-900">{{ formatDateTime(customer.createdAt) }}</dd>
        </div>
        <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
          <dt class="font-medium text-slate-500">{{ $t('customerDetail.updatedAtLabel') }}</dt>
          <dd class="font-semibold text-slate-900">{{ formatDateTime(customer.updatedAt) }}</dd>
        </div>
      </dl>
    </article>

    <TransactionHistorySection
      v-if="customer"
      :title="$t('transactionHistory.customerTitle')"
      :description="$t('transactionHistory.customerDescription')"
      :scope-label="`#${customer.id}`"
      :base-params="{ customerId: customer.id }"
      show-iban-filter
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import PageHeader from '../components/ui/PageHeader.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import TransactionHistorySection from '../components/transactions/TransactionHistorySection.vue'
import { useAuth } from '../composables/useAuth'
import * as customerService from '../services/customerService'

const props = defineProps({
  customerId: {
    type: String,
    required: true,
  },
})

const auth = useAuth()
const { locale } = useI18n()

const customer = ref(null)
const isLoading = ref(false)
const errorMessage = ref('')

const formatDateTime = (value) =>
  value
    ? new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
    : '-'

const loadCustomer = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    customer.value = await customerService.getCustomer(props.customerId, auth.accessToken.value)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

onMounted(loadCustomer)
</script>
