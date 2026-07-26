export default function access(initialState: App.InitialState | undefined) {
  const permissions = initialState?.permissions || [];
  const roles = initialState?.roles || [];

  return {
    isAuthenticated: Boolean(initialState?.currentUser),
    hasPermission: (permission: string) =>
      permissions.includes('*:*:*') || permissions.includes(permission),
    hasRole: (role: string) => roles.includes('admin') || roles.includes(role)
  };
}
