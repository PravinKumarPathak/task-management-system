import { Routes } from '@angular/router';
import { Error } from './components/error/error';
import { adminGuard } from './guards/admin-guard';
import { routeGuard } from './guards/route-guard';
import { AdminUsers } from './pages/admin-users/admin-users';
import { ForgotPassword } from './pages/forgot-password/forgot-password';
import { ListTasks } from './pages/list-tasks/list-tasks';
import { Login } from './pages/login/login';
import { Logout } from './pages/logout/logout';
import { ManageTask } from './pages/manage-task/manage-task';
import { Register } from './pages/register/register';
import { ResetPassword } from './pages/reset-password/reset-password';
import { Welcome } from './pages/welcome/welcome';

export const routes: Routes = [
  { path: '', component: Login },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'forgot-password', component: ForgotPassword },
  { path: 'reset-password', component: ResetPassword },
  { path: 'welcome', component: Welcome, canActivate: [routeGuard] },

  { path: 'admin/users', component: AdminUsers, canActivate: [routeGuard, adminGuard] },

  { path: 'tasks', component: ListTasks, canActivate: [routeGuard] },
  { path: 'logout', component: Logout, canActivate: [routeGuard] },
  { path: 'manage-task/:username/:id', component: ManageTask, canActivate: [routeGuard] },
  { path: '**', component: Error },
];
