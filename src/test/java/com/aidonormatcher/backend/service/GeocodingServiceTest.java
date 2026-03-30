package com.aidonormatcher.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        geocodingService = new GeocodingService(restTemplate);
        ReflectionTestUtils.setField(geocodingService, "provider", "nominatim");
        ReflectionTestUtils.setField(geocodingService, "nominatimBaseUrl", "https://nominatim.openstreetmap.org");
        ReflectionTestUtils.setField(geocodingService, "nominatimUserAgent", "AI-Donor-Matcher/1.0");
    }

    @Test
    void geocode_success_returnsCoordinates() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(Map.of("lat", "6.9271", "lon", "79.8612"))));

        GeocodingService.GeocodedPoint point = geocodingService.geocode("123 Main Street, Colombo");

        assertThat(point.lat()).isEqualTo(6.9271);
        assertThat(point.lng()).isEqualTo(79.8612);
    }

    @Test
    void geocode_noResults_throwsRuntimeException() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of()));

        assertThatThrownBy(() -> geocodingService.geocode("Unknown place"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to geocode");
    }

    @Test
    void geocode_exactMiss_fallsBackToBroaderAddressSegment() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of()))
                .thenReturn(ResponseEntity.ok(List.of(Map.of("lat", "14.4493", "lon", "79.9873"))));

        GeocodingService.GeocodedPoint point = geocodingService.geocode("Mulapet, Nellore");

        assertThat(point.lat()).isEqualTo(14.4493);
        assertThat(point.lng()).isEqualTo(79.9873);
        verify(restTemplate).exchange(
                eq("https://nominatim.openstreetmap.org/search?q=Mulapet%2C+Nellore&format=json&limit=1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
        verify(restTemplate).exchange(
                eq("https://nominatim.openstreetmap.org/search?q=Nellore&format=json&limit=1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }
}
