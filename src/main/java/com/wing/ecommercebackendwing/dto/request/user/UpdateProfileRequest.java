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
    @Pattern(
            regexp = "^(\\+?[1-9]\\d{1,14}|0\\d{8,9})$",
            message = "Invalid phone number format"
    )
    private String phoneNumber;
}
