import type { components } from '@/api/schema'
import { apiRequest } from '@/lib/apiClient'

export type LoginRequest = components['schemas']['LoginRequest']
export type RegisterRequest = components['schemas']['RegisterRequest']
export type ProfileUpdateRequest = components['schemas']['ProfileUpdateRequest']
export type AuthResponse = components['schemas']['AuthResponse']
export type UserResponse = components['schemas']['UserResponse']

/** POST /auth/login: 200 with the access token and the user; 401 INVALID_CREDENTIALS otherwise. */
export function login(body: LoginRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify(body) })
}

/** POST /auth/register: 201 with the user only (no token); 409 EMAIL_ALREADY_EXISTS for a taken email. */
export function register(body: RegisterRequest): Promise<UserResponse> {
  return apiRequest<UserResponse>('/auth/register', { method: 'POST', body: JSON.stringify(body) })
}

/**
 * POST /auth/logout (204, stateless). Sends the given token explicitly: the caller clears the local session first,
 * so apiRequest has no current token to add and a late 401 here can never end a newer session.
 */
export function logout(token: string): Promise<void> {
  return apiRequest<void>('/auth/logout', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    keepalive: true,
  })
}

export function getMe(): Promise<UserResponse> {
  return apiRequest<UserResponse>('/users/me')
}

export function updateMe(body: ProfileUpdateRequest): Promise<UserResponse> {
  return apiRequest<UserResponse>('/users/me', { method: 'PUT', body: JSON.stringify(body) })
}
