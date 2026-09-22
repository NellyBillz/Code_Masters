package za.codemaster.backend.exception;

/** Raised when the GitHub OAuth client credentials have not been configured. */
public class OAuthNotConfiguredException extends IllegalStateException {

    public OAuthNotConfiguredException() {
        super("GitHub OAuth is not configured. Set GITHUB_OAUTH_CLIENT_ID and "
                + "GITHUB_OAUTH_CLIENT_SECRET in backend/.env; see backend/AUTH.md.");
    }
}
