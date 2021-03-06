package com.caribou.company.service.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Component
public class EmployeeCsvParser implements EmployeeParser {

    public MappingIterator<Row> read(MultipartFile file) throws IOException {
        return read(new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    @Override
    public MappingIterator<Row> read(String csv) throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Row.class).withHeader();

        return mapper.readerFor(Row.class).with(schema).readValues(csv);
    }

    @Override
    public String generateExample() {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Row.class).withHeader();
        try {
            return mapper.writer(schema).writeValueAsString(new Row(
                    "Bernhard",
                    "Cummerata",
                    "Bernhard.Cummerata@email.com",
                    "HR",
                    24.5d
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPropertyOrder(value = {"firstName", "lastName", "email", "department", "reamingHoliday"})
    public static class Row implements EmployeeParser.Row {

        @JsonProperty("reaming holiday")
        private Double reamingHoliday;

        @JsonProperty("first name")
        private String firstName;

        @JsonProperty("last name")
        private String lastName;

        @JsonProperty("email")
        private String email;

        @JsonProperty("department")
        private String department;

        public Row() {
        }

        public Row(String firstName, String lastName, String email, String department, Double reamingHoliday) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.department = department;
            this.reamingHoliday = reamingHoliday;
        }

        @Override
        public String getFirstName() {
            return firstName;
        }

        @Override
        public String getLastName() {
            return lastName;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public String getDepartment() {
            return department;
        }

        @Override
        public Double getReamingHoliday() {
            return reamingHoliday;
        }

    }

}
