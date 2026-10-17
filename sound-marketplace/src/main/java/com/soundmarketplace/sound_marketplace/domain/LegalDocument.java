package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "legal_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "legal_documents_document_type",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "document_type_id")
    )
    @Builder.Default
    private Set<DocumentType> documentTypes = new HashSet<>();
}
