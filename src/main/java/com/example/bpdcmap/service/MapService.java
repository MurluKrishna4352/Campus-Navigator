package com.example.bpdcmap.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MapService {

    public String getGeoJson() {
        try {
            return new String(Files.readAllBytes(Paths.get("src/main/resources/completemapping.geojson")));
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public List<List<Double>> findPath(String start, String end) {
        List<List<Double>> pathCoordinates = new ArrayList<>();
        
        try {
            String geoJsonContent = getGeoJson();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(geoJsonContent);
            
            Map<String, List<Double>> buildings = new HashMap<>();
            List<List<List<Double>>> paths = new ArrayList<>();
            
            // Extract building coordinates and paths from GeoJSON
            for (JsonNode feature : root.get("features")) {
                if (feature.get("geometry").get("type").asText().equals("Polygon")) {
                    String name = feature.get("properties").get("name").asText();
                    List<Double> center = calculateCenter(feature.get("geometry").get("coordinates").get(0));
                    buildings.put(name, center);
                } else if (feature.get("geometry").get("type").asText().equals("LineString")) {
                    List<List<Double>> coordinates = new ArrayList<>();
                    for (JsonNode coord : feature.get("geometry").get("coordinates")) {
                        List<Double> point = new ArrayList<>();
                        point.add(coord.get(0).asDouble());
                        point.add(coord.get(1).asDouble());
                        coordinates.add(point);
                    }
                    paths.add(coordinates);
                }
            }

            // Here you could implement a more complex pathfinding algorithm
            // For simplicity, we will just return the first LineString found
            // In a real application, you would find the appropriate path based on start and end points
            if (!paths.isEmpty()) {
                pathCoordinates = paths.get(0); // Return the first path for demonstration
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return pathCoordinates;
    }

    private List<Double> calculateCenter(JsonNode coordinates) {
        double sumLat = 0, sumLon = 0;
        int count = coordinates.size();
        for (JsonNode coord : coordinates) {
            sumLon += coord.get(0).asDouble();
            sumLat += coord.get(1).asDouble();
        }
        return List.of(sumLon / count, sumLat / count);
    }
}