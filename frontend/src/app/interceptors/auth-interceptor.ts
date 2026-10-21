import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStore } from '../services/auth-store';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authStore = inject(AuthStore);
  const token = authStore.getAuthenticatedToken();

  const isAuthEndpoint =
    req.url.includes('/auth/authenticate') || req.url.includes('/auth/register');

  if (token && !isAuthEndpoint) {
    const formattedToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;

    req = req.clone({
      setHeaders: {
        Authorization: formattedToken,
      },
    });
  }

  return next(req);
};
