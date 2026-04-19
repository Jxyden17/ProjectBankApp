import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { LOCALE_STORAGE_KEY, SUPPORTED_LOCALES } from '../i18n'

export const useLocale = () => {
  const { locale, t } = useI18n()

  const current = computed(() => locale.value)

  const available = computed(() =>
    SUPPORTED_LOCALES.map((code) => ({
      code,
      label: t(`languages.${code}`),
    })),
  )

  const setLocale = (next) => {
    if (!SUPPORTED_LOCALES.includes(next)) return

    locale.value = next
    document.documentElement.setAttribute('lang', next)

    if (typeof window !== 'undefined') {
      window.localStorage.setItem(LOCALE_STORAGE_KEY, next)
    }
  }

  return {
    current,
    available,
    setLocale,
  }
}
