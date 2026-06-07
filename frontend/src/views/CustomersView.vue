<template>
  <section class="mx-auto max-w-6xl space-y-6">
    <PageHeader
      :label="$t('customers.label')"
      :title="$t('customers.title')"
      :description="$t('customers.description')"
    />

    <CustomerTabs />

    <FeedbackBanner :message="errorMessage" tone="error" />

    <form
      class="grid gap-4 rounded-[2rem] border border-slate-200 bg-white p-5 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)] md:grid-cols-[1fr_1fr_1fr_1fr_auto]"
      @submit.prevent="applyFilters"
    >
      <TextInput v-model="filters.firstName" :label="$t('customers.firstNameLabel')" />
      <TextInput v-model="filters.lastName" :label="$t('customers.lastNameLabel')" />
      <TextInput v-model="filters.email" :label="$t('customers.emailLabel')" type="email" />
      <label class="block space-y-2">
        <span class="text-sm font-medium text-slate-700">{{ $t('customers.approvalLabel') }}</span>
        <select
          v-model="filters.approved"
          class="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500"
        >
          <option value="">{{ $t('customers.allStatuses') }}</option>
          <option value="true">{{ $t('home.statusApproved') }}</option>
          <option value="false">{{ $t('home.statusPending') }}</option>
        </select>
      </label>
      <div class="flex items-end gap-2">
        <Button type="submit" variant="primary" :disabled="isLoading">
          {{ $t('customers.filterButton') }}
        </Button>
        <Button type="button" variant="secondary" :disabled="isLoading" @click="resetFilters">
          {{ $t('common.reset') }}
        </Button>
      </div>
    </form>

    <div
      v-if="isLoading"
      class="rounded-[2rem] border border-slate-200 bg-white/90 p-8 text-sm font-medium text-slate-600 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]"
    >
      {{ $t('common.loading') }}
    </div>

    <div
      v-else-if="customers.length === 0"
      class="rounded-[2rem] border border-dashed border-slate-300 bg-white/80 p-8 text-center"
    >
      <p class="text-sm font-semibold uppercase tracking-[0.28em] text-emerald-600">
        {{ $t('customers.emptyLabel') }}
      </p>
      <h2 class="mt-3 text-2xl font-semibold text-slate-950">
        {{ $t('customers.emptyTitle') }}
      </h2>
    </div>

    <div v-else class="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-[0_20px_60px_-40px_rgba(15,23,42,0.28)]">
      <div class="hidden grid-cols-[1.4fr_1.7fr_0.8fr] gap-4 border-b border-slate-100 bg-slate-50 px-6 py-3 text-xs font-semibold uppercase tracking-[0.2em] text-slate-500 md:grid">
        <span>{{ $t('customers.customerColumn') }}</span>
        <span>{{ $t('customers.emailLabel') }}</span>
        <span>{{ $t('customers.statusColumn') }}</span>
      </div>
      <article
        v-for="customer in customers"
        :key="customer.id"
        class="grid gap-3 border-b border-slate-100 px-6 py-5 last:border-b-0 md:grid-cols-[1.4fr_1.7fr_0.8fr] md:items-center"
      >
        <div>
          <p class="text-base font-semibold text-slate-950">
            {{ customer.firstName }} {{ customer.lastName }}
          </p>
          <p class="mt-1 text-xs font-medium text-slate-500">#{{ customer.id }}</p>
        </div>
        <p class="break-all text-sm font-medium text-slate-700">{{ customer.email }}</p>
        <span
          class="w-fit rounded-full px-3 py-1 text-xs font-semibold"
          :class="customer.approved
            ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-100'
            : 'bg-amber-50 text-amber-800 ring-1 ring-amber-100'"
        >
          {{ customer.approved ? $t('home.statusApproved') : $t('home.statusPending') }}
        </span>
      </article>
    </div>

    <div v-if="page.totalPages > 1" class="flex items-center justify-between rounded-full border border-slate-200 bg-white px-4 py-3">
      <Button variant="secondary" :disabled="isLoading || page.page === 0" @click="goToPage(page.page - 1)">
        {{ $t('customers.previousPage') }}
      </Button>
      <span class="text-sm font-semibold text-slate-600">
        {{ $t('customers.pageLabel', { current: page.page + 1, total: page.totalPages }) }}
      </span>
      <Button variant="secondary" :disabled="isLoading || page.page >= page.totalPages - 1" @click="goToPage(page.page + 1)">
        {{ $t('customers.nextPage') }}
      </Button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import CustomerTabs from '../components/customers/CustomerTabs.vue'
import PageHeader from '../components/ui/PageHeader.vue'
import TextInput from '../components/ui/TextInput.vue'
import Button from '../components/ui/Button.vue'
import FeedbackBanner from '../components/ui/FeedbackBanner.vue'
import { useAuth } from '../composables/useAuth'
import * as customerService from '../services/customerService'

const auth = useAuth()
const filters = reactive({
  firstName: '',
  lastName: '',
  email: '',
  approved: '',
})
const customers = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const page = ref({
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
})

// Combines current filters with backend pagination parameters.
const requestParams = () => ({
  firstName: filters.firstName,
  lastName: filters.lastName,
  email: filters.email,
  approved: filters.approved,
  page: page.value.page,
  size: page.value.size,
  sort: 'createdAt,desc',
})

// Loads customers for the employee overview.
const loadCustomers = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await customerService.getCustomers(requestParams(), auth.accessToken.value)
    customers.value = response?.items ?? []
    page.value = response?.page ?? page.value
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

// Applies filters from the first page.
const applyFilters = async () => {
  page.value.page = 0
  await loadCustomers()
}

// Clears all filters before reloading the overview.
const resetFilters = async () => {
  filters.firstName = ''
  filters.lastName = ''
  filters.email = ''
  filters.approved = ''
  await applyFilters()
}

// Moves to another backend page.
const goToPage = async (nextPage) => {
  page.value.page = nextPage
  await loadCustomers()
}

onMounted(loadCustomers)
</script>
