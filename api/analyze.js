// Vercel 서버리스 함수: POST /api/analyze
// API 키는 Vercel 환경변수 OPENROUTER_API_KEY에 저장한다 (브라우저로는 절대 보내지 않음).
import { handleAnalyze } from '../src/analyze.js';
import { createAiAnalyzerFromEnv } from '../src/ai.js';

const aiAnalyze = createAiAnalyzerFromEnv();

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: '잘못된 요청이에요.' });
  }
  let body = null;
  try {
    body = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;
  } catch {
    // 아래에서 400으로 처리
  }
  const ip = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim();
  try {
    const { status, json } = await handleAnalyze(body, { aiAnalyze, ip });
    res.status(status).json(json);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: '잠시 후 다시 시도해 주세요.' });
  }
}
