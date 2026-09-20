package za.codemaster.backend.dto.project;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /projects/{projectId}/maintainers}.
 * <p>
 * Matches the {@code AddMaintainerRequest} schema in codemasters-api-spec.yaml v2.1.
 * {@code role} defaults to {@code maintainer} when not provided — applied in
 * {@code MaintainerService.inviteMaintainer}, not here, since a record component
 * can't carry a runtime default the way the spec's {@code default: maintainer} does.
 */
public record AddMaintainerRequest(
        @NotBlank(message = "username is required")
        String username,

        MaintainerRole role
) {
}
