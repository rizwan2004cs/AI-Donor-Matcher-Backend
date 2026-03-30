package com.aidonormatcher.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestTemplate restTemplate;

    @Value("${geocoding.provider:nominatim}")
    private String provider;

    @Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String nominatimBaseUrl;

    @Value("${geocoding.nominatim.user-agent:AI-Donor-Matcher/1.0}")
    private String nominatimUserAgent;

    public GeocodedPoint geocode(String address) {
        if (address == null || address.isBlank()) {
            throw new RuntimeException("Address is required for geocoding.");
        }

        if (!"nominatim".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("Unsupported geocoding provider: " + provider);
        }

        String normalizedAddress = address.trim().replaceAll("\\s+", " ");

        try {
            for (String candidate : buildCandidateQueries(normalizedAddress)) {
                Optional<GeocodedPoint> point = geocodeExact(candidate);
                if (point.isPresent()) {
                    return point.get();
                }
            }
            throw new RuntimeException("Unable to geocode the provided NGO address. Please enter a more specific address.");
        } catch (RestClientException ex) {
            throw new RuntimeException("Address geocoding is temporarily unavailable. Please try again.");
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Unable to geocode the provided NGO address. Please enter a more specific address.");
        }
    }

    private Optional<GeocodedPoint> geocodeExact(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = nominatimBaseUrl + "/search?q=" + encoded + "&format=json&limit=1";

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, nominatimUserAgent);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<Map<String, Object>> body = response.getBody();
            if (body == null || body.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> result = body.get(0);
            return Optional.of(new GeocodedPoint(
                    Double.parseDouble(result.get("lat").toString()),
                    Double.parseDouble(result.get("lon").toString())
            ));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Unable to geocode the provided NGO address. Please enter a more specific address.");
        }
    }

    private List<String> buildCandidateQueries(String normalizedAddress) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalizedAddress);

        String[] rawParts = normalizedAddress.split(",");
        List<String> parts = java.util.Arrays.stream(rawParts)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();

        for (int index = 1; index < parts.size(); index++) {
            String broaderQuery = String.join(", ", parts.subList(index, parts.size()));
            if (!broaderQuery.isBlank()) {
                candidates.add(broaderQuery);
            }
        }

        return List.copyOf(candidates);
    }

    public record GeocodedPoint(Double lat, Double lng) {
    }
}
