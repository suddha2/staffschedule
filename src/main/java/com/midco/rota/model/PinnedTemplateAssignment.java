package com.midco.rota.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A pinned (template, week-of-period, employee) tuple.
 *
 * <p>{@code week_of_period} is the 1-based week index within the 4-week pay
 * period (1 = first Mon-Sun, 4 = last). Pins are week-specific so that a roster
 * pattern like "Blessed works Monday DAY @ BAYLIE LANE in week 1 only" can be
 * represented without forcing him onto every Monday in the period.
 *
 * <p>The pin is carried forward to the next period by matching on the same
 * {@code (shift_template_id, week_of_period, employee_id)} tuple: P2's week 1
 * inherits P1's week 1 pins, P2's week 2 inherits P1's week 2 pins, etc.
 */
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

    /**
     * 1-based week index within the pay period (1..4 for a standard 4-week
     * period). Together with {@code shift_template_id} and {@code employee_id}
     * forms the natural key for a pin.
     */
    @Column(name = "week_of_period", nullable = false)
    private Short weekOfPeriod;

    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;

    @Column(name = "pinned_by_user_id")
    private Long pinnedByUserId;

    public PinnedTemplateAssignment() {
    }

    public PinnedTemplateAssignment(Long shiftTemplateId, Long employeeId, Short weekOfPeriod,
            LocalDateTime pinnedAt, Long pinnedByUserId) {
        this.shiftTemplateId = shiftTemplateId;
        this.employeeId = employeeId;
        this.weekOfPeriod = weekOfPeriod;
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

    public Short getWeekOfPeriod() {
        return weekOfPeriod;
    }

    public void setWeekOfPeriod(Short weekOfPeriod) {
        this.weekOfPeriod = weekOfPeriod;
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
        private Short weekOfPeriod;
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

        public Builder weekOfPeriod(Short weekOfPeriod) {
            this.weekOfPeriod = weekOfPeriod;
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
            return new PinnedTemplateAssignment(shiftTemplateId, employeeId, weekOfPeriod, pinnedAt, pinnedByUserId);
        }
    }
}
