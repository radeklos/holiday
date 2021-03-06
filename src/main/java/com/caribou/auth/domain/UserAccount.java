package com.caribou.auth.domain;

import com.caribou.AbstractEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


@Entity
@Table(indexes = {@Index(columnList = "email, password")})
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount extends AbstractEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Getter
    @Column(nullable = false)
    private final Locale locale = Locale.UK;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    private UserAccount(Builder builder) {
        setEmail(builder.email);
        setPassword(builder.password);
        setFirstName(builder.firstName);
        setLastName(builder.lastName);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public List<UserRole> getRoles() {
        return Collections.singletonList(UserRole.MEMBER);
    }

    public static final class Builder {

        private String email;

        private String password;

        private String firstName;

        private String lastName;

        private Builder() {
        }

        public Builder email(String val) {
            email = val;
            return this;
        }

        public Builder password(String val) {
            password = val;
            return this;
        }

        public Builder firstName(String val) {
            firstName = val;
            return this;
        }

        public Builder lastName(String val) {
            lastName = val;
            return this;
        }

        public UserAccount build() {
            return new UserAccount(this);
        }
    }
}
