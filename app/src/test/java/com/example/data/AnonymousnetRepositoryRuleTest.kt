package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class AnonymousnetRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserAndCreateConfession_authenticatedOwner_succeedsAndEmitsInFeed() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = AnonymousnetRepository(firestore)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserAndPublicProfile(guestMode = false)
        }
        assertTrue(profileResult.isSuccess)

        val uniqueDocId = "post_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost(
                text = "Sometimes I just want to run away from everything...",
                type = "text",
                isAnonymous = true,
                customDocId = uniqueDocId
            )
        }
        assertTrue(createResult.isSuccess)

        val emittedPosts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublishedPosts().first { list -> list.any { it.id == uniqueDocId } }
        }
        assertTrue(emittedPosts.any { it.id == uniqueDocId })
    }

    @Test
    fun getPrivateUserAccount_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = AnonymousnetRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserAndPublicProfile(guestMode = false).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun observePublishedPosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = AnonymousnetRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublishedPosts().first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun toggleSaveAndSubmitReport_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AnonymousnetRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserAndPublicProfile(guestMode = false).getOrThrow()
        }

        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPost(
                text = "Life is better with good coffee and good vibes.",
                type = "text",
                customDocId = postId
            ).getOrThrow()
        }

        val post = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getPostById(postId).getOrThrow()
        }
        assertNotNull(post)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleSave(post!!, isCurrentlySaved = false)
        }
        assertTrue(saveResult.isSuccess)

        val savedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSavedPosts(aliceUid).first { list -> list.any { it.postId == postId } }
        }
        assertTrue(savedList.any { it.postId == postId })

        val reportResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitReport(
                targetType = "post",
                targetId = postId,
                reason = "spam",
                details = "Automated test report"
            )
        }
        assertTrue(reportResult.isSuccess)
    }

    @Test
    fun customizeUsername_realtimeUniqueReservationAndCheck_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = AnonymousnetRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserAndPublicProfile(guestMode = false).getOrThrow()
        }

        val testUniqueHandle = "shadow_${UUID.randomUUID().toString().replace("-", "").take(6)}"

        // 1. Initially should be Available
        val initialCheck = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.checkUsernameAvailability(testUniqueHandle).getOrThrow()
        }
        assertTrue(initialCheck is UsernameAvailability.Available)

        // 2. Claim the unique username
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateUsernameAndDisplayName(
                newRawHandle = testUniqueHandle,
                newDisplayName = "Shadow Phantom"
            )
        }
        assertTrue(updateResult.isSuccess)

        // 3. For Alice, it is now her active handle
        val aliceCheck = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.checkUsernameAvailability(testUniqueHandle).getOrThrow()
        }
        assertTrue(aliceCheck is UsernameAvailability.IsCurrentUser)

        // 4. Bob signs in and checks the same handle -> it must be Taken!
        signInTestUser(BOB_EMAIL)
        val bobRepo = AnonymousnetRepository(firestore)
        val bobCheck = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.checkUsernameAvailability(testUniqueHandle).getOrThrow()
        }
        assertTrue(bobCheck is UsernameAvailability.Taken)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
