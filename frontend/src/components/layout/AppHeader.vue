<template>
  <header class="sticky top-0 z-50 border-b border-white/60 bg-white/75 backdrop-blur-xl">
    <nav class="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
      <RouterLink to="/" class="flex items-center gap-3">
        <span class="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-sm font-semibold tracking-[0.2em] text-white">
          DBB
        </span>
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.35em] text-emerald-600">{{ $t('brand.name') }}</p>
          <p class="text-sm font-medium text-slate-600">{{ $t('brand.tagline') }}</p>
        </div>
      </RouterLink>

      <div
        v-if="auth.isAuthenticated.value"
        class="hidden items-center gap-2 rounded-full border border-slate-200 bg-white/80 p-1 md:flex"
      >
        <RouterLink
          v-for="link in visibleLinks"
          :key="link.to"
          :to="link.to"
          class="rounded-full px-4 py-2 text-sm font-medium transition"
          :class="isActive(link.to)
            ? 'bg-slate-950 text-white shadow-sm'
            : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'"
        >
          {{ $t(link.labelKey) }}
        </RouterLink>
      </div>

      <div class="flex items-center gap-3">
        <LanguageSwitcher class="hidden sm:flex" />
        <template v-if="auth.isAuthenticated.value">
          <span class="hidden text-sm font-medium text-slate-600 sm:inline">
            {{ $t('nav.welcome', { name: auth.currentUser.value?.firstName ?? '' }) }}
          </span>
          <Button variant="secondary" :disabled="isLoggingOut" @click="signOut">
            {{ $t('nav.logout') }}
          </Button>
        </template>
        <template v-else>
          <RouterLink to="/register">
            <Button variant="secondary">{{ $t('nav.register') }}</Button>
          </RouterLink>
          <RouterLink to="/login">
            <Button variant="primary">{{ $t('nav.signIn') }}</Button>
          </RouterLink>
        </template>
      </div>
    </nav>

    <div class="border-t border-slate-200/80 px-4 py-3 md:hidden">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <div v-if="auth.isAuthenticated.value" class="flex flex-wrap gap-2">
          <RouterLink
            v-for="link in visibleLinks"
            :key="link.to"
            :to="link.to"
            class="rounded-full px-3 py-2 text-sm font-medium transition"
            :class="isActive(link.to)
              ? 'bg-slate-950 text-white'
              : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:text-slate-950'"
          >
            {{ $t(link.labelKey) }}
          </RouterLink>
        </div>
        <div class="flex items-center gap-2">
          <span v-if="auth.isAuthenticated.value" class="text-sm font-medium text-slate-600">
            {{ auth.currentUser.value?.firstName }}
          </span>
          <Button
            v-if="auth.isAuthenticated.value"
            variant="secondary"
            :disabled="isLoggingOut"
            @click="signOut"
          >
            {{ $t('nav.logout') }}
          </Button>
          <template v-else>
            <RouterLink to="/register">
              <Button variant="secondary">{{ $t('nav.register') }}</Button>
            </RouterLink>
            <RouterLink to="/login">
              <Button variant="primary">{{ $t('nav.signIn') }}</Button>
            </RouterLink>
          </template>
          <LanguageSwitcher class="sm:hidden" />
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from '../ui/Button.vue'
import LanguageSwitcher from '../ui/LanguageSwitcher.vue'
import { useAuth } from '../../composables/useAuth'

const props = defineProps({
  links: {
    type: Array,
    default: () => [
      { labelKey: 'nav.home', to: '/' },
      { labelKey: 'nav.accounts', to: '/accounts' },
      { labelKey: 'nav.transfers', to: '/transfers' },
    ],
  },
})

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const isLoggingOut = ref(false)
const visibleLinks = computed(() =>
  auth.isRestrictedCustomer.value
    ? props.links.filter((link) => link.to === '/')
    : props.links,
)

const isActive = (path) => route.path === path

const signOut = async () => {
  isLoggingOut.value = true

  try {
    await auth.logout()
    await router.push('/login')
  } finally {
    isLoggingOut.value = false
  }
}
</script>
