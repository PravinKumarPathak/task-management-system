import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Task } from '../../models/task';
import { UserProfile } from '../../models/user-profile';
import { AdminApi } from '../../services/admin-api';
import { AuthStore } from '../../services/auth-store';
import { TaskApi } from '../../services/task-api';

@Component({
  selector: 'app-manage-task',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './manage-task.html',
  styleUrl: './manage-task.css',
})
export class ManageTask implements OnInit {
  private taskApi = inject(TaskApi);
  private adminApi = inject(AdminApi);
  private route = inject(ActivatedRoute);
  public authStore = inject(AuthStore);
  private router = inject(Router);

  username = this.route.snapshot.params['username'];
  id = Number(this.route.snapshot.params['id']);

  task = signal<Task | undefined>(undefined);
  users = signal<UserProfile[]>([]);
  errorMessage = signal<string | undefined>(undefined);

  authenticatedUser = this.authStore.getAuthenticatedUser();

  ngOnInit() {
    if (this.authStore.isAdmin()) {
      this.adminApi.getAllUsers().subscribe({
        next: (userList) => this.users.set(userList),
        error: (err) => {
          const errorMsg =
            err.status === 0
              ? 'Unable to connect to the server. Please try again later.'
              : err.error?.message || 'Failed to load user list.';
          this.errorMessage.set(errorMsg);
        },
      });
    }

    if (this.id !== -1) {
      this.taskApi.getTask(this.username, this.id).subscribe({
        next: (response) => this.task.set(response),
        error: (err) => {
          const errorMsg =
            err.status === 0
              ? 'Unable to connect to the server. Please try again later.'
              : err.error?.message || 'Task not found.';
          this.errorMessage.set(errorMsg);
        },
      });
    } else {
      const initialUser =
        this.username && !this.authStore.isAdmin() ? this.username : this.authenticatedUser;

      this.username = initialUser;
      this.task.set({
        username: this.username,
        taskDescription: '',
        dueDate: new Date(),
        status: 'IN_PROGRESS',
      });
    }
  }

  saveTask() {
    this.errorMessage.set(undefined);

    const currentTask = this.task();
    if (!currentTask) {
      this.errorMessage.set('Task data is not loaded yet.');
      return;
    }

    if (this.id !== -1) {
      this.taskApi.updateTask(this.username, this.id, currentTask).subscribe({
        next: () => this.router.navigate(['/tasks']),
        error: (err) => {
          const errorMsg =
            err.status === 0
              ? 'Unable to connect to the server. Please try again later.'
              : err.error?.message ||
                `User '${this.username}' does not exist or invalid task data.`;
          this.errorMessage.set(errorMsg);
        },
      });
    } else {
      this.taskApi.createTask(this.username, currentTask).subscribe({
        next: () => this.router.navigate(['/tasks']),
        error: (err) => {
          const errorMsg =
            err.status === 0
              ? 'Unable to connect to the server. Please try again later.'
              : err.error?.message || `User '${this.username}' does not exist.`;
          this.errorMessage.set(errorMsg);
        },
      });
    }
  }

  cancel() {
    this.router.navigate(['/tasks']);
  }
}
