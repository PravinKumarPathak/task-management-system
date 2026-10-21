import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../services/auth-store';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  username = signal<string>('');
  password = signal<string>('');
  errorMessage = signal<string>('Invalid username or password.');
  invalidLogin = signal<boolean>(false);
  isLoading = signal<boolean>(false);

  private router = inject(Router);
  private authStore = inject(AuthStore);

  handleJWTAuthLogin(): void {
    const user = this.username().trim();
    const pass = this.password();

    this.isLoading.set(true);
    this.invalidLogin.set(false);

    this.authStore.executeJWTAuthenticationService(user, pass).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.invalidLogin.set(false);
        this.router.navigate(['/welcome']);
      },
      error: (error) => {
        console.error('Login failed:', error);
        this.isLoading.set(false);

        if (error.status === 0) {
          this.errorMessage.set('Unable to connect to the server. Please try again later.');
        } else {
          this.errorMessage.set(error.error?.message || 'Invalid username or password.');
        }

        this.invalidLogin.set(true);
      },
    });
  }
}
