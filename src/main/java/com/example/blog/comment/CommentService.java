package com.example.blog.comment;

import java.util.List;

import com.example.blog.post.Post;
import com.example.blog.post.PostNotFoundException;
import com.example.blog.post.PostRepository;
import com.example.blog.user.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

	private static final int MAX_BODY_LENGTH = 2000;

	private final CommentRepository comments;
	private final PostRepository posts;

	public CommentService(CommentRepository comments, PostRepository posts) {
		this.comments = comments;
		this.posts = posts;
	}

	@Transactional
	public Comment addComment(Long postId, User author, String body) {
		Post post = posts.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
		String sanitized = sanitize(body);
		if (sanitized.isBlank()) {
			throw new IllegalArgumentException("Comment cannot be empty.");
		}
		return comments.save(new Comment(post, author, sanitized));
	}

	@Transactional(readOnly = true)
	public List<Comment> approvedComments(Long postId) {
		return comments.findByPostIdAndStatusOrderByCreatedAtAsc(postId, CommentStatus.APPROVED);
	}

	@Transactional(readOnly = true)
	public List<Comment> pendingForCreator(Long creatorId) {
		return comments.findByPostAuthorIdAndStatusOrderByCreatedAtDesc(creatorId, CommentStatus.PENDING);
	}

	@Transactional(readOnly = true)
	public List<Comment> pendingAll() {
		return comments.findByStatusOrderByCreatedAtDesc(CommentStatus.PENDING);
	}

	@Transactional
	public void approve(Long commentId, User creator) {
		Comment comment = requireOwnedComment(commentId, creator);
		comment.setStatus(CommentStatus.APPROVED);
	}

	@Transactional
	public void reject(Long commentId, User creator) {
		Comment comment = requireOwnedComment(commentId, creator);
		comment.setStatus(CommentStatus.REJECTED);
	}

	@Transactional
	public void approveAsAdmin(Long commentId) {
		Comment comment = comments.findById(commentId).orElseThrow();
		comment.setStatus(CommentStatus.APPROVED);
	}

	@Transactional
	public void rejectAsAdmin(Long commentId) {
		Comment comment = comments.findById(commentId).orElseThrow();
		comment.setStatus(CommentStatus.REJECTED);
	}

	private Comment requireOwnedComment(Long commentId, User creator) {
		Comment comment = comments.findById(commentId).orElseThrow();
		if (!comment.getPost().getAuthor().getId().equals(creator.getId())) {
			throw new SecurityException("Not allowed to moderate this comment.");
		}
		return comment;
	}

	private String sanitize(String body) {
		if (body == null) {
			return "";
		}
		String trimmed = body.trim().replaceAll("<[^>]*>", "");
		return trimmed.length() > MAX_BODY_LENGTH ? trimmed.substring(0, MAX_BODY_LENGTH) : trimmed;
	}
}
