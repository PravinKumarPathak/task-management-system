import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthStore } from '../../services/auth-store';

@Component({
  selector: 'app-logout',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './logout.html',
  styleUrl: './logout.css',
})
export class Logout {
  private authStore = inject(AuthStore);

  ngOnInit() {
    this.authStore.logout();
  }
}
