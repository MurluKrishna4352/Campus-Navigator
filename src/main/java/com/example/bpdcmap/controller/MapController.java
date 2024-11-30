package com.example.bpdcmap.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.bpdcmap.service.MapService;

@Controller
public class MapController {

    @Autowired
    private MapService mapService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/geojson")
    @ResponseBody
    public String getGeoJson() {
        return mapService.getGeoJson();
    }

    @GetMapping("/path")
    @ResponseBody
    public List<List<Double>> getPath(@RequestParam String start, @RequestParam String end) {
        return mapService.findPath(start, end);
    }
}