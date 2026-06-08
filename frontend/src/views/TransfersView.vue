<template>
  <section class="mx-auto max-w-6xl space-y-6">
    <PageHeader
      :label="$t('transfers.label')"
      :title="$t('transfers.title')"
      :description="$t('transfers.description')"
    />

    <FeedbackBanner :message="successMessage" tone="success" />
    <FeedbackBanner :message="errorMessage" tone="error" />

    <div
      v-if="isLoadingAccounts"
      class="rounded-[2rem] border border-slate-200 bg-white/90 p-8 text-sm font-medium text-slate-600 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
    >
      {{ $t('common.loading') }}
    </div>

    <div
      v-else-if="!hasTransferOptions"
      class="rounded-[2rem] border border-dashed border-slate-300 bg-white/80 p-8 text-center"
    >
      <p class="text-sm font-semibold uppercase tracking-[0.28em] text-emerald-600">
        {{ $t('transfers.emptyLabel') }}
      </p>
      <h2 class="mt-3 text-2xl font-semibold text-slate-950">
        {{ $t('transfers.emptyTitle') }}
      </h2>
      <p class="mt-3 text-sm leading-6 text-slate-600">
        {{ $t('transfers.emptyDescription') }}
      </p>
    </div>

    <template v-else>
      <article
        v-if="!auth.isEmployee.value"
        class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
      >
        <div class="space-y-2">
          <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
            {{ $t('transfers.personalLabel') }}
          </p>
          <h2 class="text-2xl font-semibold text-slate-950">{{ $t('transfers.ownTransferTitle') }}</h2>
          <p class="max-w-3xl text-sm leading-6 text-slate-600">
            {{ $t('transfers.ownTransferDescription') }}
          </p>
        </div>

        <form class="mt-6 grid gap-4 md:grid-cols-2" @submit.prevent="submitOwnTransfer">
          <label class="block space-y-2">
            <span class="text-sm font-medium text-slate-700">{{ $t('transfers.sourceAccountLabel') }}</span>
            <select
              v-model="ownTransferForm.fromAccountId"
              class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
            >
              <option value="">{{ $t('transfers.chooseAccountOption') }}</option>
              <option v-for="account in ownTransferSourceAccounts" :key="account.id" :value="String(account.id)">
                {{ accountOptionLabel(account) }}
              </option>
            </select>
          </label>

          <label class="block space-y-2">
            <span class="text-sm font-medium text-slate-700">{{ $t('transfers.destinationAccountLabel') }}</span>
            <select
              v-model="ownTransferForm.toAccountId"
              class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
            >
              <option value="">{{ $t('transfers.chooseAccountOption') }}</option>
              <option v-for="account in ownTransferDestinationAccounts" :key="account.id" :value="String(account.id)">
                {{ accountOptionLabel(account) }}
              </option>
            </select>
          </label>

          <TextInput
            v-model="ownTransferForm.amount"
            :label="$t('transfers.amountLabel')"
            type="number"
            step="0.01"
            min="0.01"
            required
            :trim="false"
          />
          <TextInput
            v-model="ownTransferForm.description"
            :label="$t('transfers.descriptionFieldLabel')"
          />

          <div class="md:col-span-2">
            <p class="text-xs font-medium text-slate-500">
              {{ $t('transfers.channelLabel') }}: <span class="font-semibold text-slate-700">WEB</span>
            </p>
            <Button class="mt-3" type="submit" variant="primary" :disabled="isSubmittingOwnTransfer">
              {{ isSubmittingOwnTransfer ? $t('common.loading') : $t('transfers.sendButton') }}
            </Button>
          </div>
        </form>
      </article>

      <article
        v-if="!auth.isEmployee.value"
        class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
      >
        <div class="space-y-2">
          <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
            {{ $t('transfers.externalLabel') }}
          </p>
          <h2 class="text-2xl font-semibold text-slate-950">{{ $t('transfers.externalTransferTitle') }}</h2>
          <p class="max-w-3xl text-sm leading-6 text-slate-600">
            {{ $t('transfers.externalTransferDescription') }}
          </p>
        </div>

        <form class="mt-6 grid gap-4 md:grid-cols-2" @submit.prevent="searchCustomerIban">
          <TextInput
            v-model="lookupForm.firstName"
            :label="$t('transfers.lookupFirstNameLabel')"
            required
          />
          <TextInput
            v-model="lookupForm.lastName"
            :label="$t('transfers.lookupLastNameLabel')"
            required
          />
          <div class="md:col-span-2">
            <Button type="submit" variant="secondary" :disabled="isLookupLoading">
              {{ isLookupLoading ? $t('common.loading') : $t('transfers.lookupButton') }}
            </Button>
          </div>
        </form>

        <FeedbackBanner :message="lookupErrorMessage" tone="error" class="mt-4" />

        <div
          v-if="lookupResults.length > 0"
          class="mt-5 space-y-3 rounded-[1.5rem] border border-slate-200 bg-slate-50 p-4"
        >
          <div class="flex items-center justify-between gap-3">
            <p class="text-sm font-semibold text-slate-700">
              {{ $t('transfers.lookupResultsLabel', { count: lookupResults.length }) }}
            </p>
            <p class="text-xs font-medium text-slate-500">
              {{ $t('transactionHistory.pageLabel', { current: lookupPage.page + 1, total: lookupPage.totalPages }) }}
            </p>
          </div>

          <button
            v-for="result in lookupResults"
            :key="`${result.firstName}-${result.lastName}-${result.iban}`"
            type="button"
            class="flex w-full flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-left transition hover:border-emerald-300"
            @click="selectLookupResult(result.iban)"
          >
            <div>
              <p class="text-sm font-semibold text-slate-950">
                {{ result.firstName }} {{ result.lastName }}
              </p>
              <p class="mt-1 font-mono text-sm text-slate-500">{{ result.iban }}</p>
            </div>
            <span class="text-sm font-semibold text-emerald-600">
              {{ $t('transfers.selectIbanButton') }}
            </span>
          </button>

          <div
            v-if="lookupPage.totalPages > 1"
            class="flex items-center justify-between rounded-full border border-slate-200 bg-white px-4 py-3"
          >
            <Button
              variant="secondary"
              :disabled="isLookupLoading || lookupPage.page === 0"
              @click="goToLookupPage(lookupPage.page - 1)"
            >
              {{ $t('transfers.previousPage') }}
            </Button>
            <Button
              variant="secondary"
              :disabled="isLookupLoading || lookupPage.page >= lookupPage.totalPages - 1"
              @click="goToLookupPage(lookupPage.page + 1)"
            >
              {{ $t('transfers.nextPage') }}
            </Button>
          </div>
        </div>

        <form class="mt-6 grid gap-4 md:grid-cols-2" @submit.prevent="submitOwnExternalTransfer">
          <label class="block space-y-2">
            <span class="text-sm font-medium text-slate-700">{{ $t('transfers.sourceAccountLabel') }}</span>
            <select
              v-model="externalTransferForm.fromAccountId"
              class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
            >
              <option value="">{{ $t('transfers.chooseAccountOption') }}</option>
              <option v-for="account in ownCheckingAccounts" :key="account.id" :value="String(account.id)">
                {{ accountOptionLabel(account) }}
              </option>
            </select>
          </label>

          <TextInput
            v-model="externalTransferForm.destinationIban"
            :label="$t('transfers.destinationIbanLabel')"
            required
          />
          <TextInput
            v-model="externalTransferForm.amount"
            :label="$t('transfers.amountLabel')"
            type="number"
            step="0.01"
            min="0.01"
            required
            :trim="false"
          />
          <TextInput
            v-model="externalTransferForm.description"
            :label="$t('transfers.descriptionFieldLabel')"
          />

          <div class="md:col-span-2">
            <p class="text-xs font-medium text-slate-500">
              {{ $t('transfers.channelLabel') }}: <span class="font-semibold text-slate-700">WEB</span>
            </p>
            <Button class="mt-3" type="submit" variant="primary" :disabled="isSubmittingExternalTransfer">
              {{ isSubmittingExternalTransfer ? $t('common.loading') : $t('transfers.sendButton') }}
            </Button>
          </div>
        </form>
      </article>

      <article
        v-else
        class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
      >
        <div class="space-y-2">
          <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
            {{ $t('transfers.employeeLabel') }}
          </p>
          <h2 class="text-2xl font-semibold text-slate-950">{{ $t('transfers.employeeTransferTitle') }}</h2>
          <p class="max-w-3xl text-sm leading-6 text-slate-600">
            {{ $t('transfers.employeeTransferDescription') }}
          </p>
        </div>

        <form class="mt-6 grid gap-4 md:grid-cols-2" @submit.prevent="submitEmployeeTransfer">
          <label class="block space-y-2">
            <span class="text-sm font-medium text-slate-700">{{ $t('transfers.sourceAccountLabel') }}</span>
            <select
              v-model="employeeTransferForm.fromAccountId"
              class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
            >
              <option value="">{{ $t('transfers.chooseAccountOption') }}</option>
              <option v-for="account in employeeCheckingAccounts" :key="account.id" :value="String(account.id)">
                {{ employeeAccountOptionLabel(account) }}
              </option>
            </select>
          </label>

          <label class="block space-y-2">
            <span class="text-sm font-medium text-slate-700">{{ $t('transfers.destinationAccountLabel') }}</span>
            <select
              v-model="employeeTransferForm.toAccountId"
              class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
            >
              <option value="">{{ $t('transfers.chooseAccountOption') }}</option>
              <option v-for="account in employeeDestinationAccounts" :key="account.id" :value="String(account.id)">
                {{ employeeAccountOptionLabel(account) }}
              </option>
            </select>
          </label>

          <TextInput
            v-model="employeeTransferForm.amount"
            :label="$t('transfers.amountLabel')"
            type="number"
            step="0.01"
            min="0.01"
            required
            :trim="false"
          />
          <TextInput
            v-model="employeeTransferForm.description"
            :label="$t('transfers.descriptionFieldLabel')"
          />

          <div class="md:col-span-2">
            <p class="text-xs font-medium text-slate-500">
              {{ $t('transfers.channelLabel') }}: <span class="font-semibold text-slate-700">EMPLOYEE</span>
            </p>
            <Button class="mt-3" type="submit" variant="primary" :disabled="isSubmittingEmployeeTransfer">
              {{ isSubmittingEmployeeTransfer ? $t('common.loading') : $t('transfers.sendButton') }}
            </Button>
          </div>
        </form>
      </article>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '../components/ui/PageHeader.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import TextInput from '../components/ui/TextInput.vue'
import Button from '../components/ui/Button.vue'
import { useAuth } from '../composables/useAuth'
import * as accountService from '../services/accountService'
import * as customerService from '../services/customerService'
import { buildCustomerIbanLookupParams } from '../services/customerLookupParams'
import * as transactionService from '../services/transactionService'

const auth = useAuth()
const { t, locale } = useI18n()

const ownAccounts = ref([])
const employeeAccounts = ref([])
const isLoadingAccounts = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const lookupForm = reactive({
  firstName: '',
  lastName: '',
})
const lookupResults = ref([])
const lookupErrorMessage = ref('')
const isLookupLoading = ref(false)
const lookupPage = ref({
  page: 0,
  size: 5,
  totalElements: 0,
  totalPages: 0,
})

const ownTransferForm = reactive({
  fromAccountId: '',
  toAccountId: '',
  amount: '',
  description: '',
})

const externalTransferForm = reactive({
  fromAccountId: '',
  destinationIban: '',
  amount: '',
  description: '',
})

const employeeTransferForm = reactive({
  fromAccountId: '',
  toAccountId: '',
  amount: '',
  description: '',
})

const isSubmittingOwnTransfer = ref(false)
const isSubmittingExternalTransfer = ref(false)
const isSubmittingEmployeeTransfer = ref(false)

const isEmployee = computed(() => auth.isEmployee.value)

const ownCheckingAccounts = computed(() =>
  ownAccounts.value.filter((account) => account.accountType === 'CHECKING' && account.isActive),
)

const ownTransferSourceAccounts = computed(() => ownAccounts.value.filter((account) => account.isActive))

const ownTransferDestinationAccounts = computed(() =>
  ownAccounts.value.filter(
    (account) => account.isActive && String(account.id) !== String(ownTransferForm.fromAccountId || ''),
  ),
)

const employeeCheckingAccounts = computed(() =>
  employeeAccounts.value.filter((account) => account.accountType === 'CHECKING' && account.isActive),
)

const employeeSourceAccountId = computed(() => String(employeeTransferForm.fromAccountId || ''))

const employeeDestinationAccounts = computed(() =>
  employeeCheckingAccounts.value.filter((account) => String(account.id) !== employeeSourceAccountId.value),
)

const hasTransferOptions = computed(() =>
  isEmployee.value ? employeeCheckingAccounts.value.length > 0 : ownTransferSourceAccounts.value.length > 0,
)

const formatCurrency = (value, currency = 'EUR') =>
  new Intl.NumberFormat(locale.value, { style: 'currency', currency }).format(Number(value ?? 0))

const accountTypeLabel = (accountType) =>
  accountType === 'SAVINGS' ? t('accounts.savingsType') : t('accounts.checkingType')

const accountOptionLabel = (account) =>
  `${account.iban} · ${accountTypeLabel(account.accountType)} · ${formatCurrency(account.balance)}`

const employeeAccountOptionLabel = (account) =>
  `${account.iban} · ${accountTypeLabel(account.accountType)} · #${account.userId} · ${formatCurrency(account.balance)}`

const clearFeedback = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const clearLookupFeedback = () => {
  lookupErrorMessage.value = ''
}

const loadEmployeeAccounts = async () => {
  const response = await accountService.listAccounts(auth.accessToken.value)
  return response ?? []
}

const syncOwnTransferDefaults = () => {
  if (ownTransferSourceAccounts.value.length > 0 && !ownTransferForm.fromAccountId) {
    ownTransferForm.fromAccountId = String(ownTransferSourceAccounts.value[0].id)
  }

  if (
    ownTransferDestinationAccounts.value.length > 0 &&
    (!ownTransferForm.toAccountId ||
      ownTransferForm.toAccountId === ownTransferForm.fromAccountId)
  ) {
    ownTransferForm.toAccountId = String(ownTransferDestinationAccounts.value[0].id)
  }

  if (ownCheckingAccounts.value.length > 0 && !externalTransferForm.fromAccountId) {
    externalTransferForm.fromAccountId = String(ownCheckingAccounts.value[0].id)
  }
}

const syncEmployeeTransferDefaults = () => {
  if (employeeCheckingAccounts.value.length > 0 && !employeeTransferForm.fromAccountId) {
    employeeTransferForm.fromAccountId = String(employeeCheckingAccounts.value[0].id)
  }

  if (
    employeeDestinationAccounts.value.length > 0 &&
    (!employeeTransferForm.toAccountId || employeeTransferForm.toAccountId === employeeTransferForm.fromAccountId)
  ) {
    employeeTransferForm.toAccountId = String(employeeDestinationAccounts.value[0].id)
  }
}

watch(
  () => ownTransferForm.fromAccountId,
  () => {
    if (ownTransferForm.toAccountId === ownTransferForm.fromAccountId) {
      ownTransferForm.toAccountId = ownTransferDestinationAccounts.value[0]?.id
        ? String(ownTransferDestinationAccounts.value[0].id)
        : ''
    }
  },
)

watch(
  () => employeeTransferForm.fromAccountId,
  () => {
    if (employeeTransferForm.toAccountId === employeeTransferForm.fromAccountId) {
      employeeTransferForm.toAccountId = employeeDestinationAccounts.value[0]?.id
        ? String(employeeDestinationAccounts.value[0].id)
        : ''
    }
  },
)

const loadAccounts = async () => {
  isLoadingAccounts.value = true
  errorMessage.value = ''

  try {
    if (isEmployee.value) {
      employeeAccounts.value = await loadEmployeeAccounts()
      syncEmployeeTransferDefaults()
    } else {
      const response = await accountService.getOwnAccounts(auth.accessToken.value)
      ownAccounts.value = response?.accounts ?? []
      syncOwnTransferDefaults()
    }
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoadingAccounts.value = false
  }
}

const reloadBalances = async () => {
  if (isEmployee.value) {
    employeeAccounts.value = await loadEmployeeAccounts()
    syncEmployeeTransferDefaults()
    return
  }

  const response = await accountService.getOwnAccounts(auth.accessToken.value)
  ownAccounts.value = response?.accounts ?? []
  syncOwnTransferDefaults()
}

const transferPayload = (base, description) => ({
  ...base,
  amount: Number.parseFloat(base.amount),
  ...(description ? { description } : {}),
})

const submitOwnTransfer = async () => {
  clearFeedback()
  isSubmittingOwnTransfer.value = true

  try {
    await transactionService.createTransferTransaction(
      transferPayload(
        {
          fromAccountId: Number.parseInt(ownTransferForm.fromAccountId, 10),
          toAccountId: Number.parseInt(ownTransferForm.toAccountId, 10),
          amount: ownTransferForm.amount,
          channel: 'WEB',
        },
        ownTransferForm.description.trim(),
      ),
      auth.accessToken.value,
    )

    ownTransferForm.amount = ''
    ownTransferForm.description = ''
    successMessage.value = t('transfers.transferSuccess')
    await reloadBalances().catch((refreshError) => {
      errorMessage.value = refreshError.message
    })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSubmittingOwnTransfer.value = false
  }
}

const submitOwnExternalTransfer = async () => {
  clearFeedback()
  isSubmittingExternalTransfer.value = true

  try {
    await transactionService.createTransferTransaction(
      transferPayload(
        {
          fromAccountId: Number.parseInt(externalTransferForm.fromAccountId, 10),
          destinationIban: externalTransferForm.destinationIban.trim(),
          amount: externalTransferForm.amount,
          channel: 'WEB',
        },
        externalTransferForm.description.trim(),
      ),
      auth.accessToken.value,
    )

    externalTransferForm.amount = ''
    externalTransferForm.description = ''
    successMessage.value = t('transfers.transferSuccess')
    await reloadBalances().catch((refreshError) => {
      errorMessage.value = refreshError.message
    })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSubmittingExternalTransfer.value = false
  }
}

const submitEmployeeTransfer = async () => {
  clearFeedback()
  isSubmittingEmployeeTransfer.value = true

  try {
    await transactionService.createTransferTransaction(
      transferPayload(
        {
          fromAccountId: Number.parseInt(employeeTransferForm.fromAccountId, 10),
          toAccountId: Number.parseInt(employeeTransferForm.toAccountId, 10),
          amount: employeeTransferForm.amount,
          channel: 'EMPLOYEE',
        },
        employeeTransferForm.description.trim(),
      ),
      auth.accessToken.value,
    )

    employeeTransferForm.amount = ''
    employeeTransferForm.description = ''
    successMessage.value = t('transfers.transferSuccess')
    await reloadBalances().catch((refreshError) => {
      errorMessage.value = refreshError.message
    })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSubmittingEmployeeTransfer.value = false
  }
}

const searchCustomerIban = async () => {
  clearLookupFeedback()
  isLookupLoading.value = true

  try {
    const response = await customerService.lookupCustomerIban(
      buildCustomerIbanLookupParams({
        firstName: lookupForm.firstName,
        lastName: lookupForm.lastName,
        page: 0,
        size: lookupPage.value.size,
      }),
      auth.accessToken.value,
    )

    lookupResults.value = response?.items ?? []
    lookupPage.value = response?.page ?? lookupPage.value
    lookupPage.value.page = 0

    if (lookupResults.value.length === 0) {
      lookupErrorMessage.value = t('transfers.lookupEmptyMessage')
    }
  } catch (error) {
    lookupErrorMessage.value = error.message
  } finally {
    isLookupLoading.value = false
  }
}

const goToLookupPage = async (nextPage) => {
  clearLookupFeedback()
  isLookupLoading.value = true

  try {
    const response = await customerService.lookupCustomerIban(
      buildCustomerIbanLookupParams({
        firstName: lookupForm.firstName,
        lastName: lookupForm.lastName,
        page: nextPage,
        size: lookupPage.value.size,
      }),
      auth.accessToken.value,
    )

    lookupResults.value = response?.items ?? []
    lookupPage.value = response?.page ?? lookupPage.value
  } catch (error) {
    lookupErrorMessage.value = error.message
  } finally {
    isLookupLoading.value = false
  }
}

const selectLookupResult = (iban) => {
  externalTransferForm.destinationIban = iban
}

onMounted(loadAccounts)
</script>
