package com.caribou.company.repository;

import com.caribou.Factory;
import com.caribou.IntegrationTests;
import com.caribou.auth.domain.UserAccount;
import com.caribou.auth.repository.UserRepository;
import com.caribou.company.domain.Company;
import com.caribou.company.domain.Department;
import com.caribou.company.domain.Invitation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class InvitationRepositoryTest extends IntegrationTests {

    @Autowired
    UserRepository userRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    InvitationRepository invitationRepository;

    Company company;

    Department department;

    @Before
    public void setUp() throws Exception {
        company = companyRepository.save(Factory.company());
        department = departmentRepository.save(Factory.department(company));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void userCannotHaveMoreInvitations() throws Exception {
        UserAccount userAccount = userRepository.save(Factory.userAccount());

        Invitation invitation = Invitation.builder()
                .company(company)
                .department(department)
                .userAccount(userAccount)
                .build();
        invitationRepository.save(invitation);

        Company company = companyRepository.save(Factory.company());
        Department department = departmentRepository.save(Factory.department(company));
        invitation = Invitation.builder()
                .key(UUID.randomUUID().toString())
                .company(company)
                .department(department)
                .userAccount(userAccount)
                .build();
        invitationRepository.save(invitation);
    }

    @Test
    public void findByEmail() throws Exception {
        UserAccount userAccount = userRepository.save(Factory.userAccount());

        Invitation invitation = Invitation.builder()
                .key(UUID.randomUUID().toString())
                .company(company)
                .department(department)
                .userAccount(userAccount)
                .build();
        invitationRepository.save(invitation);

        Optional<Invitation> inv = invitationRepository.findByUserEmail(userAccount.getEmail());
        assertThat(inv).isPresent();
        assertThat(inv.get().getKey()).isEqualTo(invitation.getKey());
    }

}
