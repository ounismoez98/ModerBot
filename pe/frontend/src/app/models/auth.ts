export type UserRole = 'STUDENT' | 'MODERATOR';

export interface LoginResponse {
  username: string;
  role: UserRole;
}
