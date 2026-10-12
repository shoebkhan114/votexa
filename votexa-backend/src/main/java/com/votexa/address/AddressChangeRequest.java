package com.votexa.address;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "address_change_requests")
@Getter @Setter @NoArgsConstructor
public class AddressChangeRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_code", nullable = false)
    private String requestCode;
    @Column(name = "voter_id", nullable = false)
    private Long voterId;
    @Column(nullable = false)
    private String source = "CITIZEN";
    @Column(name = "new_line1", nullable = false)
    private String newLine1;
    @Column(name = "new_locality", nullable = false)
    private String newLocality;
    @Column(name = "new_district", nullable = false)
    private String newDistrict;
    @Column(name = "new_state", nullable = false)
    private String newState;
    @Column(name = "new_pincode", nullable = false)
    private String newPincode;
    @Column(name = "new_constituency_id")
    private Long newConstituencyId;
    @Column(name = "new_polling_station_id")
    private Long newPollingStationId;
    @Column(nullable = false)
    private String status = "PENDING";
    private String reason;
    @Column(name = "verified_by")
    private Long verifiedBy;
    @Column(name = "verified_at")
    private Instant verifiedAt;
    @Column(name = "decided_by")
    private Long decidedBy;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "decision_remarks")
    private String decisionRemarks;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
