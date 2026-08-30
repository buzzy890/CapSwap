package de.capswap.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "offer_proposal_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferProposalPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_proposal_id", nullable = false)
    private OfferProposal offerProposal;

    @NotBlank(message = "Foto-URL darf nicht leer sein.")
    @Column(name = "photo_url", nullable = false, length = 1024)
    private String photoUrl;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
