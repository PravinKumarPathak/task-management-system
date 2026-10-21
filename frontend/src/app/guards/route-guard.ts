import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../services/auth-store';

export const routeGuard: CanActivateFn = (_route, _state) => {
  const router = inject(Router);
  const authStore = inject(AuthStore);

  if (authStore.isUserLoggedIn()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
