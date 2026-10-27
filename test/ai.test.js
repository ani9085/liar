import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createAiAnalyzer, createAiAnalyzerFromEnv, parseJson } from '../src/ai.js';

const VERDICT = {
  scam_level: 'danger',
  misinfo_level: 'not_applicable',
  headline: '사기 문자예요',
  reasons: ['안전계좌는 사기예요.'],
  advice: '전화하지 마세요.',
  fact_check: '',
};

function fakeFetch(status, data) {
  const calls = [];
  const impl = async (url, init) => {
    calls.push({ url, headers: init.headers, body: JSON.parse(init.body) });
    return new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } });
  };
  return { impl, calls };
}

const completion = (content, finish_reason = 'stop') => ({ choices: [{ message: { content }, finish_reason }] });

test('OpenRouter에 키·모델·JSON 형식을 담아 요청하고, 개인정보는 가린다', async () => {
  const { impl, calls } = fakeFetch(200, completion(JSON.stringify(VERDICT)));
  const analyze = createAiAnalyzer({ apiKey: 'sk-or-test', model: 'test/model', siteUrl: 'https://x.vercel.app', fetchImpl: impl });
  const result = await analyze('주민번호 800101-1234567 안전계좌로 보내세요', 'scam');

  assert.deepEqual(result, VERDICT);
  const [call] = calls;
  assert.equal(call.url, 'https://openrouter.ai/api/v1/chat/completions');
  assert.equal(call.headers.Authorization, 'Bearer sk-or-test');
  assert.equal(call.headers['HTTP-Referer'], 'https://x.vercel.app');
  assert.match(call.headers['X-Title'], /^[\x20-\x7e]+$/, '헤더는 영문(ASCII)만 가능');
  assert.equal(call.body.model, 'test/model');
  assert.equal(call.body.response_format.type, 'json_schema');
  assert.equal(call.body.messages[0].role, 'system');
  assert.match(call.body.messages[1].content, /\[주민번호\]/);
  assert.doesNotMatch(call.body.messages[1].content, /1234567/);
});

test('코드 블록으로 감싼 JSON도 읽는다', () => {
  assert.deepEqual(parseJson('```json\n{"a":1}\n```'), { a: 1 });
  assert.equal(parseJson('JSON 아님'), null);
  assert.equal(parseJson(undefined), null);
});

test('오류·잘린 답·이상한 형식이면 null (규칙 판별로 대체)', async () => {
  const cases = [
    fakeFetch(401, { error: { message: 'No auth credentials found' } }),
    fakeFetch(200, completion(JSON.stringify(VERDICT), 'length')),
    fakeFetch(200, completion('{"scam_level":"매우위험"}')),
    fakeFetch(200, { error: { message: 'provider error' } }),
  ];
  for (const { impl } of cases) {
    const analyze = createAiAnalyzer({ apiKey: 'k', fetchImpl: impl });
    assert.equal(await analyze('안녕', 'scam'), null);
  }
});

test('응답이 늦으면 시간 초과로 null', async () => {
  // 실제 fetch는 소켓이 이벤트 루프를 붙잡지만, 가짜 fetch는 타이머로 대신 붙잡는다.
  const slow = (url, init) =>
    new Promise((_, reject) => {
      const keepAlive = setTimeout(() => {}, 5_000);
      init.signal.addEventListener('abort', () => {
        clearTimeout(keepAlive);
        reject(init.signal.reason);
      });
    });
  const analyze = createAiAnalyzer({ apiKey: 'k', fetchImpl: slow, timeoutMs: 50 });
  assert.equal(await analyze('안녕', 'scam'), null);
});

test('키가 없거나 AI_DISABLED=1이면 AI를 쓰지 않는다', () => {
  assert.equal(createAiAnalyzerFromEnv({}), null);
  assert.equal(createAiAnalyzerFromEnv({ OPENROUTER_API_KEY: 'k', AI_DISABLED: '1' }), null);
  assert.equal(typeof createAiAnalyzerFromEnv({ OPENROUTER_API_KEY: 'k' }), 'function');
});
