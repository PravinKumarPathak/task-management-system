export interface Task {
  id?: number;
  username?: string;
  taskDescription: string;
  dueDate: Date | string;
  status: string;
}
