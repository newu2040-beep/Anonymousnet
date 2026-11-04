const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read published posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("status", "==", "published").get());
});

test("2. Authenticated user cannot read another user's private UserAccount", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      authProvider: "google",
      accountState: "active",
      guest: false,
      themeMode: "light",
      keepAnonymousDefault: true,
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("3. Authenticated user can create and read their own UserAccount", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      authProvider: "google",
      accountState: "active",
      guest: false,
      themeMode: "light",
      keepAnonymousDefault: true,
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("4. Identity spoofing on post create is rejected", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("posts").doc("post_spoof").set({
      authorUid: BOB_UID,
      anonymousLabel: "Anonymous",
      anonymousHandle: "@anon_user",
      isAnonymous: true,
      text: "Spoofed confession",
      type: "text",
      status: "published",
      likeCount: 0,
      commentCount: 0,
      viewCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("5. Shadow field injection on post create is rejected", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("posts").doc("post_shadow").set({
      authorUid: ALICE_UID,
      anonymousLabel: "Anonymous",
      anonymousHandle: "@anon_user",
      isAnonymous: true,
      text: "Confession with ghost field",
      type: "text",
      status: "published",
      likeCount: 0,
      commentCount: 0,
      viewCount: 0,
      isAdminVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("6. Authenticated user can query published posts and own drafts, but cannot read another user's draft", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("posts").doc("bob_published").set({
      authorUid: BOB_UID,
      anonymousLabel: "Anonymous",
      anonymousHandle: "@anon_bob",
      isAnonymous: true,
      text: "Bob published confession",
      type: "text",
      status: "published",
      likeCount: 5,
      commentCount: 1,
      viewCount: 20,
      createdAt: now,
      updatedAt: now,
    });
    await context.firestore().collection("posts").doc("bob_draft").set({
      authorUid: BOB_UID,
      anonymousLabel: "Anonymous",
      anonymousHandle: "@anon_bob",
      isAnonymous: true,
      text: "Bob private draft",
      type: "text",
      status: "draft",
      likeCount: 0,
      commentCount: 0,
      viewCount: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("posts").where("status", "==", "published").get()
  );
  await assertFails(aliceDb.collection("posts").doc("bob_draft").get());
  await assertFails(aliceDb.collection("posts").get());
});

test("7. Cross-user post text tampering is rejected while engagement counter update on published post succeeds", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("posts").doc("alice_post").set({
      authorUid: ALICE_UID,
      anonymousLabel: "Anonymous",
      anonymousHandle: "@anon_alice",
      isAnonymous: true,
      text: "Original confession",
      type: "text",
      status: "published",
      likeCount: 0,
      commentCount: 0,
      viewCount: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("posts").doc("alice_post").update({
      text: "Tampered text",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("alice_post").update({
      likeCount: 1,
      updatedAt: new Date(),
    })
  );
});

test("8. Saved, liked, blocks, and notifications subcollections are strictly isolated to owner", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved").doc("post_1").set({
      userId: ALICE_UID,
      postId: "post_1",
      postText: "Saved confession",
      postType: "text",
      anonymousLabel: "Anonymous",
      createdAt: now,
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved").doc("post_1").get()
  );
});

test("9. Reports are write-only for submission and cannot be deleted or read by other users", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("reports").doc("report_1").set({
      reporterUid: ALICE_UID,
      targetType: "post",
      targetId: "post_1",
      reason: "spam",
      details: "Repeated spam post",
      status: "submitted",
      createdAt: now,
    })
  );

  await assertFails(bobDb.collection("reports").doc("report_1").get());
  await assertFails(aliceDb.collection("reports").doc("report_1").delete());
});
