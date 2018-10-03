package barikoi.taufiq.videorecorder;

class Gpx {
    String id,  lat,  lon, time;
    float speed;

    public Gpx(String id, String lat, String lon, String time,float speed) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.time = time;
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    public String getSpeedwithUnit() {
        return (speed*3.6) +"km/h";
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
