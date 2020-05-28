package Flat;

import java.io.Serializable;

/**
 * Служебный класс класса Flat.
 */
public class Coordinates implements Serializable {
    private static final long serialVersionUID = 500L;
    private int x; //Значение поля должно быть больше -897
    private double y; //Поле не может быть null

    public Coordinates(int x, double y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}

