import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthStore } from '../services/auth-store';
import { authInterceptor } from './auth-interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authStoreMock: any;

  beforeEach(() => {
    TestBed.resetTestingModule();

    authStoreMock = {
      getAuthenticatedToken: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthStore, useValue: authStoreMock },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Ensure no unmatched HTTP requests are pending
    httpMock.verify();
  });

  it('should be created', () => {
    expect(authInterceptor).toBeTruthy();
  });

  it('should add Authorization header if token exists and request is not an auth endpoint', () => {
    authStoreMock.getAuthenticatedToken.mockReturnValue('Bearer test-token-123');

    httpClient.get('/api/tasks').subscribe();

    const req = httpMock.expectOne('/api/tasks');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token-123');
    req.flush({});
  });

  it('should prepend Bearer prefix if token lacks it', () => {
    authStoreMock.getAuthenticatedToken.mockReturnValue('raw-token-abc');

    httpClient.get('/api/tasks').subscribe();

    const req = httpMock.expectOne('/api/tasks');
    expect(req.request.headers.get('Authorization')).toBe('Bearer raw-token-abc');
    req.flush({});
  });

  it('should not add Authorization header if no token exists', () => {
    authStoreMock.getAuthenticatedToken.mockReturnValue(null);

    httpClient.get('/api/tasks').subscribe();

    const req = httpMock.expectOne('/api/tasks');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should not add Authorization header for /auth/authenticate endpoint even if token exists', () => {
    authStoreMock.getAuthenticatedToken.mockReturnValue('Bearer test-token-123');

    httpClient.post('/api/auth/authenticate', {}).subscribe();

    const req = httpMock.expectOne('/api/auth/authenticate');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should not add Authorization header for /auth/register endpoint even if token exists', () => {
    authStoreMock.getAuthenticatedToken.mockReturnValue('Bearer test-token-123');

    httpClient.post('/api/auth/register', {}).subscribe();

    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
