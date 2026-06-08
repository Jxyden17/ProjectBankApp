export const DEFAULT_NAV_LINKS = [
  { labelKey: 'nav.home', to: '/' },
  { labelKey: 'nav.accounts', to: '/accounts/me', customerOnly: true },
  { labelKey: 'nav.transfers', to: '/transfers' },
  { labelKey: 'nav.customers', to: '/customers', employeeOnly: true },
  { labelKey: 'nav.accountsDirectory', to: '/accounts', employeeOnly: true },
]

export const visibleNavLinks = (links, { isEmployee, isRestrictedCustomer }) =>
  links.filter((link) => {
    if (isRestrictedCustomer) {
      return link.to === '/'
    }

    if (link.employeeOnly && !isEmployee) {
      return false
    }

    if (link.customerOnly && isEmployee) {
      return false
    }

    return true
  })
