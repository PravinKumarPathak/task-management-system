import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, Observable, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from '../models/auth-response';
import { AuthUser, UserProfile } from '../models/user-profile';

const TOKEN = 'token';
const AUTHENTICATED_USER = 'authenticateUser';
const USER_ROLES = 'userRoles';

interface JwtPayload {
  roles?: unknown[];
  authorities?: unknown[];
}

@Injectable({
  providedIn: 'root',
})
export class AuthStore {
  private http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  private profileRequest$: Observable<UserProfile> | null = null;

  authToken = signal<string | null>(sessionStorage.getItem(TOKEN));
  currentUser = signal<AuthUser | null>(this.getInitialUserFromStorage());

  isAuthenticated = computed(() => !!this.authToken() && !!this.currentUser());

  isAdmin = computed(() => {
    const user = this.currentUser();
    if (!user?.roles) return false;
    return user.roles.some((role) => ['ROLE_ADMIN', 'ADMIN'].includes(role.toUpperCase()));
  });

  executeJWTAuthenticationService(username: string, password: string) {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/authenticate`, { username, password })
      .pipe(
        tap((response) => {
          const tokenStr = response.token?.startsWith('Bearer ')
            ? response.token
            : `Bearer ${response.token}`;

          sessionStorage.setItem(AUTHENTICATED_USER, username);
          sessionStorage.setItem(TOKEN, tokenStr);
          this.authToken.set(tokenStr);

          let roles: string[] =
            response.roles && response.roles.length > 0
              ? response.roles
              : this.decodeTokenRoles(tokenStr);

          if (roles.length === 0) {
            roles = this.getInitialUserFromStorage()?.roles || [];
          }

          sessionStorage.setItem(USER_ROLES, JSON.stringify(roles));
          this.currentUser.set({
            username,
            roles,
          });
        }),
      );
  }

  fetchUserProfile(): Observable<UserProfile> {
    if (this.profileRequest$) {
      return this.profileRequest$;
    }

    this.profileRequest$ = this.http.get<UserProfile>(`${this.baseUrl}/users/me`).pipe(
      tap((profile) => {
        if (profile.roles?.length) {
          sessionStorage.setItem(USER_ROLES, JSON.stringify(profile.roles));
        }
        this.currentUser.set(profile);
      }),
      shareReplay(1),
      catchError((err) => {
        this.profileRequest$ = null;
        return throwError(() => err);
      }),
    );

    return this.profileRequest$;
  }

  registerUser(username: string, email: string, password: string) {
    return this.http.post(`${this.baseUrl}/auth/register`, {
      username,
      email,
      password,
    });
  }

  getAuthenticatedUser(): string | null {
    return this.currentUser()?.username || sessionStorage.getItem(AUTHENTICATED_USER);
  }

  getAuthenticatedToken(): string | null {
    return this.authToken() || sessionStorage.getItem(TOKEN);
  }

  isUserLoggedIn(): boolean {
    return this.isAuthenticated();
  }

  logout() {
    sessionStorage.removeItem(TOKEN);
    sessionStorage.removeItem(AUTHENTICATED_USER);
    sessionStorage.removeItem(USER_ROLES);

    this.authToken.set(null);
    this.currentUser.set(null);
    this.profileRequest$ = null;
  }

  private getInitialUserFromStorage(): AuthUser | null {
    const username = sessionStorage.getItem(AUTHENTICATED_USER);
    const rawRoles = sessionStorage.getItem(USER_ROLES);
    const token = sessionStorage.getItem(TOKEN);

    if (!username) return null;

    let roles: string[] = [];
    if (rawRoles) {
      try {
        roles = JSON.parse(rawRoles);
      } catch {
        sessionStorage.removeItem(USER_ROLES);
      }
    }

    if (roles.length === 0 && token) {
      roles = this.decodeTokenRoles(token);
    }

    return { username, roles };
  }

  private decodeTokenRoles(token: string): string[] {
    try {
      const rawToken = token.startsWith('Bearer ') ? token.split(' ')[1] : token;
      let base64Url = rawToken.split('.')[1];

      while (base64Url.length % 4 !== 0) {
        base64Url += '=';
      }

      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join(''),
      );

      const parsed = JSON.parse(jsonPayload) as JwtPayload;
      const rawRoles = parsed.roles || parsed.authorities || [];

      return rawRoles
        .map((r: unknown) =>
          typeof r === 'string' ? r : (r as { authority?: string })?.authority || '',
        )
        .filter((r: string) => r.length > 0);
    } catch {
      return [];
    }
  }
}
