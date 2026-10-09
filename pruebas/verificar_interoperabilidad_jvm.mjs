// C02: verifica en Node firmas nuevas generadas por los tests Kotlin/JVM.
// No es un relay. No usa claves de una familia ni contiene claves privadas.
import { readFile } from 'node:fs/promises';
import { createPublicKey, createHash, verify } from 'node:crypto';
import assert from 'node:assert/strict';

const ruta = new URL('../android/core-protocol/build/interoperabilidad/jvm-a-node.json', import.meta.url);
const datos = JSON.parse(await readFile(ruta, 'utf8'));
assert.equal(datos.vectors.length, 10);
const roles = {
  AUTH_PROOF: null, SYNC_REQUEST: 'PARENT', SET_POLICY: 'PARENT',
  ACK: 'CHILD', STATE: 'CHILD', PAIR_ACCEPT: 'PARENT',
  OFFLINE_CHALLENGE: 'CHILD', OFFLINE_RESPONSE: 'PARENT',
};
let comprobaciones = 0;
for (const vector of datos.vectors) {
  const sobre = vector.envelope;
  const publica = createPublicKey({ key: Buffer.from(vector.publicKeyB64, 'base64url'), format: 'der', type: 'spki' });
  assert.equal(publica.asymmetricKeyType, 'ec');
  assert.equal(publica.asymmetricKeyDetails.namedCurve, 'prime256v1');
  const payload = Buffer.from(sobre.payloadB64, 'base64url');
  const firma = Buffer.from(sobre.sigB64, 'base64url');
  const bytes = Buffer.concat([Buffer.from('CPv1/' + sobre.purpose + '\n', 'ascii'), payload]);
  assert.equal(verify('sha256', bytes, { key: publica, dsaEncoding: 'der' }, firma), true);
  assert.equal(createHash('sha256').update(payload).digest('base64url'), vector.payloadSha256B64);
  const mensaje = JSON.parse(payload.toString('utf8'));
  assert.ok(Object.hasOwn(roles, mensaje.kind));
  assert.equal(vector.signerRole, roles[mensaje.kind] ?? mensaje.role);
  const alterado = Buffer.from(bytes);
  alterado[alterado.length - 1] ^= 1;
  assert.equal(verify('sha256', alterado, { key: publica, dsaEncoding: 'der' }, firma), false);
  comprobaciones += 7;
}
console.log(`OK: 10 sobres nuevos JVM aceptados en Node; ${comprobaciones} comprobaciones. Runtime: ${process.version}.`);
console.log('Solo prueba interoperabilidad criptográfica; no acredita autorización ni bloqueo en Android.');
