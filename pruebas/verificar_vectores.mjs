// Verificador de fixtures documentales CP/1. No es el relay ni un SDK de producción.
// Ejecutar desde cualquier carpeta: node /ruta/al/paquete/pruebas/verificar_vectores.mjs
// Solo usa módulos integrados. Objetivo del producto: Node 24 LTS.
import { readFile } from 'node:fs/promises';
import { createPublicKey, createHash, verify, sign, generateKeyPairSync } from 'node:crypto';
import assert from 'node:assert/strict';

const fixtures = JSON.parse(await readFile(new URL('./vectores_crypto.json', import.meta.url), 'utf8'));
let checks = 0;
function test(fn) { fn(); checks++; }
function decode64(value) {
  assert.equal(typeof value, 'string');
  assert.match(value, /^[A-Za-z0-9_-]+$/);
  const decoded = Buffer.from(value, 'base64url');
  assert.equal(decoded.toString('base64url'), value, 'Base64url no canónico');
  return decoded;
}
function canonical(value) {
  if (value === null || typeof value === 'boolean') return JSON.stringify(value);
  if (typeof value === 'number') {
    assert.ok(Number.isSafeInteger(value) && value >= 0 && !Object.is(value, -0));
    return String(value);
  }
  if (typeof value === 'string') {
    assert.match(value, /^[\x20-\x7e]*$/);
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  assert.ok(value && typeof value === 'object');
  return '{' + Object.keys(value).sort().map(k => canonical(k) + ':' + canonical(value[k])).join(',') + '}';
}
function publicKey(base64) {
  const key = createPublicKey({key: decode64(base64), format:'der', type:'spki'});
  assert.equal(key.asymmetricKeyType, 'ec');
  assert.equal(key.asymmetricKeyDetails?.namedCurve, 'prime256v1');
  return key;
}
function message(envelope) {
  return Buffer.concat([Buffer.from('CPv1/' + envelope.purpose + '\n', 'ascii'), decode64(envelope.payloadB64)]);
}
const keys = Object.fromEntries(Object.entries(fixtures.publicKeys).map(([role,key]) => [role,publicKey(key)]));
const accepted = {
  AUTH_PROOF:['AUTH', null], SYNC_REQUEST:['MESSAGE','PARENT'], SET_POLICY:['MESSAGE','PARENT'],
  ACK:['MESSAGE','CHILD'], STATE:['MESSAGE','CHILD'], PAIR_ACCEPT:['PAIR','PARENT'],
  OFFLINE_CHALLENGE:['OFFLINE','CHILD'], OFFLINE_RESPONSE:['OFFLINE','PARENT']
};
for (const vector of fixtures.vectors) {
  const e=vector.envelope;
  test(() => assert.ok(verify('sha256',message(e),{key:keys[vector.signerRole],dsaEncoding:'der'},decode64(e.sigB64))));
  test(() => assert.equal(decode64(e.payloadB64).toString('utf8'), canonical(vector.payload)));
  test(() => assert.equal(createHash('sha256').update(decode64(e.payloadB64)).digest('base64url'), vector.payloadSha256B64));
  test(() => {
    const binding=accepted[vector.payload.kind]; assert.ok(binding);
    assert.equal(e.purpose,binding[0]);
    assert.equal(vector.signerRole,binding[1] ?? vector.payload.role);
  });
  test(() => assert.equal(verify('sha256',message(e),{key:keys[vector.signerRole==='PARENT'?'CHILD':'PARENT'],dsaEncoding:'der'},decode64(e.sigB64)),false));
  test(() => {
    const wrong={...e,purpose:e.purpose==='AUTH'?'MESSAGE':'AUTH'};
    assert.equal(verify('sha256',message(wrong),{key:keys[vector.signerRole],dsaEncoding:'der'},decode64(e.sigB64)),false);
  });
  test(() => {
    const bytes=decode64(e.payloadB64); bytes[bytes.length-1]^=1;
    assert.equal(verify('sha256',message({...e,payloadB64:bytes.toString('base64url')}),{key:keys[vector.signerRole],dsaEncoding:'der'},decode64(e.sigB64)),false);
  });
}
// Una firma correcta no vuelve canónico un JSON ambiguo.
const ephemeral=generateKeyPairSync('ec',{namedCurve:'prime256v1'});
for (const text of ['{"v":1,"v":2}', '{ "v":1}', '{"v":1e0}', '{"s":"\\u0041"}']) {
  test(() => {
    const bytes=Buffer.from(text,'utf8'), data=Buffer.concat([Buffer.from('CPv1/MESSAGE\n'),bytes]);
    const sig=sign('sha256',data,{key:ephemeral.privateKey,dsaEncoding:'der'});
    assert.ok(verify('sha256',data,{key:ephemeral.publicKey,dsaEncoding:'der'},sig));
    assert.notEqual(canonical(JSON.parse(text)),text,'El JSON ambiguo debe rechazarse tras verificar firma');
  });
}
test(() => assert.throws(() => decode64('YQ==')));
test(() => assert.throws(() => decode64('YR')));
test(() => assert.throws(() => canonical(1.25)));
test(() => assert.throws(() => canonical(Number.MAX_SAFE_INTEGER+1)));
console.log(`OK: ${fixtures.vectors.length} vectores firmados; ${checks} comprobaciones documentales. Runtime: ${process.version}.`);
console.log('Esto no prueba Device Owner, Android Keystore, temporizadores, red real, emergencias ni el A13.');
