import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { environment } from '../../environments/environment';
import { UserProfile } from '../models/user-profile';
import { AuthStore } from './auth-store';
import { WelcomeApi } from './welcome-api';

describe('WelcomeApi', () => {
  let service: WelcomeApi;
  let httpMock: HttpTestingController;
  let authStoreMock: any;

  const mockProfile: UserProfile = {
    id: 1,
    username: 'test_user',
    email: 'test@test.com',
    roles: ['ROLE_USER'],
  };

  beforeEach(() => {
    TestBed.resetTestingModule();

    authStoreMock = {
      fetchUserProfile: vi.fn().mockReturnValue(of(mockProfile)),
    };

    TestBed.configureTestingModule({
      providers: [
        WelcomeApi,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthStore, useValue: authStoreMock },
      ],
    });

    service = TestBed.inject(WelcomeApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Ensure no unmatched HTTP requests are pending
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch user profile by calling authStore.fetchUserProfile', () => {
    service.getUserProfile().subscribe((profile) => {
      expect(profile).toEqual(mockProfile);
    });

    expect(authStoreMock.fetchUserProfile).toHaveBeenCalled();
  });

  it('should delete own account via HTTP delete', () => {
    service.deleteOwnAccount().subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/me`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
