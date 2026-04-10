export interface LotteryStatus {
  [key: string]: {
    name: string
    count: number
    latestDraw: string | null
    latestNumbers: string | null
  }
}

export interface LotteryResult {
  id: number
  lotteryType: string
  drawNum: string
  drawDate: string
  numbers: string
  extraInfo: string | null
  fetchedAt: string
  createdAt: string | null
  createdBy: string | null
  updatedAt: string | null
  updatedBy: string | null
}

export interface FetchTaskInfo {
  taskId: string
  type: string
  scope: string
  mode: string
  triggerSource: string
  status: 'pending' | 'running' | 'success' | 'partial_failed' | 'failed' | 'not_found'
  currentType?: string
  currentPage?: number
  page?: number
  totalFetched?: number
  total?: number
  inserted?: number
  new?: number
  updated?: number
  completedTypes?: number
  totalTypes?: number
  startedAt?: string
  finishedAt?: string
  error?: string
  results?: Record<string, FetchDetailResult>
  summary?: Record<string, unknown>
}

export interface FetchDetailResult {
  type: string
  name?: string
  scope?: string
  status: string
  currentPage?: number
  page?: number
  totalFetched?: number
  total?: number
  inserted?: number
  new?: number
  updated?: number
  error?: string
}

export interface FetchHistoryItem {
  taskId: string
  triggerSource: string
  type: string
  scope: string
  mode: string
  status: string
  currentType?: string
  currentPage?: number
  totalFetched?: number
  inserted?: number
  updated?: number
  completedTypes?: number
  totalTypes?: number
  startedAt?: string
  finishedAt?: string
  error?: string
}

export interface FetchHistoryResponse {
  data: FetchHistoryItem[]
  total: number
  limit: number
  offset: number
}

export interface FetchHistoryDetail extends FetchHistoryItem {
  results?: Record<string, FetchDetailResult>
  detailRows?: FetchDetailResult[]
}

export interface AnalysisData {
  lotteryType: string
  name: string
  totalDraws: number
  // SSQ specific
  redFreq?: Record<string, number>
  blueFreq?: Record<string, number>
  redHot?: string[]
  redCold?: string[]
  blueHot?: string[]
  blueCold?: string[]
  redMissing?: Record<string, number>
  blueMissing?: Record<string, number>
  // DLT specific
  frontFreq?: Record<string, number>
  backFreq?: Record<string, number>
  frontHot?: string[]
  frontCold?: string[]
  backHot?: string[]
  backCold?: string[]
  // Positional specific
  positions?: Array<{
    position: number
    freq: Record<string, number>
    hot: string[]
    cold: string[]
  }>
  // Common
  sumStats?: { avg: number; min: number; max: number }
  spanStats?: { avg: number; min: number; max: number }
  acStats?: { avg: number; min: number; max: number }
  consecutiveStats?: { avg: number }
  avgRepeats?: number
  sumTails?: Record<string, number>
  spanDistribution?: Record<string, number>
  acDistribution?: Record<string, number>
  consecutiveDistribution?: Record<string, number>
  oddEvenRatio?: Record<string, number>
  sizeRatio?: Record<string, number>
  primeCompositeRatio?: Record<string, number>
  mod012Ratio?: Record<string, number>
  zoneRatio3?: Record<string, number>
  dragonTiger?: { dragon: number; tiger: number; draw: number; dragonPct: number; tigerPct: number }
  topCombos?: Array<{ combo: string; count: number }>
  sumDistribution?: Record<string, number>
  error?: string
}

export interface TrendData {
  lotteryType: string
  trend: Array<{
    drawNum: string
    drawDate: string
    numbers: string
    sum?: number
    span?: number
    ac?: number
    consecutive?: number
    oddCount?: number
    bigCount?: number
    primeCount?: number
    repeats?: number
    zone1?: number
    zone2?: number
    zone3?: number
    blue?: number
    frontSum?: number
    frontSpan?: number
    frontAC?: number
    frontOdd?: number
    frontPrime?: number
    backSum?: number
    positions?: number[]
  }>
  info?: string
}

export interface RecommendGroup {
  strategy: string
  display: string
  reds?: string[]
  blues?: string[]
  fronts?: string[]
  backs?: string[]
  digits?: string[]
  numbers?: string[]
  weight?: number
}

export interface RecommendData {
  date: string
  lotteryType: string
  name: string
  groups: RecommendGroup[]
  error?: string
}

export interface RecommendHistoryItem {
  date: string
  groups: Array<{
    strategy: string
    strategyIndex: number
    numbers: string
    actual: string | null
    hitMain: number
    hitExtra: number
    date: string
  }>
}

export interface RecommendHistoryResponse {
  data: RecommendHistoryItem[]
  total: number
}

export interface RecommendStatsResponse {
  lotteryType: string
  totalDays: number
  stats: Array<{
    strategy: string
    total: number
    hitCount: number
    hitRate: number
    avgHitMain: number
    maxHitMain: number
    avgHitExtra: number
    maxHitExtra: number
  }>
}
