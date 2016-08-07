package com.caribou.holiday.domain;

import com.caribou.company.domain.Company;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;


@Entity
public class LeaveType extends AbstractEntity {

    @Column(nullable = false)
    String name;

    @ManyToOne(optional = false)
    Company company;

    private LeaveType(Builder builder) {
        name = builder.name;
        company = builder.company;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {
        private String name;
        private Company company;

        private Builder() {
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder company(Company val) {
            company = val;
            return this;
        }

        public LeaveType build() {
            return new LeaveType(this);
        }
    }
}
