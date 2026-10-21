import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Task } from '../../models/task';
import { UserProfile } from '../../models/user-profile';
import { AdminApi } from '../../services/admin-api';
import { AuthStore } from '../../services/auth-store';
import { TaskApi } from '../../services/task-api';

@Component({
  selector: 'app-list-tasks',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './list-tasks.html',
  styleUrl: './list-tasks.css',
})
export class ListTasks implements OnInit {
  tasks = signal<Task[] | undefined>(undefined);
  users = signal<UserProfile[]>([]);
  errorMessage = signal<string | undefined>(undefined);
  successMessage = signal<string | undefined>(undefined);

  searchedUsername = signal<string>('');
  isSearching = signal<boolean>(false);

  private taskApi = inject(TaskApi);
  private adminApi = inject(AdminApi);
  private router = inject(Router);
  public authStore = inject(AuthStore);

  authenticatedUser = this.authStore.getAuthenticatedUser();

  ngOnInit() {
    this.refreshTasks();
    if (this.authStore.isAdmin()) {
      this.loadUsers();
    }
  }

  loadUsers() {
    this.adminApi.getAllUsers().subscribe({
      next: (userList) => this.users.set(userList),
      error: (err) => {
        const errorMsg =
          err.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : err.error?.message || 'Failed to load user list for filtering.';
        this.showError(errorMsg);
      },
    });
  }

  refreshTasks() {
    this.searchedUsername.set('');

    if (this.authStore.isAdmin()) {
      this.taskApi.getAllTasks().subscribe({
        next: (response) => {
          this.tasks.set(response);
        },
        error: (err) => {
          const errorMsg =
            err.status === 0
              ? 'Unable to connect to the server. Please try again later.'
              : err.error?.message || 'Failed to load task list.';
          this.showError(errorMsg);
          this.tasks.set([]);
        },
      });
    } else {
      const user = this.authenticatedUser;
      if (!user) return;

      this.taskApi.getUserTasks(user).subscribe({
        next: (response) => {
          this.tasks.set(response);
        },
        error: (err) => {
          const errorMsg =
            err.status === 0
              ? 'Unable to connect to the server. Please try again later.'
              : err.error?.message || 'Failed to load your tasks.';
          this.showError(errorMsg);
          this.tasks.set([]);
        },
      });
    }
  }

  searchTasks() {
    const selectedUser = this.searchedUsername();
    if (!selectedUser) {
      this.refreshTasks();
      return;
    }

    this.isSearching.set(true);

    this.adminApi.getTasksByUsername(selectedUser).subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.isSearching.set(false);
      },
      error: (err) => {
        this.tasks.set([]);
        this.isSearching.set(false);
        const errorMsg =
          err.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : err.error?.message || `No tasks found for user '${selectedUser}'.`;
        this.showError(errorMsg);
      },
    });
  }

  onUserFilterChange(selectedUser: string) {
    this.clearMessages();
    if (selectedUser) {
      this.searchedUsername.set(selectedUser);
      this.searchTasks();
    } else {
      this.refreshTasks();
    }
  }

  deleteTask(username: string | undefined, id: number | undefined) {
    if (!username || id === undefined) return;

    this.clearMessages();
    this.taskApi.deleteTask(username, id).subscribe({
      next: () => {
        this.showSuccess(`Task deleted successfully.`);
        if (this.searchedUsername()) {
          this.searchTasks();
        } else {
          this.refreshTasks();
        }
      },
      error: (err) => {
        const errorMsg =
          err.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : err.error?.message || `Failed to delete task ${id}.`;

        this.showError(errorMsg);
      },
    });
  }

  updateTask(username: string | undefined, id: number | undefined) {
    if (!username || id === undefined) return;
    this.router.navigate(['/manage-task', username, id]);
  }

  addTask() {
    const targetUser =
      this.authStore.isAdmin() && this.searchedUsername()
        ? this.searchedUsername()
        : this.authenticatedUser || '';

    this.router.navigate(['/manage-task', targetUser, -1]);
  }

  formatStatus(status: string | undefined): string {
    if (!status) return 'Pending';
    return status
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  getStatusClass(status: string | undefined): string {
    const s = (status || '').toUpperCase();
    if (s === 'PENDING') return 'bg-warning text-dark';
    if (s === 'IN_PROGRESS') return 'bg-primary text-white';
    if (s === 'COMPLETED') return 'bg-success text-white';
    return 'bg-secondary text-white';
  }

  showSuccess(msg: string) {
    this.successMessage.set(msg);
    this.errorMessage.set(undefined);
    setTimeout(() => this.successMessage.set(undefined), 4000);
  }

  showError(msg: string) {
    this.errorMessage.set(msg);
    this.successMessage.set(undefined);
  }

  clearMessages() {
    this.errorMessage.set(undefined);
    this.successMessage.set(undefined);
  }
}
