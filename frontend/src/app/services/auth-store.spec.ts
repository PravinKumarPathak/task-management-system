import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { environment } from '../../environments/environment';
import { AuthResponse } from '../models/auth-response';
import { AuthStore } from './auth-store';

describe('AuthStore', () => {
  let store: AuthStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.resetTestingModule();
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [AuthStore, provideHttpClient(), provideHttpClientTesting()],
    });

    store = TestBed.inject(AuthStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Ensure no unmatched HTTP requests are pending
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });

  it('should authenticate user successfully and store tokens and roles', () => {
    const mockResponse: AuthResponse = {
      token: 'sample-jwt-token',
      roles: ['ROLE_ADMIN'],
    };

    store.executeJWTAuthenticationService('testuser', 'password123').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/authenticate`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    expect(store.isAuthenticated()).toBe(true);
    expect(store.isAdmin()).toBe(true);
    expect(store.getAuthenticatedUser()).toBe('testuser');
    expect(sessionStorage.getItem('token')).toBe('Bearer sample-jwt-token');
  });

  it('should correctly identify non-admin users', () => {
    const mockResponse: AuthResponse = {
      token: 'sample-jwt-token',
      roles: ['ROLE_USER'],
    };

    store.executeJWTAuthenticationService('regularuser', 'password123').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/authenticate`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    expect(store.isAuthenticated()).toBe(true);
    expect(store.isAdmin()).toBe(false);
  });

  it('should clear session and reset signals on logout', () => {
    // Set some initial session items
    sessionStorage.setItem('token', 'Bearer token');
    sessionStorage.setItem('authenticateUser', 'testuser');
    sessionStorage.setItem('userRoles', JSON.stringify(['ROLE_ADMIN']));

    store.logout();

    expect(store.isAuthenticated()).toBe(false);
    expect(store.isAdmin()).toBe(false);
    expect(store.getAuthenticatedUser()).toBeNull();
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(sessionStorage.getItem('authenticateUser')).toBeNull();
  });
});
