import { before, after, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, deleteDoc, updateDoc, collection, getDocs } from 'firebase/firestore';
let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-nexus-rules',
    firestore: { host: '127.0.0.1', port: 8088,
      rules: await readFile(new URL('../firestore.rules', import.meta.url), 'utf8') }
  });
  await env.withSecurityRulesDisabled(async ctx => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), { backupVersion: 2, cash: 5000 });
  });
});
after(async () => { if (env) await env.cleanup(); });
const own = () => env.authenticatedContext('alice').firestore();
const other = () => env.authenticatedContext('bob').firestore();
const guest = () => env.unauthenticatedContext().firestore();
test('owner can read backup', async () => { await assertSucceeds(getDoc(doc(own(), 'users/alice'))); });
test('owner can update backup', async () => { await assertSucceeds(updateDoc(doc(own(), 'users/alice'), { cash: 6000 })); });
test('new owner can create and delete own backup', async () => {
  const db = env.authenticatedContext('new-player').firestore();
  await assertSucceeds(setDoc(doc(db, 'users/new-player'), { backupVersion: 2 }));
  await assertSucceeds(deleteDoc(doc(db, 'users/new-player')));
});
test('unauthenticated read is denied', async () => { await assertFails(getDoc(doc(guest(), 'users/alice'))); });
test('unauthenticated write is denied', async () => { await assertFails(setDoc(doc(guest(), 'users/alice'), { cash: 0 })); });
test('another player cannot read backup', async () => { await assertFails(getDoc(doc(other(), 'users/alice'))); });
test('another player cannot overwrite backup', async () => { await assertFails(setDoc(doc(other(), 'users/alice'), { cash: 0 })); });
test('another player cannot delete backup', async () => { await assertFails(deleteDoc(doc(other(), 'users/alice'))); });
test('listing players is denied', async () => { await assertFails(getDocs(collection(own(), 'users'))); });
test('unrelated paths and nested documents are denied', async () => {
  await assertFails(setDoc(doc(own(), 'admin/settings'), { admin: true }));
  await assertFails(setDoc(doc(own(), 'users/alice/private/settings'), { admin: true }));
});
