import request from '@/utils/request';
import type { ActivityForm, RewardForm, ServerForm } from './types';

export const getDashboard = () => request({ url: '/game/dashboard', method: 'get' });

export const listServers = () => request({ url: '/game/servers/list', method: 'get' });
export const addServer = (data: ServerForm) => request({ url: '/game/servers', method: 'post', data });
export const updateServer = (id: string | number, data: ServerForm) => request({ url: `/game/servers/${id}`, method: 'put', data });
export const deleteServer = (id: string | number) => request({ url: `/game/servers/${id}`, method: 'delete' });
export const checkServer = (code: string) => request({ url: `/game/servers/${code}/health`, method: 'get' });

export const getPlayer = (serverCode: string, playerId: string) =>
  request({ url: `/game/players/${encodeURIComponent(serverCode)}/${encodeURIComponent(playerId)}`, method: 'get' });

export const listRewards = (params: Record<string, unknown>) => request({ url: '/game/rewards/list', method: 'get', params });
export const getReward = (id: string | number) => request({ url: `/game/rewards/${id}`, method: 'get' });
export const addReward = (data: RewardForm) => request({ url: '/game/rewards', method: 'post', data });
export const approveReward = (id: string | number) => request({ url: `/game/rewards/${id}/approve`, method: 'post' });
export const retryReward = (id: string | number) => request({ url: `/game/rewards/${id}/retry`, method: 'post' });
export const cancelReward = (id: string | number) => request({ url: `/game/rewards/${id}/cancel`, method: 'post' });

export const listActivities = (params: Record<string, unknown>) => request({ url: '/game/activities/list', method: 'get', params });
export const getActivity = (id: string | number) => request({ url: `/game/activities/${id}`, method: 'get' });
export const addActivity = (data: ActivityForm) => request({ url: '/game/activities', method: 'post', data });
export const updateActivity = (id: string | number, data: ActivityForm) => request({ url: `/game/activities/${id}`, method: 'put', data });
export const publishActivity = (id: string | number) => request({ url: `/game/activities/${id}/publish`, method: 'post' });
export const offlineActivity = (id: string | number) => request({ url: `/game/activities/${id}/offline`, method: 'post' });

export const listJobs = () => request({ url: '/game/jobs/list', method: 'get' });
export const listJobLogs = (params: Record<string, unknown>) => request({ url: '/game/jobs/logs', method: 'get', params });
export const pauseJob = (group: string, name: string) => request({ url: `/game/jobs/${group}/${name}/pause`, method: 'post' });
export const resumeJob = (group: string, name: string) => request({ url: `/game/jobs/${group}/${name}/resume`, method: 'post' });
export const runJob = (group: string, name: string) => request({ url: `/game/jobs/${group}/${name}/run`, method: 'post' });
