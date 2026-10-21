import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Task } from '../../models/task';
import { AuthUser } from '../../models/user-profile';
import { AuthStore } from '../../services/auth-store';
import { TaskApi } from '../../services/task-api';
import { WelcomeApi } from '../../services/welcome-api';

@Component({
  selector: 'app-welcome',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './welcome.html',
  styleUrl: './welcome.css',
})
export class Welcome implements OnInit {
  private router = inject(Router);
  private welcomeApi = inject(WelcomeApi);
  private taskApi = inject(TaskApi);
  public authStore = inject(AuthStore);

  userProfile = signal<AuthUser | undefined>(undefined);
  errorMessage = signal<string | undefined>(undefined);

  totalTasks = signal<number>(0);
  completedTasks = signal<number>(0);
  pendingTasks = signal<number>(0);
  inProgressTasks = signal<number>(0);

  showDeleteModal = signal<boolean>(false);

  constructor() {
    effect(() => {
      if (this.userProfile()) {
        this.loadTaskMetrics();
      }
    });
  }

  ngOnInit() {
    this.getUserProfile();
  }

  getUserProfile() {
    const currentUser = this.authStore.currentUser();
    if (currentUser && currentUser.email) {
      this.userProfile.set(currentUser);
      return;
    }

    this.welcomeApi.getUserProfile().subscribe({
      next: (response) => this.userProfile.set(response),
      error: (error) => {
        const msg =
          error.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : error.error?.message || error.message || 'Failed to load user profile.';

        this.errorMessage.set(msg);
      },
    });
  }

  loadTaskMetrics() {
    const isAdmin = this.authStore.isAdmin();

    if (isAdmin) {
      this.taskApi.getAllTasks().subscribe({
        next: (tasks) => this.setTaskMetrics(tasks),
        error: (err) => {
          const msg =
            err.status === 0
              ? 'Unable to connect to the server. Please try again later.'
              : err.error?.message || 'Failed to load system task metrics.';
          this.errorMessage.set(msg);
        },
      });
      return;
    }

    const username = this.authStore.getAuthenticatedUser();
    if (!username) return;

    this.taskApi.getUserTasks(username).subscribe({
      next: (tasks) => this.setTaskMetrics(tasks),
      error: (err) => {
        const msg =
          err.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : err.error?.message || 'Failed to load task metrics.';
        this.errorMessage.set(msg);
      },
    });
  }

  openDeleteModal() {
    this.errorMessage.set(undefined); // Clear old errors when opening the modal
    this.showDeleteModal.set(true);
  }

  closeDeleteModal() {
    this.showDeleteModal.set(false);
    this.errorMessage.set(undefined);
  }

  deleteAccount() {
    this.welcomeApi.deleteOwnAccount().subscribe({
      next: () => {
        this.authStore.logout();
        this.router.navigate(['/login']);
      },
      error: (error) => {
        const msg =
          error.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : error.error?.message || 'Failed to delete account.';

        this.showDeleteModal.set(false); // Directly close modal without resetting errorMessage to undefined
        this.errorMessage.set(msg);
      },
    });
  }

  private setTaskMetrics(tasks: Task[]) {
    this.totalTasks.set(tasks.length);
    this.completedTasks.set(tasks.filter((t) => t.status?.toUpperCase() === 'COMPLETED').length);
    this.pendingTasks.set(tasks.filter((t) => t.status?.toUpperCase() === 'PENDING').length);
    this.inProgressTasks.set(tasks.filter((t) => t.status?.toUpperCase() === 'IN_PROGRESS').length);
  }
}
