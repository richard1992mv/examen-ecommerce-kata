/** Body de POST /auth/login (LoginRequest del backend). */
export interface LoginRequest {
  username: string;
  password: string;
}

/** Respuesta de POST /auth/login (TokenResponse del backend). */
export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number; // segundos
}
