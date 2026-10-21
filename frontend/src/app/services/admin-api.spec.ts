import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { environment } from '../../environments/environment';
import { Task } from '../models/task';
import { UserProfile } from '../models/user-profile';
import { AdminApi } from './admin-api';

describe('AdminApi', () => {
  let service: AdminApi;
  let httpMock: HttpTestingController;

  const mockUser: UserProfile = {
    id: 1,
    username: 'admin_user',
    email: 'admin@test.com',
    roles: ['ROLE_ADMIN'],
  };

  const mockTask: Task = {
    id: 1,
    username: 'admin_user',
    taskDescription: 'Manage system tasks',
    dueDate: '2026-12-31',
    status: 'PENDING',
  };

  beforeEach(() => {
    TestBed.resetTestingModule();

    TestBed.configureTestingModule({
      providers: [AdminApi, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AdminApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Ensure no unmatched HTTP requests are pending
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all users (getAllUsers)', () => {
    const mockUsers: UserProfile[] = [mockUser];

    service.getAllUsers().subscribe((users) => {
      expect(users).toEqual(mockUsers);
      expect(users.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users`);
    expect(req.request.method).toBe('GET');
    req.flush(mockUsers);
  });

  it('should fetch tasks by username with query parameters (getTasksByUsername)', () => {
    const username = 'admin_user';
    const mockTasks: Task[] = [mockTask];

    service.getTasksByUsername(username).subscribe((tasks) => {
      expect(tasks).toEqual(mockTasks);
      expect(tasks[0].username).toBe(username);
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/admin/tasks?username=${encodeURIComponent(username)}`,
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockTasks);
  });

  it('should update user roles (updateUserRoles)', () => {
    const userId = 1;
    const newRoles = ['ROLE_ADMIN', 'ROLE_USER'];
    const updatedUser: UserProfile = { ...mockUser, roles: newRoles };

    service.updateUserRoles(userId, newRoles).subscribe((user) => {
      expect(user.roles).toEqual(newRoles);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users/${userId}/roles`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(newRoles);
    req.flush(updatedUser);
  });

  it('should delete a user (deleteUser)', () => {
    const userId = 1;

    service.deleteUser(userId).subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users/${userId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
