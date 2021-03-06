package com.caribou.holiday.rest;

import com.caribou.Factory;
import com.caribou.IntegrationTests;
import com.caribou.auth.domain.UserAccount;
import com.caribou.auth.repository.UserRepository;
import com.caribou.auth.service.UserService;
import com.caribou.company.domain.Company;
import com.caribou.company.domain.Department;
import com.caribou.company.domain.Role;
import com.caribou.company.repository.CompanyRepository;
import com.caribou.company.repository.DepartmentRepository;
import com.caribou.company.service.CompanyService;
import com.caribou.holiday.domain.LeaveType;
import com.caribou.holiday.repository.LeaveRepository;
import com.caribou.holiday.repository.LeaveTypeRepository;
import com.caribou.holiday.rest.dto.ListDto;
import com.caribou.holiday.service.LeaveService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class CompanyLeaveControllerTest extends IntegrationTests {

    private Company company;

    private UserAccount userAccount;

    private LeaveType leaveType;

    private String password;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserService userService;
    private UserAccount approver;

    @Before
    public void setUp() throws Exception {
        userAccount = Factory.userAccount();
        password = userAccount.getPassword();
        userService.register(userAccount);

        company = companyRepository.save(Factory.company());
        companyRepository.addEmployee(company, userAccount, Role.Admin);
        leaveType = leaveTypeRepository.save(LeaveType.newBuilder().company(company).name("Holiday").build());

        approver = userRepository.save(Factory.userAccount());
    }

    @Test
    public void getList() throws Exception {
        leaveRepository.save(Factory.leave(userAccount, approver, leaveType, LocalDate.of(2017, 4, 25), LocalDate.of(2017, 5, 14)));

        String url = String.format("/v1/company/%s/leaves?from=2017-05-01&to=2017-05-31", company.getUid());
        ResponseEntity<ListDto> response = get(
                url,
                ListDto.class,
                userAccount.getEmail(),
                password
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ListDto body = response.getBody();
        assertThat(body.getTotal()).isEqualTo(1);

        LinkedHashMap employeeLeavesDto = (LinkedHashMap) body.getItems().get(0);
        LinkedHashMap employeeDto = (LinkedHashMap) employeeLeavesDto.get("employee");
        assertThat(employeeDto.get("email")).isEqualTo(userAccount.getEmail());

        List<LinkedHashMap> leavesDto = (List<LinkedHashMap>) employeeLeavesDto.get("leaves");
        assertThat(leavesDto.get(0).get("starting")).isEqualTo("2017-04-25");
        assertThat(leavesDto.get(0).get("ending")).isEqualTo("2017-05-14");

        assertThat(employeeLeavesDto.get("department")).isNull();
    }

    @Test
    public void shouldHaveDepartmentIfEmployeeHasAny() throws Exception {
        UserAccount anotherUserAccount = Factory.userAccount();
        String anotherUserPassword = anotherUserAccount.getPassword();
        userService.register(anotherUserAccount);
        Company anotherCompany = companyRepository.save(Factory.company());
        Department department = departmentRepository.save(Factory.department(anotherCompany, userAccount));
        companyRepository.addEmployee(anotherCompany, department, anotherUserAccount, Role.Admin);

        String url = String.format("/v1/company/%s/leaves?from=2017-05-01&to=2017-05-31", anotherCompany.getUid());
        ResponseEntity<ListDto> response = get(
                url,
                ListDto.class,
                anotherUserAccount.getEmail(),
                anotherUserPassword
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ListDto body = response.getBody();
        LinkedHashMap employeeLeavesDto = (LinkedHashMap) body.getItems().get(0);
        LinkedHashMap departmentDto = (LinkedHashMap) employeeLeavesDto.get("department");
        assertThat(departmentDto.get("label")).isEqualTo(department.getName());
        assertThat(departmentDto.get("uid")).isEqualTo(department.getUid().toString());
        assertThat(departmentDto.get("href")).isNotNull();
    }

    @Test
    public void iCanSeeOnlyMineRemainingDaysOff() throws Exception {
        UserAccount me = Factory.userAccount();
        String minePassword = me.getPassword();
        me = userService.create(me);
        Department department = departmentRepository.save(Factory.department(company, userAccount));
        companyRepository.addEmployee(company, department, me, Role.Viewer);
        departmentRepository.addEmployee(department, me);

        String url = String.format("/v1/company/%s/leaves?from=2017-05-01&to=2017-05-31", company.getUid());
        ResponseEntity<ListDto> response = get(
                url,
                ListDto.class,
                me.getEmail(),
                minePassword
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ListDto body = response.getBody();
        assertThat(findUser(body.getItems(), me).get("remaining")).isNotNull();
    }

    private LinkedHashMap findUser(List items, UserAccount me) {
        Optional<LinkedHashMap> user = ((List<LinkedHashMap>) items).stream()
                .filter(i -> ((LinkedHashMap) i.get("employee")).get("uid").toString().equals(me.getUid().toString()))
                .findFirst();
        return user.orElse(null);
    }

    @Test
    public void iCanNotSeeRemainingDaysOffOfOthers() throws Exception {
        UserAccount me = Factory.userAccount();
        String minePassword = me.getPassword();
        me = userService.create(me);
        Department department = departmentRepository.save(Factory.department(company, userAccount));
        companyRepository.addEmployee(company, department, me, Role.Viewer);

        String url = String.format("/v1/company/%s/leaves?from=2017-05-01&to=2017-05-31", company.getUid());
        ResponseEntity<ListDto> response = get(
                url,
                ListDto.class,
                me.getEmail(),
                minePassword
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ListDto body = response.getBody();
        assertThat(findUser(body.getItems(), userAccount).get("remaining")).isNull();
    }

    @Test
    public void shouldNotGetLeavesFromAnotherCompany() throws Exception {
        UserAccount anotherUserAccount = Factory.userAccount();
        userRepository.save(anotherUserAccount);
        Company anotherCompany = companyRepository.save(Factory.company());
        companyRepository.addEmployee(anotherCompany, anotherUserAccount, Role.Admin);

        String url = String.format("/v1/company/%s/leaves?from=2017-05-01&to=2017-05-31", company.getUid());
        ResponseEntity<ListDto> response = get(
                url,
                ListDto.class,
                userAccount.getEmail(),
                password
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ListDto body = response.getBody();
        assertThat(body.getTotal()).isEqualTo(1);
    }

    @Test
    public void canNotGetLeavesFromAnotherCompany() throws Exception {
        UserAccount anotherUserAccount = Factory.userAccount();
        userRepository.save(anotherUserAccount);
        Company anotherCompany = companyRepository.save(Factory.company());
        companyRepository.addEmployee(anotherCompany, anotherUserAccount, Role.Admin);

        String url = String.format("/v1/company/%s/leaves?from=2017-05-01&to=2017-05-31", anotherCompany.getUid());
        ResponseEntity<ListDto> response = get(
                url,
                ListDto.class,
                userAccount.getEmail(),
                password
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getListWithoutDateRange() throws Exception {
        String url = String.format("/v1/company/%s/leaves", company.getUid());
        ResponseEntity<String> response = get(
                url,
                String.class,
                userAccount.getEmail(),
                password
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void bossCanSeeRemaningOfAllUsers() throws Exception {
        UserAccount me = Factory.userAccount();
        me = userService.create(me);
        Department department = departmentRepository.save(Factory.department(company, userAccount));
        companyRepository.addEmployee(company, department, me, Role.Viewer);

        String url = String.format("/v1/company/%s/leaves?from=2017-05-01&to=2017-05-31", company.getUid());
        ResponseEntity<ListDto> response = get(
                url,
                ListDto.class,
                userAccount.getEmail(),
                password
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ListDto body = response.getBody();
        assertThat(findUser(body.getItems(), userAccount).get("remaining")).isNotNull();
        assertThat(findUser(body.getItems(), me).get("remaining")).isNotNull();
    }

    @Test
    public void leaveCanBeConfirmedOnlyByBoss() throws Exception {

    }

    @Test
    public void leaveConfirmBy() throws Exception {

    }
}
