package Flat;

import java.io.Serializable;

/**
 * Служебный класс класса Flat.
 */
public class House implements Serializable {
    private static final long serialVersionUID = 500L;
    private String name; //Поле не может быть null
    private int year; //Значение поля должно быть больше 0
    private Long numberOfLifts; //Значение поля должно быть больше 0

    public House(String name, Integer year, Long numberOfLifts) {
        this.name = name;
        this.year = year;
        this.numberOfLifts = numberOfLifts;
    }

    public String getName() {
        return name;
    }

    public Integer getYear() {
        return year;
    }

    public Long getNumberOfLifts() {
        return numberOfLifts;
    }
}
