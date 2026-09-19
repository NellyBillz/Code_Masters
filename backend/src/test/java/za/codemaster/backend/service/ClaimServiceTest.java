package za.codemaster.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.Claim;
import za.codemaster.backend.dto.ClaimStatus;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.5's service-layer behavior (round2-tickets.md): claims can be
 * created, released, and listed, non-exclusivity across different users holds
 * (design doc §7 — the whole point of the ticket), and a second active claim by
 * the same user is rejected with {@code CLAIM_ALREADY_ACTIVE} (409), caught from
 * the real partial unique index rather than pre-checked.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code CommentServiceTest}.
 * <p>
 * Note on the duplicate-claim test: once {@link ClaimService#createClaim}'s
 * {@code saveAndFlush} fails on the partial unique index, PostgreSQL aborts the
 * current transaction — any further statement on that same connection fails until
 * rollback. Since this whole test class shares one transaction per test method
 * (rolled back at the end for isolation), the duplicate-claim assertion is kept
 * as the last thing each such test does, with no further DB access afterward.
 */
@SpringBootTest(
    classes = BackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    OAuth2ClientAutoConfiguration.class
})
@Transactional
class ClaimServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    private ClaimService service;
    private ProjectQueryServiceFixtures fixtures;
    private User claimant;

    @BeforeEach
    void setUp() {
        service = new ClaimService(claimRepository, issueRepository);
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        claimant = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("claimant_" + System.nanoTime())
                .displayName("Claimant")
                .build());
    }

    private User otherUser() {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("other_" + System.nanoTime())
                .displayName("Other User")
                .build());
    }

    @Test
    void createClaimPersistsAndReturnsActiveStatus() {
        Long issueId = fixtures.issueId(0);

        Claim created = service.createClaim(issueId, "I'll take this one.", claimant);

        assertNotNull(created.id());
        assertEquals(issueId, created.issueId());
        assertEquals(ClaimStatus.ACTIVE, created.status());
        assertEquals("I'll take this one.", created.note());
        assertEquals(claimant.getUsername(), created.user().username());
    }

    @Test
    void createClaimWithoutNoteSucceeds() {
        Long issueId = fixtures.issueId(0);

        Claim created = service.createClaim(issueId, null, claimant);

        assertNull(created.note());
    }

    @Test
    void createClaimOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createClaim(-999L, null, claimant));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void twoDifferentUsersCanBothHoldActiveClaimsOnSameIssue() {
        Long issueId = fixtures.issueId(0);
        User userA = claimant;
        User userB = otherUser();

        service.createClaim(issueId, "A is on it", userA);
        service.createClaim(issueId, "B is on it too", userB);

        List<Claim> claims = service.getActiveClaims(issueId);

        assertEquals(2, claims.size(),
                "claims are a non-exclusive signal of interest (design doc §7) — both must be active");
        assertTrue(claims.stream().anyMatch(c -> c.user().username().equals(userA.getUsername())));
        assertTrue(claims.stream().anyMatch(c -> c.user().username().equals(userB.getUsername())));
    }

    @Test
    void activeClaimsAreListedOldestFirst() {
        Long issueId = fixtures.issueId(0);
        User userA = claimant;
        User userB = otherUser();

        Claim first = service.createClaim(issueId, null, userA);
        Claim second = service.createClaim(issueId, null, userB);

        List<Claim> claims = service.getActiveClaims(issueId);

        assertEquals(List.of(first.id(), second.id()), claims.stream().map(Claim::id).toList());
    }

    @Test
    void getActiveClaimsOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> service.getActiveClaims(-999L));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
    }

    @Test
    void releaseClaimMarksItReleasedAndRemovesItFromActiveList() {
        Long issueId = fixtures.issueId(0);
        service.createClaim(issueId, null, claimant);

        service.releaseClaim(issueId, claimant);

        List<Claim> claims = service.getActiveClaims(issueId);
        assertTrue(claims.isEmpty(), "a released claim must not appear in the active claims list");
    }

    @Test
    void releasingWithNoActiveClaimThrowsClaimNotFound() {
        Long issueId = fixtures.issueId(0);

        ApiException ex = assertThrows(ApiException.class, () -> service.releaseClaim(issueId, claimant));

        assertEquals("CLAIM_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void releasingOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> service.releaseClaim(-999L, claimant));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
    }

    @Test
    void afterReleaseTheSameUserCanClaimAgain() {
        Long issueId = fixtures.issueId(0);
        service.createClaim(issueId, "first attempt", claimant);
        service.releaseClaim(issueId, claimant);

        Claim reclaimed = service.createClaim(issueId, "second attempt", claimant);

        assertEquals(ClaimStatus.ACTIVE, reclaimed.status());
        assertEquals(1, service.getActiveClaims(issueId).size());
    }

    @Test
    void secondActiveClaimBySameUserOnSameIssueThrows409() {
        Long issueId = fixtures.issueId(0);
        service.createClaim(issueId, "first", claimant);

        // Kept as the last action in this test — see class Javadoc note on
        // why nothing touches the DB again after a caught constraint violation.
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createClaim(issueId, "duplicate", claimant));

        assertEquals("CLAIM_ALREADY_ACTIVE", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }
}
