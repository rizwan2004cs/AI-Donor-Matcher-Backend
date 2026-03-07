package com.aidonormatcher.backend.entity;

import com.aidonormatcher.backend.enums.NeedCategory;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.TrustTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ngos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ngo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String name;
    private String address;
    private String contactEmail;
    private String contactPhone;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private NeedCategory categoryOfWork;

    private String photoUrl;
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    private NgoStatus status;

    private boolean profileComplete = false;

    private Double lat;
    private Double lng;

    private int trustScore = 0;

    @Enumerated(EnumType.STRING)
    private TrustTier trustTier;

    private LocalDateTime verifiedAt;
    private LocalDateTime lastActivityAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
