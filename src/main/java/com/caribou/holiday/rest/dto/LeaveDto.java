package com.caribou.holiday.rest.dto;

import com.caribou.holiday.domain.LeaveType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDto {

    @JsonProperty
    private LeaveType leaveType;

    @JsonProperty(required = true)
    private LocalDate starting;

    @JsonProperty(required = true)
    private LocalDate ending;

    @Size(max = 255)
    @JsonProperty
    private String reason;

    private String uid;

}
