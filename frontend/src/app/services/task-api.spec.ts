import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { environment } from '../../environments/environment';
import { Task } from '../models/task';
import { TaskApi } from './task-api';

describe('TaskApi', () => {
  let service: TaskApi;
  let httpMock: HttpTestingController;

  const mockTask: Task = {
    id: 1,
    username: 'john_doe',
    taskDescription: 'A task for unit testing',
    dueDate: '2026-12-31',
    status: 'PENDING',
  };

  beforeEach(() => {
    TestBed.resetTestingModule();

    TestBed.configureTestingModule({
      providers: [TaskApi, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(TaskApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Ensure no unmatched HTTP requests are pending
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all tasks (getAllTasks)', () => {
    const mockTasks: Task[] = [mockTask];

    service.getAllTasks().subscribe((tasks) => {
      expect(tasks).toEqual(mockTasks);
      expect(tasks.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/tasks`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTasks);
  });

  it('should fetch tasks for a specific user (getUserTasks)', () => {
    const username = 'john_doe';
    const mockTasks: Task[] = [mockTask];

    service.getUserTasks(username).subscribe((tasks) => {
      expect(tasks).toEqual(mockTasks);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/${username}/tasks`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTasks);
  });

  it('should fetch a single task by id (getTask)', () => {
    const username = 'john_doe';
    const taskId = 1;

    service.getTask(username, taskId).subscribe((task) => {
      expect(task).toEqual(mockTask);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/${username}/tasks/${taskId}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTask);
  });

  it('should create a new task (createTask)', () => {
    const username = 'john_doe';

    service.createTask(username, mockTask).subscribe((task) => {
      expect(task).toEqual(mockTask);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/${username}/tasks`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockTask);
    req.flush(mockTask);
  });

  it('should update an existing task (updateTask)', () => {
    const username = 'john_doe';
    const taskId = 1;
    const updatedTask: Task = { ...mockTask, status: 'COMPLETED' };

    service.updateTask(username, taskId, updatedTask).subscribe((task) => {
      expect(task.status).toBe('COMPLETED');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/${username}/tasks/${taskId}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updatedTask);
    req.flush(updatedTask);
  });

  it('should delete a task (deleteTask)', () => {
    const username = 'john_doe';
    const taskId = 1;

    service.deleteTask(username, taskId).subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/${username}/tasks/${taskId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
