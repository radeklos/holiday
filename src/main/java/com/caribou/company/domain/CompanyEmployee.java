package com.caribou.company.domain;

import com.caribou.auth.domain.UserAccount;

import javax.persistence.*;


@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_uid", "member_uid"})
)
@Entity
public class CompanyEmployee extends AbstractEntity {

    @ManyToOne(optional = false)
    private Company company;

    @ManyToOne(optional = false)
    private UserAccount member;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    public CompanyEmployee() {
    }

    CompanyEmployee(Company company, UserAccount member, Role role) {
        this.company = company;
        this.member = member;
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public int hashCode() {
        int result = getCompany().hashCode();
        result = 31 * result + getMember().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompanyEmployee that = (CompanyEmployee) o;

        return getCompany().equals(that.getCompany()) && getMember().equals(that.getMember());

    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public UserAccount getMember() {
        return member;
    }

    public void setMember(UserAccount member) {
        this.member = member;
    }
}
