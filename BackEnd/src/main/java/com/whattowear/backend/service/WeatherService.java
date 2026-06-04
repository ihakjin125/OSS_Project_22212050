package com.whattowear.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whattowear.backend.domain.WeatherData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class WeatherService {

    @Value("${weather.api.key}")
    private String weatherApiKey;

    @Value("${weather.api.url}")
    private String weatherApiUrl;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    @Value("${kakao.api.url}")
    private String kakaoApiUrl;

    public Map<String, Double> getCoordinates(String address) {
        Map<String, Double> coordinates = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            URI uri = UriComponentsBuilder.fromUriString(kakaoApiUrl)
                    .queryParam("query", address)
                    .build()
                    .encode()
                    .toUri();

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode documents = root.path("documents");

            if (documents.isArray() && documents.size() > 0) {
                JsonNode location = documents.get(0);
                coordinates.put("x", location.path("x").asDouble());
                coordinates.put("y", location.path("y").asDouble());
                System.out.println("✅ 카카오 변환 성공: X=" + coordinates.get("x") + ", Y=" + coordinates.get("y"));
            }
        } catch (Exception e) {
            System.out.println("❌ 카카오 에러: " + e.getMessage());
        }
        return coordinates;
    }

    private Map<String, Integer> convertToGrid(double lat, double lon) {
        double RE = 6371.00877, GRID = 5.0, SLAT1 = 30.0, SLAT2 = 60.0;
        double OLON = 126.0, OLAT = 38.0, XO = 43, YO = 136;
        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + (lat * DEGRAD) * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        Map<String, Integer> grid = new HashMap<>();
        grid.put("nx", (int) Math.floor(ra * Math.sin(theta) + XO + 0.5));
        grid.put("ny", (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5));

        return grid;
    }

    public WeatherData getCurrentWeather(String locationName) {
        WeatherData weatherData = new WeatherData();

        try {
            Map<String, Double> coords = getCoordinates(locationName);
            if(coords.isEmpty()) {
                coords.put("x", 128.56888);
                coords.put("y", 35.84004);
            }

            Map<String, Integer> grid = convertToGrid(coords.get("y"), coords.get("x"));
            int nx = grid.get("nx");
            int ny = grid.get("ny");

            LocalDateTime now = LocalDateTime.now();
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = now.minusHours(1).format(DateTimeFormatter.ofPattern("HH00"));

            URI uri = UriComponentsBuilder.fromUriString(weatherApiUrl)
                    .queryParam("serviceKey", weatherApiKey)
                    .queryParam("pageNo", "1")
                    .queryParam("numOfRows", "10")
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build(true)
                    .toUri();

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(uri, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode items = objectMapper.readTree(response).path("response").path("body").path("items").path("item");

            double currentTemp = 0.0;
            int ptyCode = 0; // 🌟 추가: 강수형태 코드 저장용 변수

            for (JsonNode item : items) {
                String category = item.path("category").asText();
                if ("T1H".equals(category)) {
                    currentTemp = item.path("obsrValue").asDouble();
                } else if ("PTY".equals(category)) {
                    ptyCode = item.path("obsrValue").asInt(); // 🌟 강수형태 데이터 추출
                }
            }

            // 🌟 PTY 코드에 따라 이모지 결정
            String weatherIcon = "☁️"; // 기본값 (강수 없음)
            if (ptyCode == 1 || ptyCode == 4 || ptyCode == 5) {
                weatherIcon = "☔"; // 비, 소나기 등
            } else if (ptyCode == 2 || ptyCode == 6) {
                weatherIcon = "🌨️"; // 비 섞인 눈
            } else if (ptyCode == 3 || ptyCode == 7) {
                weatherIcon = "⛄"; // 눈
            }

            weatherData.setCurrentTemp(currentTemp);
            weatherData.setSkyStatus(weatherIcon); // 🌟 결정된 이모지를 데이터에 쏙 담아줍니다!

            weatherData.setMaxTemp(currentTemp + 5.0);
            weatherData.setMinTemp(currentTemp - 5.0);
            weatherData.setPop(0);

        } catch (Exception e) {
            System.out.println("❌ 에러: " + e.getMessage());
            weatherData.setCurrentTemp(22.5);
            weatherData.setSkyStatus("☁️"); // 에러 났을 때 기본 아이콘
        }

        return weatherData;
    }
}