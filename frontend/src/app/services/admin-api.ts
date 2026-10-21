import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Task } from '../models/task';
import { UserProfile } from '../models/user-profile';

@Injectable({
  providedIn: 'root',
})
export class AdminApi {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin`;

  getAllUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.apiUrl}/users`);
  }

  getTasksByUsername(username: string): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.apiUrl}/tasks?username=${encodeURIComponent(username)}`);
  }

  updateUserRoles(userId: number, roles: string[]): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/users/${userId}/roles`, roles);
  }

  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${userId}`);
  }
}
