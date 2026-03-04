package com.aidonormatcher.backend.entity;

import com.aidonormatcher.backend.enums.PledgeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pledges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "donor_id")
    private User donor;

    @ManyToOne
    @JoinColumn(name = "need_id")
    private Need need;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private PledgeStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
