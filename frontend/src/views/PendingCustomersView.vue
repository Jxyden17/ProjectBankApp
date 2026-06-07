<template>
  <section class="mx-auto max-w-6xl space-y-6">
    <PageHeader
      :label="$t('pendingCustomers.label')"
      :title="$t('pendingCustomers.title')"
      :description="$t('pendingCustomers.description')"
    />

    <CustomerTabs />

    <FeedbackBanner :message="successMessage" tone="success" />
    <FeedbackBanner :message="errorMessage" tone="error" />

    <div
      v-if="isLoading"
      class="rounded-[2rem] border border-slate-200 bg-white/90 p-8 text-sm font-medium text-slate-600 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
    >
      {{ $t('common.loading') }}
    </div>

    <div
      v-else-if="pendingCustomers.length === 0"
      class="rounded-[2rem] border border-dashed border-slate-300 bg-white/80 p-8 text-center"
    >
      <p class="text-sm font-semibold uppercase tracking-[0.28em] text-emerald-600">
        {{ $t('pendingCustomers.emptyLabel') }}
      </p>
      <h2 class="mt-3 text-2xl font-semibold text-slate-950">
        {{ $t('pendingCustomers.emptyTitle') }}
      </h2>
    </div>

    <div v-else class="space-y-5">
      <article
        v-for="customer in pendingCustomers"
        :key="customer.id"
        class="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
      >
        <div class="grid gap-6 border-b border-slate-100 p-6 lg:grid-cols-[1.2fr_2fr]">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">
              {{ $t('pendingCustomers.customerLabel') }}
            </p>
            <h2 class="mt-3 text-2xl font-semibold text-slate-950">
              {{ customer.firstName }} {{ customer.lastName }}
            </h2>
            <dl class="mt-5 space-y-3 text-sm">
              <div class="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
                <dt class="font-medium text-slate-500">{{ $t('pendingCustomers.emailLabel') }}</dt>
                <dd class="font-semibold text-slate-900">{{ customer.email }}</dd>
              </div>
              <div class="flex items-center justify-between gap-4 rounded-2xl bg-amber-50 px-4 py-3">
                <dt class="font-medium text-amber-700">{{ $t('pendingCustomers.statusLabel') }}</dt>
                <dd class="font-semibold text-amber-800">{{ $t('home.statusPending') }}</dd>
              </div>
            </dl>
          </div>

          <form class="grid gap-4 md:grid-cols-2" @submit.prevent="approve(customer)">
            <TextInput
              v-model="approvalForms[customer.id].checkingAbsoluteTransferLimit"
              :label="$t('pendingCustomers.checkingAbsoluteLabel')"
              type="number"
              step="0.01"
              required
              :trim="false"
            />
            <TextInput
              v-model="approvalForms[customer.id].checkingDailyTransferLimit"
              :label="$t('pendingCustomers.checkingDailyLabel')"
              type="number"
              min="0"
              step="0.01"
              required
              :trim="false"
            />
            <TextInput
              v-model="approvalForms[customer.id].savingsAbsoluteTransferLimit"
              :label="$t('pendingCustomers.savingsAbsoluteLabel')"
              type="number"
              min="0"
              step="0.01"
              required
              :trim="false"
            />
            <TextInput
              v-model="approvalForms[customer.id].savingsDailyTransferLimit"
              :label="$t('pendingCustomers.savingsDailyLabel')"
              type="number"
              min="0"
              step="0.01"
              required
              :trim="false"
            />

            <div class="flex items-end md:col-span-2">
              <Button
                type="submit"
                variant="success"
                class="w-full md:w-auto"
                :disabled="approvingCustomerId === customer.id"
              >
                {{
                  approvingCustomerId === customer.id
                    ? $t('common.creating')
                    : $t('pendingCustomers.approveButton')
                }}
              </Button>
            </div>
          </form>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import CustomerTabs from '../components/customers/CustomerTabs.vue'
import PageHeader from '../components/ui/PageHeader.vue'
import TextInput from '../components/ui/TextInput.vue'
import Button from '../components/ui/Button.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import { useAuth } from '../composables/useAuth'
import * as customerService from '../services/customerService'

const defaultApprovalLimits = {
  checkingAbsoluteTransferLimit: '-500.00',
  checkingDailyTransferLimit: '1000.00',
  savingsAbsoluteTransferLimit: '0.00',
  savingsDailyTransferLimit: '5000.00',
}

const auth = useAuth()
const { t } = useI18n()
const pendingCustomers = ref([])
const approvalForms = reactive({})
const isLoading = ref(false)
const approvingCustomerId = ref(null)
const errorMessage = ref('')
const successMessage = ref('')

// Prepares the approval form defaults for one customer.
const initializeApprovalForm = (customerId) => {
  approvalForms[customerId] = { ...defaultApprovalLimits }
}

// Creates approval forms for every pending customer returned by the API.
const initializeApprovalForms = (customers) => {
  customers.forEach((customer) => initializeApprovalForm(customer.id))
}

// Loads the current pending approval queue.
const loadPendingCustomers = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await customerService.getPendingCustomers(auth.accessToken.value)
    pendingCustomers.value = response?.items ?? []
    initializeApprovalForms(pendingCustomers.value)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

// Converts decimal input strings into JSON numbers.
const toAmount = (value) => Number.parseFloat(value)

// Clears success and error messages before a new approval attempt.
const clearFeedback = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

// Builds the approval payload expected by the backend.
const approvalPayloadFor = (customerId) => {
  const form = approvalForms[customerId]

  return {
    approved: true,
    checkingAbsoluteTransferLimit: toAmount(form.checkingAbsoluteTransferLimit),
    checkingDailyTransferLimit: toAmount(form.checkingDailyTransferLimit),
    savingsAbsoluteTransferLimit: toAmount(form.savingsAbsoluteTransferLimit),
    savingsDailyTransferLimit: toAmount(form.savingsDailyTransferLimit),
  }
}

// Removes an approved customer from the local queue.
const removeApprovedCustomer = (customer) => {
  pendingCustomers.value = pendingCustomers.value.filter((item) => item.id !== customer.id)
  delete approvalForms[customer.id]
}

// Shows a localized success message after approval.
const showApprovalSuccess = (customer) => {
  successMessage.value = t('pendingCustomers.approvalSuccess', {
    name: `${customer.firstName} ${customer.lastName}`,
  })
}

// Approves the customer and updates the local queue.
const approve = async (customer) => {
  approvingCustomerId.value = customer.id
  clearFeedback()

  try {
    await customerService.approveCustomer(
      customer.id,
      approvalPayloadFor(customer.id),
      auth.accessToken.value,
    )

    removeApprovedCustomer(customer)
    showApprovalSuccess(customer)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    approvingCustomerId.value = null
  }
}

onMounted(loadPendingCustomers)
</script>
