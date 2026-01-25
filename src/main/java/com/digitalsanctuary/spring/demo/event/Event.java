package com.digitalsanctuary.spring.demo.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Event name is required")
    private String name;
    
    @NotBlank(message = "Event description is required")
    private String description;
    
    @NotBlank(message = "Event location is required")
    private String location;
    
    @NotNull(message = "Event date is required")
    private LocalDate date;
    
    @NotNull(message = "Event time is required")
    private LocalTime time;

    /**
     * Override the equals method to compare the id of the Event object
     *
     * @param o passed in object
     * @return true if the id of the Event object is equal to the id of the passed in object
     */
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }

    /**
     * Override the hashCode method to return the hash code of the id of the Event object
     *
     * @return the hash code of the id of the Event object
     */
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
