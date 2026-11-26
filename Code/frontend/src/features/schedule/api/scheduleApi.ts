import { apiRequest } from '@/lib/apiClient';
import type {
  ConfirmRequest,
  GenerateScheduleRequest,
  ProposalBatchResponse,
  ProposedSession,
  SessionEditRequest,
} from '../types';

export const scheduleApi = {
  generateProposal(request: GenerateScheduleRequest): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>('/schedules/generate', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  getCurrentProposal(): Promise<ProposalBatchResponse | null> {
    return apiRequest<ProposalBatchResponse | null>('/schedules/proposals/current');
  },

  getProposal(proposalId: number): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>(`/schedules/proposals/${proposalId}`);
  },

  discardProposal(proposalId: number, version: number): Promise<void> {
    return apiRequest<void>(`/schedules/proposals/${proposalId}?version=${version}`, {
      method: 'DELETE',
    });
  },

  editSession(sessionId: number, request: SessionEditRequest): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>(`/schedules/sessions/${sessionId}`, {
      method: 'PUT',
      body: JSON.stringify(request),
    });
  },

  deleteSession(sessionId: number, version: number): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>(
      `/schedules/sessions/${sessionId}?version=${version}`,
      {
        method: 'DELETE',
      },
    );
  },

  confirmProposal(proposalId: number, request: ConfirmRequest): Promise<ProposalBatchResponse> {
    return apiRequest<ProposalBatchResponse>(`/schedules/${proposalId}/confirm`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  listCommittedSessions(startIso: string, endIso: string): Promise<ProposedSession[]> {
    const params = new URLSearchParams({ start: startIso, end: endIso });
    return apiRequest<ProposedSession[]>(`/schedules/sessions?${params.toString()}`);
  },
};
