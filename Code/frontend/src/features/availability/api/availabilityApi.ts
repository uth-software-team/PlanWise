import { apiRequest } from '@/lib/apiClient'
import type {
  AvailabilityRuleRequest,
  AvailabilityRuleResponse,
  FixedEventRequest,
  FixedEventResponse,
} from '../types'

export function listAvailabilityRules(): Promise<AvailabilityRuleResponse[]> {
  return apiRequest<AvailabilityRuleResponse[]>('/availability-rules', {
    method: 'GET',
  })
}

export function createAvailabilityRule(
  payload: AvailabilityRuleRequest,
): Promise<AvailabilityRuleResponse> {
  return apiRequest<AvailabilityRuleResponse>('/availability-rules', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateAvailabilityRule(
  ruleId: number,
  payload: AvailabilityRuleRequest,
): Promise<AvailabilityRuleResponse> {
  return apiRequest<AvailabilityRuleResponse>(`/availability-rules/${ruleId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteAvailabilityRule(ruleId: number): Promise<void> {
  return apiRequest<void>(`/availability-rules/${ruleId}`, {
    method: 'DELETE',
  })
}

export function listFixedEvents(): Promise<FixedEventResponse[]> {
  return apiRequest<FixedEventResponse[]>('/fixed-events', {
    method: 'GET',
  })
}

export function createFixedEvent(payload: FixedEventRequest): Promise<FixedEventResponse> {
  return apiRequest<FixedEventResponse>('/fixed-events', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateFixedEvent(
  eventId: number,
  payload: FixedEventRequest,
): Promise<FixedEventResponse> {
  return apiRequest<FixedEventResponse>(`/fixed-events/${eventId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteFixedEvent(eventId: number): Promise<void> {
  return apiRequest<void>(`/fixed-events/${eventId}`, {
    method: 'DELETE',
  })
}
