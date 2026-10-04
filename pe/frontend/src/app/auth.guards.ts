import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserRole } from './models/auth';
import { AuthService } from './services/auth.service';

function roleGuard(role: UserRole): CanActivateFn {
  return async () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    try {
      await auth.restoreSession();
    } catch {
      return router.createUrlTree(['/login']);
    }
    if (auth.role() === role) {
      return true;
    }
    return router.createUrlTree(['/login']);
  };
}

export const studentGuard = roleGuard('STUDENT');
export const moderatorGuard = roleGuard('MODERATOR');

export const loginGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  try {
    await auth.restoreSession();
  } catch {
    return true;
  }
  if (auth.role() === 'STUDENT') {
    return router.createUrlTree(['/home']);
  }
  if (auth.role() === 'MODERATOR') {
    return router.createUrlTree(['/moderation']);
  }
  return true;
};
