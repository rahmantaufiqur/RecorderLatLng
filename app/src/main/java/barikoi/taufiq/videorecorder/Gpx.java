package barikoi.taufiq.videorecorder;

class Gpx {
    String id,  lat,  lon, time;

    public Gpx(String id, String lat, String lon, String time) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.time = time;
    }

    public String getId() {
        return id;
    }


    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }


    public String getTime() {
        return time;
    }

}
