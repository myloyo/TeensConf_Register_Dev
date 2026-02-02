package com.teensconf.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationCreatedEvent {
    private Long registrationId;
    private String firstName;
    private String lastName;
    private String email;
}
