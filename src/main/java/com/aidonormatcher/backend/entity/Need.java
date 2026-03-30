package com.aidonormatcher.backend.entity;

import com.aidonormatcher.backend.enums.NeedCategory;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "needs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Need {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ngo_id")
    private Ngo ngo;

    @Enumerated(EnumType.STRING)
    private NeedCategory category;

    private String itemName;
    private String description;

    private int quantityRequired;
    private int quantityPledged = 0;
    private int quantityReceived = 0;

    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgency;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    private NeedStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime fulfilledAt;

    /**
     * Derived — not stored in database.
     */
    @Transient
    public int getQuantityRemaining() {
        return quantityRequired - quantityPledged;
    }

    @Transient
    public int getQuantityRemainingToReceive() {
        return quantityRequired - quantityReceived;
    }
}
