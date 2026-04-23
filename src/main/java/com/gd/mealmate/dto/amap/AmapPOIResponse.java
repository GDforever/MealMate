package com.gd.mealmate.dto.amap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmapPOIResponse {

    private Integer status;
    private String info;
    private String infocode;
    private List<AmapPOI> pois;

    @JsonProperty("count")
    private String totalCount;
}
