import http from 'node:http';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { analyzeText, buildVerdict } from './public/js/rules.js';

const here = path.dirname(fileURLToPath(import.meta.url));
const PUBLIC_DIR = path.join(here, 'public');
const MAX_TEXT = 5000;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.webmanifest': 'application/manifest+json',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
};

function sendJson(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(body));
}

async function readBody(req, limit = 64 * 1024) {
  let size = 0;
  const chunks = [];
  for await (const chunk of req) {
    size += chunk.length;
    if (size > limit) throw Object.assign(new Error('too large'), { status: 413 });
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString('utf8');
}

async function serveStatic(req, res) {
  const { pathname } = new URL(req.url, 'http://localhost');
  const filePath = path.normalize(path.join(PUBLIC_DIR, pathname === '/' ? 'index.html' : pathname));
  if (!filePath.startsWith(PUBLIC_DIR + path.sep)) {
    res.writeHead(403).end();
    return;
  }
  try {
    const data = await readFile(filePath);
    res.writeHead(200, {
      'Content-Type': MIME[path.extname(filePath)] || 'application/octet-stream',
      'Cache-Control': 'no-cache',
    });
    res.end(data);
  } catch {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' }).end('찾을 수 없어요');
  }
}

export function createServer({ aiAnalyze = null } = {}) {
  return http.createServer(async (req, res) => {
    try {
      if (req.method === 'POST' && req.url === '/api/analyze') {
        const { text, mode } = JSON.parse(await readBody(req));
        if (typeof text !== 'string' || !text.trim()) {
          return sendJson(res, 400, { error: '확인할 내용을 입력해 주세요.' });
        }
        const input = text.slice(0, MAX_TEXT);
        const safeMode = mode === 'share' ? 'share' : 'scam';
        const rules = analyzeText(input);
        const ai = aiAnalyze ? await aiAnalyze(input, safeMode) : null;
        return sendJson(res, 200, buildVerdict(safeMode, rules, ai));
      }
      if (req.method === 'GET' && req.url === '/api/health') {
        return sendJson(res, 200, { ok: true, ai: Boolean(aiAnalyze) });
      }
      if (req.method === 'GET' || req.method === 'HEAD') {
        return await serveStatic(req, res);
      }
      res.writeHead(405).end();
    } catch (error) {
      if (error instanceof SyntaxError) return sendJson(res, 400, { error: '잘못된 요청이에요.' });
      if (error.status === 413) return sendJson(res, 413, { error: '글이 너무 길어요.' });
      console.error(error);
      sendJson(res, 500, { error: '잠시 후 다시 시도해 주세요.' });
    }
  });
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const hasKey = Boolean(process.env.ANTHROPIC_API_KEY || process.env.ANTHROPIC_AUTH_TOKEN);
  const useAI = process.env.AI_DISABLED !== '1' && hasKey;
  const aiAnalyze = useAI ? (await import('./src/ai.js')).createAiAnalyzer() : null;
  const port = Number(process.env.PORT) || 3000;
  createServer({ aiAnalyze }).listen(port, () => {
    console.log(`진실판별기 실행 중: http://localhost:${port}`);
    console.log(useAI ? 'AI 판별: 켜짐' : 'AI 판별: 꺼짐 (ANTHROPIC_API_KEY가 없어 규칙 판별만 사용)');
  });
}
