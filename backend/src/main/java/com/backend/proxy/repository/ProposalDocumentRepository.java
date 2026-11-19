package com.backend.proxy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.proxy.entity.ProposalDocument;

public interface ProposalDocumentRepository extends JpaRepository<ProposalDocument, Long> {

    /** Cuántas versiones tiene la propuesta del curso: la próxima es esta cantidad + 1. */
    int countByCourseId(Long courseId);

    Optional<ProposalDocument> findByCourseIdAndVersion(Long courseId, int version);

    Optional<ProposalDocument> findFirstByCourseIdOrderByVersionDesc(Long courseId);

    List<ProposalDocument> findByCourseIdOrderByVersionDesc(Long courseId);
}
