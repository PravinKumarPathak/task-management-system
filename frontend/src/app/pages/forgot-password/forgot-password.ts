import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-forgot-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPassword {
  private http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  email = '';
  message = signal<string | undefined>(undefined);
  errorMessage = signal<string | undefined>(undefined);
  isLoading = signal<boolean>(false);

  requestReset() {
    this.errorMessage.set(undefined);
    this.message.set(undefined);
    this.isLoading.set(true);

    this.http.post<any>(`${this.apiUrl}/auth/forgot-password`, { email: this.email }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.message.set(res.message);
      },
      error: (err) => {
        this.isLoading.set(false);
        let errorMsg = 'Failed to submit request. Please try again.';

        if (err.status === 0) {
          errorMsg = 'Unable to connect to the server. Please try again later.';
        } else if (err.status === 429) {
          errorMsg = 'Too many requests. Please try again later.';
        } else if (err.error?.message) {
          errorMsg = err.error.message;
        }

        this.errorMessage.set(errorMsg);
      },
    });
  }
}
