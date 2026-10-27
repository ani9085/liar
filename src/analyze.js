// /api/analyze 요청 처리. Vercel 함수(api/analyze.js)와 로컬 서버(server.js)가 같이 쓴다.
import { analyzeText, buildVerdict } from '../public/js/rules.js';

export const MAX_TEXT = 5000;
const AI_LIMIT_PER_MINUTE = 10;

// IP별 AI 호출 횟수 제한 (서버 인스턴스마다 따로 센다).
// 한도를 넘으면 오류 대신 규칙 판별 결과만 돌려준다.
const recentCalls = new Map();

export function allowAiCall(ip, now = Date.now()) {
  if (!ip) return true;
  const calls = (recentCalls.get(ip) || []).filter((t) => now - t < 60_000);
  if (calls.length >= AI_LIMIT_PER_MINUTE) {
    recentCalls.set(ip, calls);
    return false;
  }
  calls.push(now);
  recentCalls.set(ip, calls);
  if (recentCalls.size > 10_000) recentCalls.clear();
  return true;
}

export async function handleAnalyze(body, { aiAnalyze = null, ip = '' } = {}) {
  const text = body?.text;
  if (typeof text !== 'string' || !text.trim()) {
    return { status: 400, json: { error: '확인할 내용을 입력해 주세요.' } };
  }
  const input = text.slice(0, MAX_TEXT);
  const mode = body.mode === 'share' ? 'share' : 'scam';
  const rules = analyzeText(input);
  const ai = aiAnalyze && allowAiCall(ip) ? await aiAnalyze(input, mode) : null;
  return { status: 200, json: buildVerdict(mode, rules, ai) };
}
