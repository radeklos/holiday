package com.caribou.auth.rest;

import com.caribou.Factory;
import com.caribou.IntegrationTests;
import com.caribou.auth.domain.UserAccount;
import com.caribou.auth.repository.UserRepository;
import com.caribou.auth.rest.dto.Error;
import com.caribou.auth.rest.dto.ErrorField;
import com.caribou.auth.rest.dto.UserAccountDto;
import com.caribou.auth.service.UserService;
import com.caribou.company.domain.Company;
import com.caribou.company.domain.Role;
import com.caribou.company.repository.CompanyRepository;
import org.junit.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


public class UserRestControllerTest extends IntegrationTests {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Test
    public void requestWithEmptyJsonRequestReturnsUnprocessableEntity() throws Exception {
        ResponseEntity<HashMap> response = testRestTemplate().exchange(
                path("/v1/users"),
                HttpMethod.POST,
                new HttpEntity("{}", jsonHeader()),
                HashMap.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("errors")).isInstanceOf(HashMap.class);
    }

    @Test
    public void createNewUser() throws Exception {
        UserAccountDto userAccount = Factory.userAccountDto();
        String json = "{" +
                "\"firstName\":\"%s\"," +
                "\"lastName\":\"%s\"," +
                "\"email\":\"%s\"," +
                "\"password\":\"%s\"" +
                "}";
        json = String.format(json, userAccount.getFirstName(), userAccount.getLastName(), userAccount.getEmail(), userAccount.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = testRestTemplate().exchange(
                path("/v1/users"),
                HttpMethod.POST,
                new HttpEntity<>(json, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Optional<UserAccount> user = userRepository.findByEmail(userAccount.getEmail());
        assertThat(user.isPresent()).isTrue();
    }

    @Test
    public void createNewUserWithoutPasswordReturns422() throws Exception {
        UserAccountDto userAccount = Factory.userAccountDto();
        String json = "{" +
                "\"firstName\":\"%s\"," +
                "\"lastName\":\"%s\"," +
                "\"email\":\"%s\"" +
                "}";
        json = String.format(json, userAccount.getFirstName(), userAccount.getLastName(), userAccount.getEmail());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = testRestTemplate().exchange(
                path("/v1/users"),
                HttpMethod.POST,
                new HttpEntity<>(json, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    public void updateUserDetails() throws Exception {
        UserAccount user = Factory.userAccount();
        String password = user.getPassword();
        userService.create(user);

        UserAccountDto userDto = Factory.userAccountDto();
        ResponseEntity<String> response = put(
                "/v1/users/me",
                userDto,
                String.class,
                user.getEmail(),
                password
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void updateOnlyPassword() throws Exception {
        UserAccount user = Factory.userAccount();
        String password = user.getPassword();
        userService.create(user);

        UserAccountDto userDto = UserAccountDto.builder().password("new_password").build();
        ResponseEntity<String> response = put(
                "/v1/users/me",
                userDto,
                String.class,
                user.getEmail(),
                password
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK).as(response.getBody());

        response = get("/v1/users/me", String.class, user.getEmail(), password);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void updateUserDetailsForUnauthorisedUserReturns401() throws Exception {
        UserAccount user = Factory.userAccount();
        userService.create(user);

        UserAccountDto userDto = Factory.userAccountDto();
        ResponseEntity<String> response = put(
                "/v1/users/me",
                userDto,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void cannotCreateUserWithSameEmailAddress() throws Exception {
        UserAccountDto userAccount = Factory.userAccountDto();

        ModelMapper modelMapper = new ModelMapper();
        userRepository.save(modelMapper.map(userAccount, UserAccount.class));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String payload = String.format("{\"email\":\"%s\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"password\":\"abcabc\"}", userAccount.getEmail());
        ResponseEntity<Error> response = testRestTemplate().exchange(
                path("/v1/users"),
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Error.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ErrorField emailError = response.getBody().getValidationErrors().get("email");
        assertThat(emailError.getCode()).isEqualTo("must be unique");
        assertThat(emailError.getDefaultMessage()).isEqualTo("Email is already taken");
        assertThat(emailError.getRejectedValue()).isEqualTo(userAccount.getEmail());

        Optional<UserAccount> user = userRepository.findByEmail(userAccount.getEmail());
        assertThat(user.isPresent()).isTrue();
    }

    @Test
    public void userDetailIsUnauthorized() throws Exception {
        UserAccount userAccount = Factory.userAccount();
        userRepository.save(userAccount);

        ResponseEntity<UserAccountDto> response = testRestTemplate().exchange(
                path("/v1/users/me"),
                HttpMethod.GET,
                new HttpEntity<>(null, jsonHeader()),
                UserAccountDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void getMineUserDetail() throws Exception {
        UserAccount userAccount = Factory.userAccount();
        String userPassword = userAccount.getPassword();
        userAccount = userService.create(userAccount);

        ResponseEntity<UserAccountDto> response = testRestTemplate().exchange(
                path("/v1/users/me"),
                HttpMethod.GET,
                new HttpEntity<>(null, getTokenHeader(userAccount.getEmail(), userPassword)),
                UserAccountDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserAccountDto user = response.getBody();
        assertThat(user.getFirstName()).isEqualTo(userAccount.getFirstName());
        assertThat(user.getLastName()).isEqualTo(userAccount.getLastName());
        assertThat(user.getEmail()).isEqualTo(userAccount.getEmail());
        assertThat(user.getPassword()).isNull();
    }

    @Test
    public void userHasLinkToHisCompany() throws Exception {
        UserAccount userAccount = Factory.userAccount();
        String userPassword = userAccount.getPassword();
        userAccount = userService.create(userAccount);
        Company company = Factory.company();
        company.addEmployee(userAccount, Role.Owner);
        companyRepository.save(company);

        ResponseEntity<UserAccountDto> response = testRestTemplate().exchange(
                path("/v1/users/me"),
                HttpMethod.GET,
                new HttpEntity<>(null, getTokenHeader(userAccount.getEmail(), userPassword)),
                UserAccountDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        UserAccountDto user = response.getBody();
        assertThat(user.getCompany()).isNotNull();
        assertThat(user.getCompany().getHref()).isNotEmpty();
        assertThat(user.getCompany().getUid()).isEqualTo(company.getUid().toString());
    }

}
