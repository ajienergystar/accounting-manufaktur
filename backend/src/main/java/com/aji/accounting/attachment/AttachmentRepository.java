package com.aji.accounting.attachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByEntityTypeAndEntityIdOrderByUploadedAtDesc(String entityType, Long entityId);
}
