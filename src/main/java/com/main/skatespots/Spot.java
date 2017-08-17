package com.main.skatespots;

public class Spot {
    private String id;
    private String name;
    private String description;
    private double lat;
    private double lng;
    private String type;

    // No parm constructor used for Firebase
    private Spot() {}

    public Spot(String id, String name, String description, double lat, double lng, String type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.lat = lat;
        this.lng = lng;
        this.type = type;
    }

    public String getId() { return id; }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public double getLat() {
        return lat;
    }
    public double getLng() {
        return lng;
    }
    public String getType() {
        return type;
    }
    public String getKeywords() { return name.toLowerCase() + " " + description.toLowerCase()
            + " " + type.toLowerCase(); }
}
