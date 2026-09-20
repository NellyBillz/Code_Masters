package za.codemaster.testsupport;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.security.AuthenticatedUser;

/**
 * A throwaway endpoint for API-02.2's security integration test — see that ticket's
 * acceptance criteria ("test against a throwaway endpoint if every real write endpoint
 * is still pending"). Deliberately lives outside {@code za.codemaster.backend}'s
 * component-scan tree and is registered only via explicit {@code @Import} in the test
 * that needs it, so it never appears in the real application context.
 */
@RestController
public class ThrowawayWriteController {

    public static final String PATH = "/api/v1/_test/throwaway";

    @PostMapping(PATH)
    public ResponseEntity<Void> write(@AuthenticatedUser User currentUser) {
        return ResponseEntity.ok().build();
    }

    @GetMapping(PATH)
    public ResponseEntity<Void> read() {
        return ResponseEntity.ok().build();
    }
}
