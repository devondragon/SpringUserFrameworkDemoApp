package com.digitalsanctuary.spring.demo.event;

import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String location;
    private LocalDate date;
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
        return id.equals(event.id);
    }

    /**
     * Override the hashCode method to return the hash code of the id of the Event object
     *
     * @return the hash code of the id of the Event object
     */
    public int hashCode() {
        return id.hashCode();
    }
}
