export interface Me {
  id: number;
  username: string;
  email: string;
  fullName: string;
  company: { id: number; code: string; name: string };
  roles: string[];
  permissions: string[];
}

export interface RegisterResponse {
  message: string;
  status: "ACTIVE" | "PENDING";
}
