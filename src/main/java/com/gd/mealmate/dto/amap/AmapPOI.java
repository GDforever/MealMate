package com.gd.mealmate.dto.amap;

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
    private Double locationLat;  // parsed from "lat,lng"
    private Double locationLng;
    private String type;
    private Integer distance;
}
