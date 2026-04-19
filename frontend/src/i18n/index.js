import { createI18n } from 'vue-i18n'
import enGB from './locales/en-GB.json'
import nlNL from './locales/nl-NL.json'

export const SUPPORTED_LOCALES = ['en-GB', 'nl-NL']
export const DEFAULT_LOCALE = 'en-GB'
export const LOCALE_STORAGE_KEY = 'app.locale'

const resolveInitialLocale = () => {
  if (typeof window === 'undefined') return DEFAULT_LOCALE

  const stored = window.localStorage.getItem(LOCALE_STORAGE_KEY)
  if (stored && SUPPORTED_LOCALES.includes(stored)) return stored

  const browser = window.navigator.language
  if (SUPPORTED_LOCALES.includes(browser)) return browser

  const short = browser?.split('-')[0]
  const match = SUPPORTED_LOCALES.find((l) => l.startsWith(short))
  return match ?? DEFAULT_LOCALE
}

const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: {
    'en-GB': enGB,
    'nl-NL': nlNL,
  },
})

export default i18n
