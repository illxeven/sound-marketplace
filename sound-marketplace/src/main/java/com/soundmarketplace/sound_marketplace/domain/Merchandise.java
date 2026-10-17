package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchandise")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchandise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchandise_type_id", nullable = true)
    private MerchandiseType merchandiseType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private MerchandiseCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private UserAccount seller;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_blob", columnDefinition = "LONGBLOB")
    private byte[] imageBlob;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_blob_2", columnDefinition = "LONGBLOB")
    private byte[] imageBlob2;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_blob_3", columnDefinition = "LONGBLOB")
    private byte[] imageBlob3;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_blob_4", columnDefinition = "LONGBLOB")
    private byte[] imageBlob4;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_blob_5", columnDefinition = "LONGBLOB")
    private byte[] imageBlob5;

    @Column(name = "image_key_1", length = 500)
    private String imageKey1;

    @Column(name = "image_key_2", length = 500)
    private String imageKey2;

    @Column(name = "image_key_3", length = 500)
    private String imageKey3;

    @Column(name = "image_key_4", length = 500)
    private String imageKey4;

    @Column(name = "image_key_5", length = 500)
    private String imageKey5;

    @Column(name = "options", length = 500)
    private String options; // E.g., 'S,M,L,XL'

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Deprecated
    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
