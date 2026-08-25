package intercoach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {

    @Size(min = 3, max = 50)
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    private String username;

    @Email
    @Size(max = 255)
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    private String email;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
