import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-reset-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
})
export class ResetPassword implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private router = inject(Router);
  private readonly apiUrl = environment.apiUrl;

  token = '';
  newPassword = '';
  errorMessage = signal<string | undefined>(undefined);
  isLoading = signal<boolean>(false);

  ngOnInit() {
    this.token = this.route.snapshot.queryParams['token'] || '';
    if (!this.token) {
      this.errorMessage.set('Invalid or missing password reset token.');
    }
  }

  performReset() {
    if (!this.token || this.isLoading()) return;

    this.errorMessage.set(undefined);
    this.isLoading.set(true);

    this.http
      .post<any>(`${this.apiUrl}/auth/reset-password`, {
        token: this.token,
        newPassword: this.newPassword,
      })
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          alert('Password updated successfully! Redirecting to sign in.');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.isLoading.set(false);
          const errorMsg =
            err.status === 0
              ? 'Unable to connect to the server. Please try again later.'
              : err.error?.message || 'Invalid or expired token.';

          this.errorMessage.set(errorMsg);
        },
      });
  }
}
