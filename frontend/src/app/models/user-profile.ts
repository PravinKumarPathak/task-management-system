export interface UserProfile {
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export type AuthUser = Omit<UserProfile, 'id' | 'email'> & {
  id?: number;
  email?: string;
};
