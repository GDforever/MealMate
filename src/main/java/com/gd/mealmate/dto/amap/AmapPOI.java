package com.gd.mealmate.dto.amap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapPOI {

    private String id;
    private String name;
    private Object address;
    private Object tel;
    private String location;  // "lng,lat" format returned by AMap
    private String type;
    private String distance;
    private java.util.List<AmapPhoto> photos;

    @JsonIgnore
    public String getAddressAsString() {
        if (address == null) return null;
        if (address instanceof String) return (String) address;
        if (address instanceof java.util.List<?> list && !list.isEmpty()) return list.get(0).toString();
        return address.toString();
    }

    @JsonIgnore
    public String getFirstPhotoUrl() {
        if (photos != null && !photos.isEmpty() && photos.get(0).getUrl() != null) {
            return photos.get(0).getUrl();
        }
        return null;
    }

    @JsonIgnore
    public Double getLocationLat() {
        if (location == null || !location.contains(",")) {
            return null;
        }
        return Double.parseDouble(location.split(",")[1]);
    }

    @JsonIgnore
    public Double getLocationLng() {
        if (location == null || !location.contains(",")) {
            return null;
        }
        return Double.parseDouble(location.split(",")[0]);
    }
}
