import { Routes } from '@angular/router';
import { loginGuard, moderatorGuard, studentGuard } from './auth.guards';
import { LoginPage } from './pages/login/login';
import { StudentHome } from './pages/student-home/student-home';
import { ModeratorDashboard } from './pages/moderator-dashboard/moderator-dashboard';

export const routes: Routes = [
  { path: 'login', component: LoginPage, canActivate: [loginGuard] },
  { path: 'home', component: StudentHome, canActivate: [studentGuard] },
  { path: 'moderation', component: ModeratorDashboard, canActivate: [moderatorGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' },
];
