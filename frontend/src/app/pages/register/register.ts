import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../services/auth-store';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  username = '';
  email = '';
  password = '';

  errorMessage = signal<string | undefined>(undefined);
  isSubmitting = signal(false);

  private router = inject(Router);
  private authStore = inject(AuthStore);

  handleRegister() {
    if (this.isSubmitting()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(undefined);

    this.authStore.registerUser(this.username, this.email, this.password).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const errorMsg =
          err.status === 0
            ? 'Unable to connect to the server. Please try again later.'
            : err?.error?.message ||
              'Registration failed. Please check your details and try again.';

        this.errorMessage.set(errorMsg);
      },
    });
  }

  cancel() {
    this.router.navigate(['/login']);
  }
}
