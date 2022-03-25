package com.geri;

enum LocationType {
    CITY, SEASIDE
}

public class Location {
    private String name;
    private LocationType type;
    private Double latitude;
    private Double longitude;

    public Location(String name, LocationType type) {
        this.name = name;
        this.type = type;
    }
}
