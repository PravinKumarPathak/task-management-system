import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthStore } from '../services/auth-store';
import { routeGuard } from './route-guard';

describe('routeGuard', () => {
  let authStoreMock: any;
  let router: Router;

  const executeGuard = () => TestBed.runInInjectionContext(() => routeGuard({} as any, {} as any));

  beforeEach(() => {
    TestBed.resetTestingModule();

    // Create a mock implementation of AuthStore
    authStoreMock = {
      isUserLoggedIn: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStore, useValue: authStoreMock }],
    });

    router = TestBed.inject(Router);
  });

  it('should be created', () => {
    expect(routeGuard).toBeTruthy();
  });

  it('should allow access (true) if user is logged in', () => {
    authStoreMock.isUserLoggedIn.mockReturnValue(true);

    const result = executeGuard();

    expect(authStoreMock.isUserLoggedIn).toHaveBeenCalled();
    expect(result).toBe(true);
  });

  it('should redirect to /login if user is not logged in', () => {
    authStoreMock.isUserLoggedIn.mockReturnValue(false);
    const createUrlTreeSpy = vi.spyOn(router, 'createUrlTree');

    const result = executeGuard();

    expect(authStoreMock.isUserLoggedIn).toHaveBeenCalled();
    expect(createUrlTreeSpy).toHaveBeenCalledWith(['/login']);
    expect(result).toBeInstanceOf(UrlTree);
  });
});
