import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from '../server.js';
import { allowAiCall, handleAnalyze } from '../src/analyze.js';
import vercelAnalyze from '../api/analyze.js';

let server;
let base;
const aiCalls = [];

before(async () => {
  server = createServer({
    aiAnalyze: async (text, mode) => {
      aiCalls.push({ text, mode });
      return null; // AI 실패 상황: 규칙 판정만 써야 한다
    },
  });
  await new Promise((resolve) => server.listen(0, resolve));
  base = `http://localhost:${server.address().port}`;
});

after(() => server.close());

const post = (body) =>
  fetch(`${base}/api/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });

test('사기 문자를 보내면 위험 판정을 돌려준다', async () => {
  const res = await post({ text: '안전계좌로 이체하셔야 합니다. 금감원 수사관입니다', mode: 'scam' });
  assert.equal(res.status, 200);
  const v = await res.json();
  assert.equal(v.level, 'danger');
  assert.equal(v.usedAI, false);
  assert.equal(aiCalls.at(-1).mode, 'scam');
});

test('알 수 없는 mode는 scam으로 처리한다', async () => {
  const v = await (await post({ text: '안녕하세요', mode: 'weird' })).json();
  assert.equal(v.mode, 'scam');
});

test('빈 글과 잘못된 요청은 400', async () => {
  assert.equal((await post({ text: '   ' })).status, 400);
  assert.equal((await post('{not json')).status, 400);
});

test('정적 파일을 제공하고 상위 폴더 접근은 막는다', async () => {
  const home = await fetch(`${base}/`);
  assert.equal(home.status, 200);
  assert.match(await home.text(), /진실판별기/);
  const manifest = await (await fetch(`${base}/manifest.webmanifest`)).json();
  assert.equal(manifest.name, '진실판별기');
  assert.equal(manifest.share_target.method, 'GET');
  const traversal = await fetch(`${base}/..%2fpackage.json`);
  assert.notEqual(traversal.status, 200);
});

test('같은 IP가 1분에 너무 많이 부르면 AI 없이 규칙 판별만 한다', async () => {
  const now = Date.now();
  for (let i = 0; i < 10; i++) assert.equal(allowAiCall('1.1.1.1', now), true);
  assert.equal(allowAiCall('1.1.1.1', now), false);
  assert.equal(allowAiCall('2.2.2.2', now), true);
  assert.equal(allowAiCall('1.1.1.1', now + 61_000), true);

  let called = false;
  const { status, json } = await handleAnalyze(
    { text: '안녕하세요' },
    { ip: '1.1.1.1', aiAnalyze: async () => ((called = true), null) },
  );
  assert.equal(status, 200);
  assert.equal(json.level, 'safe');
  assert.equal(called, true, '한도가 풀린 뒤에는 다시 AI를 부른다');
});

function mockRes() {
  const res = { statusCode: 0, headers: {}, body: undefined };
  res.status = (code) => ((res.statusCode = code), res);
  res.json = (data) => ((res.body = data), res);
  res.setHeader = (k, v) => (res.headers[k] = v);
  return res;
}

test('Vercel 함수: POST만 받고, 판정을 JSON으로 돌려준다', async () => {
  const get = mockRes();
  await vercelAnalyze({ method: 'GET', headers: {} }, get);
  assert.equal(get.statusCode, 405);

  const ok = mockRes();
  await vercelAnalyze(
    { method: 'POST', headers: { 'x-forwarded-for': '9.9.9.9' }, body: { text: '모바일 부고장 han.gl/abc', mode: 'share' } },
    ok,
  );
  assert.equal(ok.statusCode, 200);
  assert.equal(ok.body.level, 'danger');
  assert.equal(ok.body.category, 'scam');

  const bad = mockRes();
  await vercelAnalyze({ method: 'POST', headers: {}, body: '{oops' }, bad);
  assert.equal(bad.statusCode, 400);
});
