package org.tanzu.ipzs.legislation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tanzu.ipzs.legislation.model.entity.LegislationDocument;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegislationDocumentRepository extends JpaRepository<LegislationDocument, UUID> {

    /**
     * Find document by its unique document ID (not the UUID primary key)
     */
    Optional<LegislationDocument> findByDocumentId(String documentId);

    /**
     * Find all documents that are effective on a given date
     */
    @Query("SELECT d FROM LegislationDocument d WHERE d.effectiveDate <= :date AND (d.expirationDate IS NULL OR d.expirationDate > :date)")
    List<LegislationDocument> findEffectiveDocuments(@Param("date") LocalDate date);

    /**
     * Find documents by document type
     */
    List<LegislationDocument> findByDocumentType(String documentType);

    /**
     * Find documents by issuing authority
     */
    List<LegislationDocument> findByIssuingAuthority(String issuingAuthority);

    /**
     * Find documents effective within a date range
     */
    @Query("SELECT d FROM LegislationDocument d WHERE d.effectiveDate BETWEEN :startDate AND :endDate")
    List<LegislationDocument> findByEffectiveDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find documents by title containing text (case-insensitive)
     */
    @Query("SELECT d FROM LegislationDocument d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :titlePart, '%'))")
    List<LegislationDocument> findByTitleContainingIgnoreCase(@Param("titlePart") String titlePart);

    /**
     * Check if a document with the given documentId already exists
     */
    boolean existsByDocumentId(String documentId);

    /**
     * Find all documents ordered by effective date (newest first)
     */
    List<LegislationDocument> findAllByOrderByEffectiveDateDesc();

    /**
     * Find documents that have expired as of a given date
     */
    @Query("SELECT d FROM LegislationDocument d WHERE d.expirationDate IS NOT NULL AND d.expirationDate <= :date")
    List<LegislationDocument> findExpiredDocuments(@Param("date") LocalDate date);
}