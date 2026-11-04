# Anonymousnet Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Authentication Gate**: No unauthenticated user (`request.auth == null`) may read or write any collection in Firestore.
2. **Private User Profile Isolation (`/users/{userId}`)**: Only `request.auth.uid == userId` may read, create, update, or delete their private user profile and subcollections (`saved`, `liked`, `blocks`, `notifications`).
3. **Public Pseudonymous Profile (`/publicProfiles/{userId}`)**: Never stores email, legal name, or phone number. Only `request.auth.uid == userId` can create, update, or delete their own public profile.
4. **Confession Posts (`/posts/{postId}`)**:
   - `authorUid` on create must equal `request.auth.uid` and is immutable on update.
   - `createdAt` must be a valid server timestamp (`<= request.time`) and is immutable on update.
   - Public list/get is restricted to documents where `status == 'published'` or `authorUid == request.auth.uid`.
   - Authors can update post content/status; other authenticated users may only update engagement counters (`likeCount`, `commentCount`, `viewCount`, `updatedAt`) on published posts.
5. **Comments (`/posts/{postId}/comments/{commentId}`)**:
   - `authorUid` must equal `request.auth.uid`.
   - List/get requires `status == 'published'` or `authorUid == request.auth.uid`.
6. **Likes (`/posts/{postId}/likes/{userId}`)**:
   - Keyed by `userId` for idempotent 1-like-per-user-per-post invariant; only `request.auth.uid == userId` may read, create, or delete.
7. **Reports (`/reports/{reportId}`)**:
   - Write-only creation by authenticated `reporterUid == request.auth.uid` with valid category and bounded details; only the reporter may read their own report. No updates or deletes allowed by clients.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Feed Read**: `unauthDb.collection("posts").where("status", "==", "published").get()` -> Reject
2. **Unfiltered Post List**: `aliceDb.collection("posts").get()` (when draft/under_review posts of others exist) -> Reject
3. **Cross-User Private Profile Read**: `bobDb.collection("users").doc(ALICE_UID).get()` -> Reject
4. **Identity Spoofing on Post Create**: `aliceDb.collection("posts").doc("post_1").set({ authorUid: BOB_UID, ... })` -> Reject
5. **Shadow Field Injection on Post Create**: `aliceDb.collection("posts").doc("post_1").set({ ..., isAdminOverride: true })` -> Reject
6. **Oversized Confession Text (> 2000 chars)**: `aliceDb.collection("posts").doc("post_1").set({ text: "a".repeat(2500), ... })` -> Reject
7. **Immutable Field Tampering (`createdAt` / `authorUid`)**: `aliceDb.collection("posts").doc("post_1").update({ authorUid: BOB_UID })` -> Reject
8. **Cross-User Post Content Edit**: `bobDb.collection("posts").doc(alicePostId).update({ text: "Hacked" })` -> Reject
9. **Cross-User Saved Subcollection Read**: `bobDb.collection("users").doc(ALICE_UID).collection("saved").get()` -> Reject
10. **Cross-User Like Impersonation**: `bobDb.collection("posts").doc(postId).collection("likes").doc(ALICE_UID).set(...)` -> Reject
11. **Report Tampering / Deletion**: `aliceDb.collection("reports").doc(reportId).delete()` -> Reject
12. **Future Timestamp Spoofing**: `aliceDb.collection("posts").doc("post_1").set({ createdAt: futureTimestamp, ... })` -> Reject
