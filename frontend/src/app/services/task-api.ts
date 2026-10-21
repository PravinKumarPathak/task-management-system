import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Task } from '../models/task';

@Injectable({
  providedIn: 'root',
})
export class TaskApi {
  private http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getAllTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/tasks`);
  }

  getUserTasks(username: string): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/users/${encodeURIComponent(username)}/tasks`);
  }

  getTask(username: string, id: number): Observable<Task> {
    return this.http.get<Task>(`${this.baseUrl}/users/${encodeURIComponent(username)}/tasks/${id}`);
  }

  deleteTask(username: string, id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/users/${encodeURIComponent(username)}/tasks/${id}`,
    );
  }

  updateTask(username: string, id: number, task: Task): Observable<Task> {
    return this.http.put<Task>(
      `${this.baseUrl}/users/${encodeURIComponent(username)}/tasks/${id}`,
      task,
    );
  }

  createTask(username: string, task: Task): Observable<Task> {
    return this.http.post<Task>(
      `${this.baseUrl}/users/${encodeURIComponent(username)}/tasks`,
      task,
    );
  }
}
