// ============================================================================
//  Cadejo — deterministic engine, TypeScript port (server-side re-validation)
//
//  This mirrors the Kotlin domain in :core:domain EXACTLY. It exists so the
//  Edge Function can re-run a submitted intent tape from the daily seed and
//  recompute the score itself — never trusting the number the client sends.
//
//  Determinism is bit-for-bit: 64-bit values use BigInt with explicit masking so
//  the SplitMix64 PRNG, the FNV-1a seed and the level generator produce the same
//  numbers as Kotlin's Long. Iteration order (hex sort, DIRECTIONS, first-minimum
//  tie-breaks) is replicated precisely.
//
//  ⚠ Before going live, cross-validate against the Kotlin golden vectors — see
//  backend/README.md. A silent mismatch would reject legitimate scores.
// ============================================================================

const MASK64 = (1n << 64n) - 1n;
const u64 = (x: bigint): bigint => x & MASK64;

// --- Hex (axial) ------------------------------------------------------------

export interface Hex {
  q: number;
  r: number;
}
const hexKey = (h: Hex): string => `${h.q},${h.r}`;
const hexAdd = (a: Hex, b: Hex): Hex => ({ q: a.q + b.q, r: a.r + b.r });
const hexSub = (a: Hex, b: Hex): Hex => ({ q: a.q - b.q, r: a.r - b.r });
const hexEq = (a: Hex, b: Hex): boolean => a.q === b.q && a.r === b.r;

function hexDistance(a: Hex, b: Hex): number {
  const dq = a.q - b.q;
  const dr = a.r - b.r;
  const ds = -a.q - a.r - (-b.q - b.r);
  return (Math.abs(dq) + Math.abs(dr) + Math.abs(ds)) / 2;
}

// Clockwise from East — MUST match Kotlin Hex.DIRECTIONS.
const DIRECTIONS: Hex[] = [
  { q: 1, r: 0 }, { q: 1, r: -1 }, { q: 0, r: -1 },
  { q: -1, r: 0 }, { q: -1, r: 1 }, { q: 0, r: 1 },
];
const neighbors = (h: Hex): Hex[] => DIRECTIONS.map((d) => hexAdd(h, d));

function disc(radius: number): Hex[] {
  const out: Hex[] = [];
  for (let dq = -radius; dq <= radius; dq++) {
    const rMin = Math.max(-radius, -dq - radius);
    const rMax = Math.min(radius, -dq + radius);
    for (let dr = rMin; dr <= rMax; dr++) out.push({ q: dq, r: dr });
  }
  return out;
}

// Total order matching Kotlin compareBy({ q }, { r }).
const hexCompare = (a: Hex, b: Hex): number => (a.q !== b.q ? a.q - b.q : a.r - b.r);

/** First element achieving the minimum key (stable), mirroring Kotlin minByOrNull. */
function minBy<T>(items: T[], key: (t: T) => number): T | undefined {
  let best: T | undefined;
  let bestK = Number.POSITIVE_INFINITY;
  for (const it of items) {
    const k = key(it);
    if (k < bestK) {
      bestK = k;
      best = it;
    }
  }
  return best;
}

// --- SplitMix64 PRNG --------------------------------------------------------

const GAMMA = 0x9e3779b97f4a7c15n;
const MIX1 = 0xbf58476d1ce4e5b9n;
const MIX2 = 0x94d049bb133111ebn;

class SeededRng {
  state: bigint;
  constructor(seed: bigint) {
    this.state = u64(seed);
  }
  nextLong(): bigint {
    this.state = u64(this.state + GAMMA);
    let z = this.state;
    z = u64((z ^ (z >> 30n)) * MIX1);
    z = u64((z ^ (z >> 27n)) * MIX2);
    return u64(z ^ (z >> 31n));
  }
  nextInt(bound: number): number {
    return Number((this.nextLong() >> 1n) % BigInt(bound));
  }
  pick<T>(list: T[]): T | undefined {
    return list.length === 0 ? undefined : list[this.nextInt(list.length)];
  }
}

// --- Model ------------------------------------------------------------------

type Terrain = "FLOOR" | "WALL" | "GOAL" | "HAZARD";
type PatternType = "CHASE" | "PATROL" | "MIRROR";
type Status = "PLAYING" | "WON" | "LOST";
type AbilityId = "HOWL" | "LEAP" | "PROTECTIVE_LIGHT";

interface Board {
  terrain: Map<string, Terrain>;
}
const terrainAt = (b: Board, h: Hex): Terrain | undefined => b.terrain.get(hexKey(h));
const isWalkable = (b: Board, h: Hex): boolean => {
  const t = terrainAt(b, h);
  return t !== undefined && t !== "WALL";
};

interface Enemy {
  id: string;
  kind: string;
  position: Hex;
  pattern: PatternType;
  stunnedTurns: number;
  patrolRoute: Hex[];
  patrolIndex: number;
}
interface AbilityState {
  id: AbilityId;
  cooldown: number;
  remaining: number;
}
interface GameState {
  board: Board;
  player: Hex;
  traveler: Hex;
  enemies: Enemy[];
  goal: Hex;
  seed: bigint;
  rngState: bigint;
  level: number;
  turn: number;
  status: Status;
  travelerShield: number;
  abilities: AbilityState[];
  score: number;
  lastPlayerStep: Hex | null;
}

const Balance = {
  HOWL_COOLDOWN: 4, HOWL_RADIUS: 2, HOWL_STUN_TURNS: 1,
  LEAP_COOLDOWN: 3, LEAP_RANGE: 2,
  LIGHT_COOLDOWN: 5, LIGHT_SHIELD_TURNS: 3,
  LEVELS_PER_RUN: 10, PAR_TURNS: 40,
};
const defaultAbilities = (): AbilityState[] => [
  { id: "HOWL", cooldown: Balance.HOWL_COOLDOWN, remaining: 0 },
  { id: "LEAP", cooldown: Balance.LEAP_COOLDOWN, remaining: 0 },
  { id: "PROTECTIVE_LIGHT", cooldown: Balance.LIGHT_COOLDOWN, remaining: 0 },
];
const ability = (s: GameState, id: AbilityId) => s.abilities.find((a) => a.id === id);

// --- Pathing ----------------------------------------------------------------

function bfsDistances(board: Board, from: Hex, blocked: Set<string>): Map<string, number> {
  const dist = new Map<string, number>();
  const queue: Hex[] = [from];
  dist.set(hexKey(from), 0);
  while (queue.length > 0) {
    const cur = queue.shift()!;
    const d = dist.get(hexKey(cur))!;
    for (const n of neighbors(cur)) {
      const k = hexKey(n);
      if (dist.has(k) || !isWalkable(board, n) || blocked.has(k)) continue;
      dist.set(k, d + 1);
      queue.push(n);
    }
  }
  return dist;
}

function stepFrom(board: Board, from: Hex, target: Hex, blocked: Set<string>): Hex {
  const dist = bfsDistances(board, target, blocked);
  const candidates = neighbors(from).filter((h) => isWalkable(board, h) && !blocked.has(hexKey(h)));
  const best = minBy(candidates, (h) => dist.get(hexKey(h)) ?? Number.MAX_SAFE_INTEGER);
  const bestD = best ? dist.get(hexKey(best)) ?? Number.MAX_SAFE_INTEGER : Number.MAX_SAFE_INTEGER;
  const hereD = dist.get(hexKey(from)) ?? Number.MAX_SAFE_INTEGER;
  return best && bestD < hereD ? best : from;
}

function enemyStep(state: GameState, enemy: Enemy, target: Hex, blocked: Set<string>): Enemy {
  if (enemy.pattern === "MIRROR") {
    const delta = state.lastPlayerStep;
    if (delta) {
      const mirrored = hexAdd(enemy.position, delta);
      if (isWalkable(state.board, mirrored) && !blocked.has(hexKey(mirrored))) {
        return { ...enemy, position: mirrored };
      }
    }
    return { ...enemy, position: stepFrom(state.board, enemy.position, target, blocked) };
  }
  if (enemy.pattern === "PATROL" && enemy.patrolRoute.length > 0) {
    const waypoint = enemy.patrolRoute[enemy.patrolIndex % enemy.patrolRoute.length];
    if (hexEq(enemy.position, waypoint)) {
      const nextIndex = (enemy.patrolIndex + 1) % enemy.patrolRoute.length;
      const next = enemy.patrolRoute[nextIndex];
      return { ...enemy, position: stepFrom(state.board, enemy.position, next, blocked), patrolIndex: nextIndex };
    }
    return { ...enemy, position: stepFrom(state.board, enemy.position, waypoint, blocked) };
  }
  return { ...enemy, position: stepFrom(state.board, enemy.position, target, blocked) };
}

// --- Engine (mirrors GameEngine.reduce) ------------------------------------

function levelCompletionScore(s: GameState): number {
  const speed = Math.max(0, Balance.PAR_TURNS - s.turn) * 5;
  const shield = s.travelerShield > 0 ? 15 : 0;
  return 100 + s.level * 25 + speed + shield;
}

type Intent =
  | { kind: "WAIT" }
  | { kind: "MOVE"; target: Hex }
  | { kind: "ABILITY"; id: AbilityId; target: Hex | null };

function applyPlayerAction(state: GameState, intent: Intent): GameState | null {
  if (intent.kind === "WAIT") return { ...state, lastPlayerStep: null };
  if (intent.kind === "MOVE") {
    const t = intent.target;
    const legal = neighbors(state.player).some((n) => hexEq(n, t)) &&
      isWalkable(state.board, t) && !hexEq(t, state.traveler);
    if (!legal) return null;
    return { ...state, player: t, lastPlayerStep: hexSub(t, state.player) };
  }
  // ABILITY
  const ab = ability(state, intent.id);
  if (!ab || ab.remaining !== 0) return null;
  const triggered = state.abilities.map((a) => (a.id === ab.id ? { ...a, remaining: a.cooldown } : a));
  if (intent.id === "HOWL") {
    const enemies = state.enemies.map((e) =>
      hexDistance(e.position, state.player) <= Balance.HOWL_RADIUS
        ? { ...e, stunnedTurns: Math.max(e.stunnedTurns, Balance.HOWL_STUN_TURNS + 1) }
        : e
    );
    return { ...state, enemies, abilities: triggered, lastPlayerStep: null };
  }
  if (intent.id === "PROTECTIVE_LIGHT") {
    return { ...state, travelerShield: Balance.LIGHT_SHIELD_TURNS + 1, abilities: triggered, lastPlayerStep: null };
  }
  // LEAP
  const t = intent.target;
  if (!t) return null;
  const dist = hexDistance(t, state.player);
  const legal = dist >= 1 && dist <= Balance.LEAP_RANGE && isWalkable(state.board, t) && !hexEq(t, state.traveler);
  if (!legal) return null;
  return { ...state, player: t, abilities: triggered, lastPlayerStep: null };
}

function moveTraveler(state: GameState): GameState {
  const blocked = new Set<string>();
  for (const e of state.enemies) blocked.add(hexKey(e.position));
  blocked.add(hexKey(state.player));
  return { ...state, traveler: stepFrom(state.board, state.traveler, state.player, blocked) };
}

function moveEnemies(state: GameState): GameState {
  let working = state;
  const updated = working.enemies.map((enemy) => {
    if (enemy.stunnedTurns > 0) return { ...enemy, stunnedTurns: enemy.stunnedTurns - 1 };
    const blocked = new Set<string>();
    for (const other of working.enemies) if (other.id !== enemy.id) blocked.add(hexKey(other.position));
    if (working.travelerShield > 0) blocked.add(hexKey(working.traveler));
    const moved = enemyStep(working, enemy, working.traveler, blocked);
    working = { ...working, enemies: working.enemies.map((e) => (e.id === moved.id ? moved : e)) };
    return moved;
  });
  return { ...working, enemies: updated };
}

function reduce(state: GameState, intent: Intent): GameState {
  if (state.status !== "PLAYING") return state;
  const afterPlayer = applyPlayerAction(state, intent);
  if (!afterPlayer) return state;

  if (hexEq(afterPlayer.player, afterPlayer.goal)) {
    return {
      ...afterPlayer,
      status: "WON",
      turn: afterPlayer.turn + 1,
      score: afterPlayer.score + levelCompletionScore(afterPlayer),
      abilities: afterPlayer.abilities.map((a) => ({ ...a, remaining: Math.max(0, a.remaining - 1) })),
    };
  }
  if (afterPlayer.enemies.some((e) => hexEq(e.position, afterPlayer.player))) {
    return { ...afterPlayer, status: "LOST", turn: afterPlayer.turn + 1 };
  }

  const afterTraveler = moveTraveler(afterPlayer);
  const afterEnemies = moveEnemies(afterTraveler);

  const caughtPlayer = afterEnemies.enemies.some((e) => hexEq(e.position, afterEnemies.player));
  const caughtTraveler = afterEnemies.travelerShield === 0 &&
    afterEnemies.enemies.some((e) => hexEq(e.position, afterEnemies.traveler));
  const status: Status = caughtPlayer || caughtTraveler ? "LOST" : "PLAYING";

  return {
    ...afterEnemies,
    status,
    turn: afterEnemies.turn + 1,
    abilities: afterEnemies.abilities.map((a) => ({ ...a, remaining: Math.max(0, a.remaining - 1) })),
    travelerShield: Math.max(0, afterEnemies.travelerShield - 1),
  };
}

// --- Level generator (mirrors LevelGenerator) ------------------------------

const MIN_RADIUS = 3, MAX_RADIUS = 6, MAX_ENEMIES = 4;

function isReachable(radius: number, walls: Set<string>, from: Hex, goal: Hex): boolean {
  const onBoard = new Set(disc(radius).map(hexKey));
  const visited = new Set<string>([hexKey(from)]);
  const queue: Hex[] = [from];
  while (queue.length > 0) {
    const cur = queue.shift()!;
    if (hexEq(cur, goal)) return true;
    for (const n of neighbors(cur)) {
      const k = hexKey(n);
      if (visited.has(k) || !onBoard.has(k) || walls.has(k)) continue;
      visited.add(k);
      queue.push(n);
    }
  }
  return false;
}

function enemyArchetype(i: number, levelIndex: number): [string, PatternType] {
  if (i === 0) return ["CADEJO_NEGRO", "MIRROR"];
  if (i === 1) return ["LLORONA", "CHASE"];
  if (i === 2) return ["SOMBRERON", "PATROL"];
  return levelIndex % 2 === 0 ? ["LLORONA", "CHASE"] : ["SOMBRERON", "PATROL"];
}

export function generateLevel(seed: bigint, levelIndex: number, startingScore: number): GameState {
  const rng = new SeededRng(u64(seed * 0x9e3779b1n + BigInt(levelIndex)));
  const radius = Math.min(MAX_RADIUS, Math.max(MIN_RADIUS, MIN_RADIUS + Math.floor(levelIndex / 2)));
  const allHexes = disc(radius).sort(hexCompare);
  const rim = allHexes.filter((h) => hexDistance(h, { q: 0, r: 0 }) === radius);

  const player = rng.pick(rim) ?? { q: 0, r: 0 };
  const goalCandidates = rim.filter((h) => !hexEq(h, player));
  let goal = { q: 0, r: 0 };
  {
    // Kotlin: maxWith(compareBy({ distanceTo(player) }, { hexOrder.compare(it, player) })).
    // The second key is a Comparator result, i.e. the SIGN (-1/0/1), not the raw
    // coordinate difference. maxWith keeps the FIRST maximal element (candidates
    // are iterated in sorted order), so we only replace on a strict increase.
    let best: Hex | undefined;
    let bestD = -1, bestTie = -Infinity;
    for (const h of goalCandidates) {
      const d = hexDistance(h, player);
      const tie = Math.sign(hexCompare(h, player));
      if (d > bestD || (d === bestD && tie > bestTie)) {
        best = h; bestD = d; bestTie = tie;
      }
    }
    if (best) goal = best;
  }

  const travelerCandidates = neighbors(player).filter(
    (h) => allHexes.some((a) => hexEq(a, h)) && !hexEq(h, goal),
  );
  const traveler = minBy(travelerCandidates, (h) => hexDistance(h, goal)) ?? player;

  const reserved = new Set<string>([hexKey(player), hexKey(goal), hexKey(traveler)]);

  // Walls
  const budget = Math.min(levelIndex, Math.floor(allHexes.length / 5));
  const walls = new Set<string>();
  const wallCandidates = allHexes.filter((h) => !reserved.has(hexKey(h)));
  let attempts = 0;
  while (walls.size < budget && attempts < budget * 5) {
    attempts++;
    const c = rng.pick(wallCandidates);
    if (!c) break;
    if (walls.has(hexKey(c))) continue;
    const trial = new Set(walls);
    trial.add(hexKey(c));
    if (isReachable(radius, trial, player, goal)) walls.add(hexKey(c));
  }

  // Enemies
  const count = Math.min(MAX_ENEMIES, Math.max(1, 1 + Math.floor(levelIndex / 3)));
  const taken = new Set(reserved);
  const enemies: Enemy[] = [];
  for (let i = 0; i < count; i++) {
    const open = allHexes.filter(
      (h) => !walls.has(hexKey(h)) && !taken.has(hexKey(h)) && hexDistance(h, traveler) >= 3,
    );
    const start = rng.pick(open) ?? allHexes.find((h) => !walls.has(hexKey(h)) && !taken.has(hexKey(h)));
    if (!start) continue;
    taken.add(hexKey(start));
    const [kind, pattern] = enemyArchetype(i, levelIndex);
    let route: Hex[] = [];
    if (pattern === "PATROL") {
      const ring = neighbors(start).filter(
        (h) => allHexes.some((a) => hexEq(a, h)) && !walls.has(hexKey(h)),
      );
      route = ring.length >= 2 ? [start, ...ring.slice(0, 3)] : [start];
    }
    enemies.push({ id: `enemy-${i}`, kind, position: start, pattern, stunnedTurns: 0, patrolRoute: route, patrolIndex: 0 });
  }

  const terrain = new Map<string, Terrain>();
  for (const h of disc(radius)) {
    const k = hexKey(h);
    terrain.set(k, hexEq(h, goal) ? "GOAL" : walls.has(k) ? "WALL" : "FLOOR");
  }

  return {
    board: { terrain },
    player, traveler, enemies, goal,
    seed, rngState: rng.state,
    level: levelIndex, turn: 0, status: "PLAYING",
    travelerShield: 0, abilities: defaultAbilities(),
    score: startingScore, lastPlayerStep: null,
  };
}

// --- Run (mirrors RunEngine) -----------------------------------------------

export interface RunResult {
  status: "RUNNING" | "COMPLETED" | "FAILED";
  reachedLevel: number;
  score: number;
}

export function replayRun(seed: bigint, intents: Intent[]): RunResult {
  let current = generateLevel(seed, 1, 0);
  let levelIndex = 1;
  let status: RunResult["status"] = "RUNNING";

  for (const intent of intents) {
    if (status !== "RUNNING") break;
    const next = reduce(current, intent);
    if (next.status === "PLAYING") {
      current = next;
    } else if (next.status === "LOST") {
      current = next;
      status = "FAILED";
    } else {
      // WON
      if (levelIndex >= Balance.LEVELS_PER_RUN) {
        current = next;
        status = "COMPLETED";
      } else {
        levelIndex += 1;
        current = generateLevel(seed, levelIndex, next.score);
      }
    }
  }
  return { status, reachedLevel: levelIndex, score: current.score };
}

// --- Intent codec (mirrors IntentCodec) ------------------------------------

export function decodeIntents(encoded: string): Intent[] {
  if (!encoded || encoded.trim() === "") return [];
  return encoded.split(";").map((token): Intent => {
    const head = token[0];
    if (head === "W") return { kind: "WAIT" };
    if (head === "M") {
      const [q, r] = token.slice(1).split(",").map(Number);
      return { kind: "MOVE", target: { q, r } };
    }
    if (head === "A") {
      const parts = token.slice(1).split(",");
      const id = parts[0] as AbilityId;
      const target = parts.length >= 3 ? { q: Number(parts[1]), r: Number(parts[2]) } : null;
      return { kind: "ABILITY", id, target };
    }
    throw new Error(`Unknown intent token: ${token}`);
  });
}

// --- Daily seed (mirrors DailySeed) ----------------------------------------

const FNV_OFFSET = 0xcbf29ce484222325n;
const FNV_PRIME = 0x100000001b3n;

export function seedForDate(dateUtc: string): bigint {
  let hash = FNV_OFFSET;
  for (const byte of new TextEncoder().encode(dateUtc)) {
    hash = u64(hash ^ BigInt(byte));
    hash = u64(hash * FNV_PRIME);
  }
  return hash;
}
