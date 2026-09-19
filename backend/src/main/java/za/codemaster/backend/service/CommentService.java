package za.codemaster.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Comment;
import za.codemaster.backend.exception.CommentValidationException;
import za.codemaster.backend.repository.CommentRepository;

/**
 * Service orchestrating comment persistence with application-level target validation.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    /**
     * Persists a comment after enforcing that exactly one of Project or Issue is assigned.
     * Clients passing both or neither receive a 400 Bad Request rather than an internal DB error.
     *
     * @param comment the comment entity to persist
     * @return the persisted comment
     * @throws CommentValidationException if both or neither parent associations are provided
     */
    @Transactional
    public Comment createComment(Comment comment) {
        boolean hasProject = comment.getProject() != null && comment.getProject().getId() != null;
        boolean hasIssue = comment.getIssue() != null && comment.getIssue().getId() != null;

        if (hasProject && hasIssue) {
            throw new CommentValidationException("A comment cannot be attached to both a project and an issue.");
        }
        if (!hasProject && !hasIssue) {
            throw new CommentValidationException("A comment must be attached to either a project or an issue.");
        }

        return commentRepository.save(comment);
    }
}