import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { buildFixtures, FIXTURE_PATH } from '../android/tools/rules-fixtures.mjs';

// 안드로이드 Kotlin 규칙은 이 예문 파일로 rules.js와 같은 결과를 내는지 확인한다.
test('안드로이드 비교용 예문이 rules.js 결과와 같다 (다르면 node android/tools/rules-fixtures.mjs 실행)', () => {
  const saved = JSON.parse(readFileSync(FIXTURE_PATH, 'utf8'));
  assert.deepEqual(saved, buildFixtures());
});
