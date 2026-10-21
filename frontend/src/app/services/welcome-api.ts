import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserProfile } from '../models/user-profile';
import { AuthStore } from './auth-store';

@Injectable({
  providedIn: 'root',
})
export class WelcomeApi {
  private http = inject(HttpClient);
  private authStore = inject(AuthStore);
  private readonly baseUrl = environment.apiUrl;

  getUserProfile(): Observable<UserProfile> {
    return this.authStore.fetchUserProfile();
  }

  deleteOwnAccount(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/users/me`);
  }
}
