package com.midco.rota.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pinned_template_assignment")
public class PinnedTemplateAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_template_id", nullable = false)
    private Long shiftTemplateId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;

    @Column(name = "pinned_by_user_id")
    private Long pinnedByUserId;

    public PinnedTemplateAssignment() {
    }

    public PinnedTemplateAssignment(Long shiftTemplateId, Long employeeId, LocalDateTime pinnedAt, Long pinnedByUserId) {
        this.shiftTemplateId = shiftTemplateId;
        this.employeeId = employeeId;
        this.pinnedAt = pinnedAt;
        this.pinnedByUserId = pinnedByUserId;
    }

    @PrePersist
    protected void onCreate() {
        if (pinnedAt == null) {
            pinnedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShiftTemplateId() {
        return shiftTemplateId;
    }

    public void setShiftTemplateId(Long shiftTemplateId) {
        this.shiftTemplateId = shiftTemplateId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getPinnedAt() {
        return pinnedAt;
    }

    public void setPinnedAt(LocalDateTime pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    public Long getPinnedByUserId() {
        return pinnedByUserId;
    }

    public void setPinnedByUserId(Long pinnedByUserId) {
        this.pinnedByUserId = pinnedByUserId;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long shiftTemplateId;
        private Long employeeId;
        private LocalDateTime pinnedAt;
        private Long pinnedByUserId;

        public Builder shiftTemplateId(Long shiftTemplateId) {
            this.shiftTemplateId = shiftTemplateId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder pinnedAt(LocalDateTime pinnedAt) {
            this.pinnedAt = pinnedAt;
            return this;
        }

        public Builder pinnedByUserId(Long pinnedByUserId) {
            this.pinnedByUserId = pinnedByUserId;
            return this;
        }

        public PinnedTemplateAssignment build() {
            return new PinnedTemplateAssignment(shiftTemplateId, employeeId, pinnedAt, pinnedByUserId);
        }
    }
}