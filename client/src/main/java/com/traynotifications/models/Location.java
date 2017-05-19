package com.traynotifications.models;

/**
 * Created by rask on 07.05.2017.
 */

public class Location {

    private double x, y;

    public Location(double xLoc, double yLoc) {
        this.x = xLoc;
        this.y = yLoc;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
