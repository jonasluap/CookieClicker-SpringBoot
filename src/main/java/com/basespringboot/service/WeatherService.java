package com.basespringboot.service;

import com.basespringboot.entity.PlayerWeather;
import com.basespringboot.exception.ResourceNotFoundException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class WeatherService {
    private final RestClient geocoding;
    private final RestClient forecast;

    public WeatherService(
            @Value("${app.weather.geocoding-url:https://geocoding-api.open-meteo.com}") String geocodingUrl,
            @Value("${app.weather.forecast-url:https://api.open-meteo.com}") String forecastUrl
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        geocoding = RestClient.builder().baseUrl(geocodingUrl).requestFactory(requestFactory).build();
        forecast = RestClient.builder().baseUrl(forecastUrl).requestFactory(requestFactory).build();
    }

    public PlayerWeather lookup(String city) {
        try {
            var locations = geocoding.get()
                    .uri(uri -> uri.path("/v1/search")
                            .queryParam("name", "{city}").queryParam("count", 1).queryParam("language", "fr")
                            .build(city.strip()))
                    .retrieve().body(GeocodingResponse.class);
            if (locations == null) {
                throw unavailable();
            }
            if (locations.results() == null || locations.results().isEmpty()) {
                throw new ResourceNotFoundException("Ville introuvable. Essayez un autre nom.");
            }

            var location = locations.results().getFirst();
            if (location.name() == null || location.latitude() == null || location.longitude() == null) {
                throw unavailable();
            }
            var prediction = forecast.get()
                    .uri(uri -> uri.path("/v1/forecast")
                            .queryParam("latitude", location.latitude()).queryParam("longitude", location.longitude())
                            .queryParam("current", "temperature_2m,weather_code").build())
                    .retrieve().body(ForecastResponse.class);
            var current = prediction == null ? null : prediction.current();
            if (current == null || current.temperature() == null || current.code() == null) {
                throw unavailable();
            }
            return new PlayerWeather(location.displayName(), current.temperature(), current.code(),
                    Instant.now().plus(Duration.ofHours(1)));
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Service météo momentanément indisponible.", exception);
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service météo momentanément indisponible.");
    }

    private record GeocodingResponse(List<Location> results) {
    }

    private record Location(String name, @JsonProperty("admin1") String region, String country,
                            Double latitude, Double longitude) {
        String displayName() {
            return Stream.of(name, region, country)
                    .filter(Objects::nonNull)
                    .filter(part -> !part.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
        }
    }

    private record ForecastResponse(CurrentWeather current) {
    }

    private record CurrentWeather(@JsonProperty("temperature_2m") Double temperature,
                                  @JsonProperty("weather_code") Integer code) {
    }
}
