package com.gd.mealmate.dto.amap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmapPOI {

    private String id;
    private String name;
    private String address;
    private String tel;
    private String location;  // "lng,lat" format returned by AMap
    private String type;
    private String distance;

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
