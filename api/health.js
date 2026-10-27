// Vercel 서버리스 함수: GET /api/health — AI 판별이 켜져 있는지 확인용
import { DEFAULT_MODEL } from '../src/ai.js';

export default function handler(req, res) {
  res.status(200).json({
    ok: true,
    ai: Boolean(process.env.OPENROUTER_API_KEY) && process.env.AI_DISABLED !== '1',
    model: process.env.OPENROUTER_MODEL || DEFAULT_MODEL,
  });
}
