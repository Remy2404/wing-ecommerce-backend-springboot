package com.wing.ecommercebackendwing.dto.request.user;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @Pattern(regexp = ".*\\S.*", message = "First name must not be blank")
    private String firstName;
    @Pattern(regexp = ".*\\S.*", message = "Last name must not be blank")
    private String lastName;
    private String phoneNumber;
}
