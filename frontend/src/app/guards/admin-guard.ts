import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthStore } from '../services/auth-store';

export const adminGuard: CanActivateFn = (_route, _state) => {
  const router = inject(Router);
  const authStore = inject(AuthStore);

  if (!authStore.isUserLoggedIn()) {
    return router.createUrlTree(['/login']);
  }

  // 1. If signal already has profile loaded, decide synchronously
  if (authStore.currentUser() !== null) {
    if (authStore.isAdmin()) {
      return true;
    }
    const username = authStore.getAuthenticatedUser();
    return router.createUrlTree(['/welcome', username]);
  }

  // 2. Race Condition Fix: If signal is empty, fetch profile BEFORE evaluating route
  return authStore.fetchUserProfile().pipe(
    map((profile) => {
      if (authStore.isAdmin()) {
        return true;
      }
      return router.createUrlTree(['/welcome', profile.username]);
    }),
  );
};
