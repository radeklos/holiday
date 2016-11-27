package com.caribou.company.rest;

import com.caribou.Factory;
import com.caribou.Header;
import com.caribou.IntegrationTests;
import com.caribou.Json;
import com.caribou.WebApplication;
import com.caribou.auth.domain.UserAccount;
import com.caribou.auth.repository.UserRepository;
import com.caribou.company.domain.Company;
import com.caribou.company.domain.Role;
import com.caribou.company.repository.CompanyRepository;
import com.caribou.company.rest.dto.CompanyDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.StatusResultMatchers;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {WebApplication.class})
@WebAppConfiguration
public class CompanyRestControllerTest extends IntegrationTests {

    private static UserAccount userAccount;

    private static HttpHeaders authHeader;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected FilterChainProxy[] filterChainProxy;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    UserRepository userRepository;

    private MockMvc mockMvc;

    private StatusResultMatchers status;
    private TestRestTemplate restAuthenticated;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = webAppContextSetup(webApplicationContext).addFilters(filterChainProxy).build();
        status = status();

        userAccount = Factory.userAccount();
        userRepository.save(userAccount);

        authHeader = Header.basic(userAccount.getEmail(), userAccount.getPassword());

        restAuthenticated = new TestRestTemplate(userAccount.getEmail(), userAccount.getPassword());
    }

    @Test
    public void whenUserRequestCompanyWhereHeDoesNotBelongToReturns404() throws Exception {
        Company company = Factory.company();
        companyRepository.save(company);

        ResponseEntity<CompanyDto> response = restAuthenticated.getForEntity(
                path(String.format("/v1/companies/%s", company.getUid())),
                CompanyDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getUserCompany() throws Exception {
        Company company = Factory.company();
        company.addEmployee(userAccount, Role.Owner);
        companyRepository.save(company);

        ResponseEntity<CompanyDto> response = restAuthenticated.getForEntity(
                path(String.format("/v1/companies/%s", company.getUid())),
                CompanyDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getCompany() throws Exception {
        Company company = Factory.company();
        company.addEmployee(userAccount, Role.Owner);
        companyRepository.save(company);

        ResponseEntity<CompanyDto> response = restAuthenticated.getForEntity(
                path(String.format("/v1/companies/%s", company.getUid())),
                CompanyDto.class);

        CompanyDto companyDto = response.getBody();
        assertThat(companyDto.getUid()).isEqualTo(companyDto.getUid());
        assertThat(companyDto.getName()).isEqualTo(company.getName());
        assertThat(companyDto.getDefaultDaysOff()).isEqualTo(company.getDefaultDaysOff());
    }

    @Test
    public void requestWithEmptyJsonRequestReturnsUnprocessableEntity() throws Exception {
        CompanyDto company = CompanyDto.newBuilder().build();

        mockMvc.perform(
                post("/v1/companies")
                        .content(Json.dumps(company))
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(authHeader))
                .andExpect(status.isUnprocessableEntity());
    }

    @Test
    public void createNewCompany() throws Exception {
        CompanyDto company = Factory.companyDto();

        MvcResult result = mockMvc.perform(
                post("/v1/companies")
                        .content(Json.dumps(company))
                        .headers(authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    public void updateCompany() throws Exception {
        Company company = Factory.company();
        companyRepository.save(company);

        CompanyDto companyDto = CompanyDto.newBuilder()
                .name("company name")
                .defaultDaysOf(10)
                .build();

        MvcResult result = mockMvc.perform(
                put(String.format("/v1/companies/%s", company.getUid()))
                        .content(Json.dumps(companyDto))
                        .headers(authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void updateNonExistingCompany() throws Exception {
        CompanyDto companyDto = Factory.companyDto();

        mockMvc.perform(
                put("/v1/companies/0")
                        .content(Json.dumps(companyDto))
                        .headers(authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status.isNotFound());
    }

    @Test
    public void nonExisting() throws Exception {
        mockMvc.perform(get("/v1/companies/0").headers(authHeader)).andExpect(status.isNotFound());
    }

}
