import Cookies from 'js-cookie';

const TOKEN_KEY = 'Admin-Token';
const EXPIRES_IN_KEY = 'Admin-Expires-In';

export const getToken = () => Cookies.get(TOKEN_KEY);
export const setToken = (token: string) => Cookies.set(TOKEN_KEY, token);
export const removeToken = () => Cookies.remove(TOKEN_KEY);
export const setExpiresIn = (expiresIn: number) => Cookies.set(EXPIRES_IN_KEY, String(expiresIn));
export const removeExpiresIn = () => Cookies.remove(EXPIRES_IN_KEY);

export const clearSession = () => {
  removeToken();
  removeExpiresIn();
};
