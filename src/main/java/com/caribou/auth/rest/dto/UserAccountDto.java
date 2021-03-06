package com.caribou.auth.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


public class UserAccountDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String uid;

    @NotEmpty(groups = {CreateGroup.class})
    @Size(max = 255)
    @JsonProperty
    private String firstName;

    @NotEmpty(groups = {CreateGroup.class})
    @Size(max = 255)
    @JsonProperty
    private String lastName;

    @NotNull(groups = {CreateGroup.class})
    @Size(min = 6, max = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotEmpty(groups = {CreateGroup.class})
    @Email
    @Size(max = 255)
    private String email;

    @JsonProperty
    private NestedSingleObject company;

    public UserAccountDto() {
    }

    private UserAccountDto(Builder builder) {
        firstName = builder.firstName;
        lastName = builder.lastName;
        password = builder.password;
        email = builder.email;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public NestedSingleObject getCompany() {
        return company;
    }

    public void setCompany(NestedSingleObject company) {
        this.company = company;
    }

    public static final class Builder {

        private String firstName;

        private String lastName;

        private String password;

        private String email;

        private Builder() {
        }

        public Builder firstName(String val) {
            firstName = val;
            return this;
        }

        public Builder lastName(String val) {
            lastName = val;
            return this;
        }

        public Builder password(String val) {
            password = val;
            return this;
        }

        public Builder email(String val) {
            email = val;
            return this;
        }

        public UserAccountDto build() {
            return new UserAccountDto(this);
        }
    }

    public interface CreateGroup {
    }
}
