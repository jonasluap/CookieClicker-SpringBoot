package com.basespringboot.dto;

public record WeatherResponse(String place, double temperature, int code, int bonus, boolean expired) {
}
