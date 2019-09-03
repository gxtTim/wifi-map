package mitre.demo;

public class SignalInfo implements Comparable{
    String wifiId;
    String wifiName;
    int strength;
    double latitude;
    double longitude;

    public SignalInfo(String wifiId, String wifiName, int strength, double latitude, double longitude) {
        this.wifiId = wifiId;
        this.wifiName = wifiName;
        this.strength = strength;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return wifiId + " - " + wifiName + " - " + latitude + ", " + longitude + " - " + strength;
    }

    @Override
    public int compareTo(Object o) {
        SignalInfo otherSignal = (SignalInfo) o;
        return otherSignal.strength - strength;
    }
}
