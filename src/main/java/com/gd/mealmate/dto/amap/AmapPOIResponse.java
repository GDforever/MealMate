package com.gd.mealmate.dto.amap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmapPOIResponse {

    private String status;
    private String info;
    private String infocode;
    private List<AmapPOI> pois;
    private String count;

    public boolean isSuccess() {
        return "1".equals(status);
    }
}
