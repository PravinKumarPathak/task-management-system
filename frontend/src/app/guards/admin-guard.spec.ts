import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthStore } from '../services/auth-store';
import { adminGuard } from './admin-guard';

describe('adminGuard', () => {
  let authStoreMock: any;
  let router: Router;

  const executeGuard = () => TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

  beforeEach(() => {
    TestBed.resetTestingModule();

    // Create a mock implementation of AuthStore
    authStoreMock = {
      isUserLoggedIn: vi.fn(),
      currentUser: vi.fn(),
      isAdmin: vi.fn(),
      getAuthenticatedUser: vi.fn(),
      fetchUserProfile: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStore, useValue: authStoreMock }],
    });

    router = TestBed.inject(Router);
  });

  it('should be created', () => {
    expect(adminGuard).toBeTruthy();
  });

  it('should redirect to /login if user is not logged in', () => {
    authStoreMock.isUserLoggedIn.mockReturnValue(false);
    const createUrlTreeSpy = vi.spyOn(router, 'createUrlTree');

    const result = executeGuard();

    expect(authStoreMock.isUserLoggedIn).toHaveBeenCalled();
    expect(createUrlTreeSpy).toHaveBeenCalledWith(['/login']);
    expect(result).toBeInstanceOf(UrlTree);
  });

  it('should allow access (true) if user is logged in, profile exists, and user is an admin', () => {
    authStoreMock.isUserLoggedIn.mockReturnValue(true);
    authStoreMock.currentUser.mockReturnValue({ id: 1, username: 'admin' });
    authStoreMock.isAdmin.mockReturnValue(true);

    const result = executeGuard();

    expect(result).toBe(true);
  });

  it('should redirect to /welcome/:username if user is logged in, profile exists, but user is not an admin', () => {
    authStoreMock.isUserLoggedIn.mockReturnValue(true);
    authStoreMock.currentUser.mockReturnValue({ id: 2, username: 'regularuser' });
    authStoreMock.isAdmin.mockReturnValue(false);
    authStoreMock.getAuthenticatedUser.mockReturnValue('regularuser');
    const createUrlTreeSpy = vi.spyOn(router, 'createUrlTree');

    const result = executeGuard();

    expect(createUrlTreeSpy).toHaveBeenCalledWith(['/welcome', 'regularuser']);
    expect(result).toBeInstanceOf(UrlTree);
  });

  it('should fetch user profile and allow access if admin when current user signal is null', () => {
    authStoreMock.isUserLoggedIn.mockReturnValue(true);
    authStoreMock.currentUser.mockReturnValue(null);
    authStoreMock.fetchUserProfile.mockReturnValue(
      of({ id: 1, username: 'admin', email: 'admin@test.com', roles: ['ROLE_ADMIN'] }),
    );
    authStoreMock.isAdmin.mockReturnValue(true);

    const result = executeGuard() as any;

    result.subscribe((res: any) => {
      expect(res).toBe(true);
    });
  });

  it('should fetch user profile and redirect to welcome if not admin when current user signal is null', () => {
    authStoreMock.isUserLoggedIn.mockReturnValue(true);
    authStoreMock.currentUser.mockReturnValue(null);
    const mockProfile = {
      id: 2,
      username: 'regularuser',
      email: 'user@test.com',
      roles: ['ROLE_USER'],
    };
    authStoreMock.fetchUserProfile.mockReturnValue(of(mockProfile));
    authStoreMock.isAdmin.mockReturnValue(false);
    const createUrlTreeSpy = vi.spyOn(router, 'createUrlTree');

    const result = executeGuard() as any;

    result.subscribe((res: any) => {
      expect(createUrlTreeSpy).toHaveBeenCalledWith(['/welcome', 'regularuser']);
      expect(res).toBeInstanceOf(UrlTree);
    });
  });
});
