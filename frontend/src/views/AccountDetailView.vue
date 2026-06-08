<template>
  <section class="mx-auto max-w-3xl space-y-6">
    <RouterLink
      :to="backTo"
      class="inline-flex items-center gap-2 text-sm font-semibold text-emerald-600 transition hover:text-emerald-700"
    >
      ← {{ $t('accountDetail.backButton') }}
    </RouterLink>

    <PageHeader :label="$t('accountDetail.label')" :title="$t('accountDetail.title')" />

    <FeedbackBanner :message="errorMessage" tone="error" />

    <div
      v-if="isLoading"
      class="rounded-4xl border border-slate-200 bg-white/90 p-8 text-sm font-medium text-slate-600 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
    >
      {{ $t('common.loading') }}
    </div>

    <template v-else-if="account">
      <article class="rounded-4xl border border-slate-200 bg-white p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <span class="font-mono text-sm text-slate-600">{{ account.iban }}</span>
          <span
            class="rounded-full px-3 py-1 text-xs font-semibold"
            :class="account.isActive ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'"
          >
            {{ account.isActive ? $t('accountDetail.statusActive') : $t('accountDetail.statusInactive') }}
          </span>
        </div>
        <p class="mt-2 text-xs font-semibold uppercase tracking-[0.2em] text-emerald-600">
          {{ accountTypeLabel(account.accountType) }}
        </p>
        <p class="mt-4 text-4xl font-semibold tracking-tight text-slate-950">{{ formatCurrency(account.balance) }}</p>

        <dl class="mt-6 grid gap-3 text-sm sm:grid-cols-2">
          <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
            <dt class="font-medium text-slate-500">{{ $t('accountDetail.ownerLabel') }}</dt>
            <dd class="font-semibold text-slate-900">{{ account.userId }}</dd>
          </div>
          <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
            <dt class="font-medium text-slate-500">{{ $t('accountDetail.statusLabel') }}</dt>
            <dd class="font-semibold" :class="account.isActive ? 'text-emerald-700' : 'text-rose-700'">
              {{ account.isActive ? $t('accountDetail.statusActive') : $t('accountDetail.statusInactive') }}
            </dd>
          </div>
          <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
            <dt class="font-medium text-slate-500">{{ $t('accounts.absoluteLimitLabel') }}</dt>
            <dd class="font-semibold text-slate-900">{{ formatCurrency(account.absoluteTransferLimit) }}</dd>
          </div>
          <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
            <dt class="font-medium text-slate-500">{{ $t('accounts.dailyLimitLabel') }}</dt>
            <dd class="font-semibold text-slate-900">{{ formatCurrency(account.dailyTransferLimit) }}</dd>
          </div>
          <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
            <dt class="font-medium text-slate-500">{{ $t('accountDetail.createdAtLabel') }}</dt>
            <dd class="font-semibold text-slate-900">{{ formatDateTime(account.createdAt) }}</dd>
          </div>
          <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
            <dt class="font-medium text-slate-500">{{ $t('accountDetail.updatedAtLabel') }}</dt>
            <dd class="font-semibold text-slate-900">{{ formatDateTime(account.updatedAt) }}</dd>
          </div>
        </dl>
      </article>

      <form
        v-if="auth.isEmployee.value"
        class="space-y-5 rounded-4xl border border-slate-200 bg-white/90 p-8 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
        @submit.prevent="save"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
          {{ $t('accountDetail.editTitle') }}
        </p>

        <FeedbackBanner :message="successMessage" tone="success" />
        <FeedbackBanner :message="editError" tone="error" />

        <div class="grid gap-4 md:grid-cols-2">
          <TextInput
            v-model="form.absoluteTransferLimit"
            :label="$t('accounts.absoluteLimitLabel')"
            type="number"
            step="0.01"
            :trim="false"
          />
          <TextInput
            v-model="form.dailyTransferLimit"
            :label="$t('accounts.dailyLimitLabel')"
            type="number"
            min="0"
            step="0.01"
            :trim="false"
          />
          <label class="block space-y-2">
            <span class="text-sm font-medium text-slate-700">{{ $t('accountDetail.statusLabel') }}</span>
            <select
              v-model="form.isActive"
              class="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
            >
              <option value="true">{{ $t('accountDetail.statusActive') }}</option>
              <option value="false">{{ $t('accountDetail.statusInactive') }}</option>
            </select>
          </label>
        </div>

        <Button type="submit" variant="primary" :disabled="isSaving">
          {{ isSaving ? $t('common.loading') : $t('accountDetail.saveButton') }}
        </Button>
      </form>

      <TransactionHistorySection
        :title="$t('transactionHistory.accountTitle')"
        :description="$t('transactionHistory.accountDescription')"
        :scope-label="account.iban"
        :base-params="{ iban: account.iban }"
      />
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import PageHeader from '../components/ui/PageHeader.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import TextInput from '../components/ui/TextInput.vue'
import Button from '../components/ui/Button.vue'
import TransactionHistorySection from '../components/transactions/TransactionHistorySection.vue'
import { useAuth } from '../composables/useAuth'
import * as accountService from '../services/accountService'

const props = defineProps({
  accountId: {
    type: String,
    required: true,
  },
})

const auth = useAuth()
const { t, locale } = useI18n()

const account = ref(null)
const isLoading = ref(false)
const errorMessage = ref('')

const form = reactive({ absoluteTransferLimit: '', dailyTransferLimit: '', isActive: 'true' })
const isSaving = ref(false)
const successMessage = ref('')
const editError = ref('')

const backTo = computed(() => (auth.isEmployee.value ? '/accounts' : '/accounts/me'))

const formatCurrency = (value) =>
  new Intl.NumberFormat(locale.value, { style: 'currency', currency: 'EUR' }).format(Number(value ?? 0))

const formatDateTime = (value) =>
  value
    ? new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
    : '-'

const accountTypeLabel = (accountType) =>
  accountType === 'SAVINGS' ? t('accounts.savingsType') : t('accounts.checkingType')

const syncForm = () => {
  if (!account.value) return
  form.absoluteTransferLimit = String(account.value.absoluteTransferLimit)
  form.dailyTransferLimit = String(account.value.dailyTransferLimit)
  form.isActive = account.value.isActive ? 'true' : 'false'
}

const loadAccount = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    account.value = await accountService.getAccountById(props.accountId, auth.accessToken.value)
    syncForm()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

const save = async () => {
  isSaving.value = true
  successMessage.value = ''
  editError.value = ''

  try {
    account.value = await accountService.updateAccount(
      account.value.id,
      {
        absoluteTransferLimit: Number.parseFloat(form.absoluteTransferLimit),
        dailyTransferLimit: Number.parseFloat(form.dailyTransferLimit),
        isActive: form.isActive === 'true',
      },
      auth.accessToken.value,
    )
    syncForm()
    successMessage.value = t('accountDetail.updateSuccess')
  } catch (error) {
    editError.value = error.message
  } finally {
    isSaving.value = false
  }
}

onMounted(loadAccount)
</script>
