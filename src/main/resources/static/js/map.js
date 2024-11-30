let map;
let geojsonLayer;
let buildings = [];

function initMap() {
    map = L.map('map').setView([25.13207, 55.4197599], 18);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

    fetch('/geojson')
        .then(response => response.json())
        .then(data => {
            geojsonLayer = L.geoJSON(data, {
                style: feature => ({
                    fillColor: feature.properties.fill,
                    weight: 2,
                    opacity: 1,
                    color: feature.properties.stroke,
                    fillOpacity: 0.7
                }),
                onEachFeature: (feature, layer) => {
                    if (feature.geometry.type === 'Polygon') {
                        layer.bindPopup(feature.properties.name);
                        buildings.push(feature.properties.name);
                    }
                }
            }).addTo(map);
            populateDropdowns();
        });
}

function populateDropdowns() {
    const startSelect = document.getElementById('start');
    const endSelect = document.getElementById('end');
    
    buildings.forEach(building => {
        startSelect.add(new Option(building, building));
        endSelect.add(new Option(building, building));
    });
}

document.addEventListener('DOMContentLoaded', initMap);

document.getElementById('findPath').addEventListener('click', () => {
    const start = document.getElementById('start').value;
    const end = document.getElementById('end').value;
    
    if (start && end) {
        fetch(`/path?start=${start}&end=${end}`)
            .then(response => response.json())
            .then(path => {
                if (window.currentPath) {
                    map.removeLayer(window.currentPath);
                }
                
                // Convert path to Leaflet format and add it to the map
                window.currentPath = L.polyline(path.map(coord => [coord[1], coord[0]]), { // Note: Leaflet uses [lat, lng]
                    color: 'red',
                    weight: 3,
                    opacity: 0.7,
                    smoothFactor: 1
                }).addTo(map);
                
                map.fitBounds(window.currentPath.getBounds());
            });
    }
});