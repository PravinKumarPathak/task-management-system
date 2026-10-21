import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UserProfile } from '../../models/user-profile';
import { AdminApi } from '../../services/admin-api';
import { AuthStore } from '../../services/auth-store';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css',
})
export class AdminUsers implements OnInit {
  private adminApi = inject(AdminApi);
  public authStore = inject(AuthStore);

  users = signal<UserProfile[]>([]);
  errorMessage = signal<string | undefined>(undefined);
  successMessage = signal<string | undefined>(undefined);
  selectedUserId = signal<number | null>(null);

  currentUsername = this.authStore.getAuthenticatedUser() || '';

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.adminApi.getAllUsers().subscribe({
      next: (data) => this.users.set(data),
      error: (err) => {
        const errorMsg =
          err.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : err.error?.message || 'Failed to load users.';
        this.showError(errorMsg);
      },
    });
  }

  toggleAdminRole(user: UserProfile) {
    this.clearMessages();
    const isCurrentlyAdmin = this.hasAdminRole(user.roles);
    const updatedRoles = isCurrentlyAdmin
      ? user.roles.filter((r) => !['ROLE_ADMIN', 'ADMIN'].includes(r.toUpperCase()))
      : [...user.roles, 'ROLE_ADMIN'];

    this.adminApi.updateUserRoles(user.id, updatedRoles).subscribe({
      next: (updatedUser) => {
        this.users.update((list) => list.map((u) => (u.id === updatedUser.id ? updatedUser : u)));
        this.showSuccess(`Updated role rights for user '${user.username}'.`);
      },
      error: (err) => {
        const errorMsg =
          err.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : err.error?.message || 'Failed to update user roles.';
        this.showError(errorMsg);
      },
    });
  }

  confirmDelete(userId: number) {
    this.selectedUserId.set(userId);
  }

  deleteUser() {
    const id = this.selectedUserId();
    if (!id) return;

    // Retrieve username for contextual success message feedback
    const userToDelete = this.users().find((u) => u.id === id);
    const username = userToDelete ? userToDelete.username : 'User';

    this.clearMessages();
    this.adminApi.deleteUser(id).subscribe({
      next: () => {
        this.selectedUserId.set(null);
        this.showSuccess(`User '${username}' deleted successfully.`);
        this.loadUsers();
      },
      error: (err) => {
        this.selectedUserId.set(null);
        const errorMsg =
          err.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : err.error?.message || 'Failed to delete user.';
        this.showError(errorMsg);
      },
    });
  }

  isSelf(user: UserProfile): boolean {
    return user.username === this.currentUsername;
  }

  hasAdminRole(roles: string[]): boolean {
    return roles.some((r) => ['ROLE_ADMIN', 'ADMIN'].includes(r.toUpperCase()));
  }

  isPeerAdmin(user: UserProfile): boolean {
    return !this.isSelf(user) && this.hasAdminRole(user.roles);
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
