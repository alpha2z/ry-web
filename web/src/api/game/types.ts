export interface GameServer {
  id: string | number;
  serverCode: string;
  serverName: string;
  baseUrl: string;
  appId: string;
  timeoutMs: number;
  enabled: boolean;
  remark?: string;
}

export interface ServerForm extends Omit<GameServer, 'id'> {
  id?: string | number;
  secret?: string;
}

export interface PlayerView {
  serverCode: string;
  playerId: string;
  playerName: string;
  level: number;
  status: string;
  assets: Record<string, unknown>;
}

export interface RewardOrder {
  id: string | number;
  orderNo: string;
  title: string;
  status: string;
  deliveryMode: string;
  scheduledAt?: string;
  targetCount: number;
  successCount: number;
  failedCount: number;
  createTime: string;
}

export interface RewardForm {
  title: string;
  mailTitle: string;
  mailContent: string;
  targets: Array<{ serverCode: string; playerId: string }>;
  items: Array<{ itemCode: string; quantity: number }>;
  scheduledAt?: string;
  timezone: string;
  remark?: string;
}

export interface Activity {
  id: string | number;
  activityCode: string;
  activityName: string;
  activityType: string;
  status: string;
  timezone: string;
  startAt: string;
  endAt: string;
  publishAt?: string;
  currentVersion: number;
  serverCodesJson: string;
  ruleJson: string;
  rewardJson: string;
  description?: string;
}

export interface ActivityForm {
  activityCode: string;
  activityName: string;
  activityType: string;
  timezone: string;
  displayStartAt?: string;
  startAt: string;
  endAt: string;
  publishAt?: string;
  serverCodes: string[];
  rule: Record<string, unknown>;
  rewards: Array<Record<string, unknown>>;
  description?: string;
}

export interface JobView {
  name: string;
  group: string;
  trigger?: string;
  state?: string;
  nextFireAt?: string;
  cron?: string;
}

export interface DashboardStats {
  enabledServers: number;
  publishedActivities: number;
  pendingApprovals: number;
  rewardSuccessRate: number;
  failedJobs: number;
}
