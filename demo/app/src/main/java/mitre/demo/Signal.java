package mitre.demo;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;

public class Signal implements Comparable {
    private String id; //mac address or BSSID
    private Integer level; //RSSI. -70...-30
    private Integer strength; // Calculated strength based on RSSI. 0..100
    private Integer freq; //frequency (only wifi)
    private String name; // SSID for wifi
    private String venue; // registered venue of access point (only wifi)
    private String type; //wifi
    private ArrayList<Integer> history;

    Signal(ScanResult wifi) {
        this.id = wifi.BSSID;
        this.level = wifi.level;
        this.strength = WifiManager.calculateSignalLevel(wifi.level, 100);
        this.freq = wifi.frequency;
        this.name = wifi.SSID==null?"NA WiFi":wifi.SSID;
        this.venue = wifi.venueName.toString();
        this.type = "wifi";
        history = new ArrayList<>();
        history.add(this.level);
    }

    @Override
    public int compareTo(Object o) {
        Signal otherSignal = (Signal) o;
        return otherSignal.level - level;
    }

    @Override
    public String toString() {
        return type + " - " + name + " - " + strength;
    }

    public void update(int level) {
        this.level = level;
        strength = WifiManager.calculateSignalLevel(level, 100);
        history.add(level);
        while (history.size() > 21) {
            history.remove(0);
        }
    }

    @Override
    public boolean equals(Object obj) {
        Signal otherSignal = (Signal) obj;
        return id.equals(otherSignal.id);
    }

    public String getId() {
        return id;
    }

    public ArrayList<Integer> getHistory() {
        return history;
    }

    public int getLevel() {
        return level;
    }

    public int getStrength() {
        return strength;
    }

    public String getName() {
        return name;
    }

    public String getVenue() {
        return venue;
    }

    public int getFreq() {
        return freq;
    }

    public String getType() {
        return type;
    }
}
