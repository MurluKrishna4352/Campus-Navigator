import com.google.gson.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CampusNavigatorWithGeoJSON {

    public static void main(String[] args) {
        Graph campusGraph = new Graph();
        List<List<double[]>> walls = parseGeoJSON("data/campus.geojson", campusGraph);

        System.out.println("\nCampus Navigator Initialized!");
        campusGraph.printLocations();

        Scanner scanner = new Scanner(System.in);

        System.out.print("\nEnter the starting location: ");
        String start = scanner.nextLine();
        System.out.print("Enter the destination: ");
        String end = scanner.nextLine();

        try {
            List<Node> path = campusGraph.aStar(start, end, walls);
            System.out.println("\nDirections:");
            if (path == null || path.isEmpty()) {
                System.out.println("No valid path found.");
            } else {
                List<String> directions = campusGraph.generateDetailedDirections(path);
                for (int i = 0; i < directions.size(); i++) {
                    System.out.println((i + 1) + ". " + directions.get(i));
                }
                System.out.println("\nEnjoy your journey!");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static List<List<double[]>> parseGeoJSON(String filePath, Graph graph) {
        List<List<double[]>> walls = new ArrayList<>();
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray features = jsonObject.getAsJsonArray("features");

            for (JsonElement feature : features) {
                JsonObject geometry = feature.getAsJsonObject().getAsJsonObject("geometry");
                JsonObject properties = feature.getAsJsonObject().getAsJsonObject("properties");
                String type = geometry.get("type").getAsString();

                if (type.equals("Polygon") && properties.has("name")) {
                    String name = properties.get("name").getAsString();
                    JsonArray coordinates = geometry.get("coordinates").getAsJsonArray().get(0).getAsJsonArray();
                    JsonArray firstCoord = coordinates.get(0).getAsJsonArray();
                    double lon = firstCoord.get(0).getAsDouble();
                    double lat = firstCoord.get(1).getAsDouble();

                    String landmark = properties.has("landmark") ? properties.get("landmark").getAsString() : null;

                    graph.addNode(name, lat, lon, landmark);
                } else if (type.equals("LineString")) {
                    JsonArray coordinates = geometry.get("coordinates").getAsJsonArray();
                    List<double[]> wall = new ArrayList<>();

                    for (JsonElement coord : coordinates) {
                        JsonArray point = coord.getAsJsonArray();
                        wall.add(new double[]{point.get(0).getAsDouble(), point.get(1).getAsDouble()});
                    }
                    walls.add(wall);
                }
            }

            graph.connectAllNodes();

        } catch (IOException e) {
            System.err.println("Error reading GeoJSON file: " + e.getMessage());
        }
        return walls;
    }
}

class Graph {
    Map<String, Node> nodes;

    public Graph() {
        nodes = new HashMap<>();
    }

    public void addNode(String name, double lat, double lon, String landmark) {
        nodes.putIfAbsent(name, new Node(name, lat, lon, landmark));
    }

    public void connectAllNodes() {
        for (Node nodeA : nodes.values()) {
            for (Node nodeB : nodes.values()) {
                if (!nodeA.equals(nodeB)) {
                    double distance = nodeA.distanceTo(nodeB);
                    addEdge(nodeA.name, nodeB.name, distance);
                }
            }
        }
        System.out.println("All nodes are fully connected.");
    }

    public void addEdge(String from, String to, double cost) {
        Node fromNode = nodes.get(from);
        Node toNode = nodes.get(to);
        if (fromNode != null && toNode != null) {
            fromNode.addEdge(toNode, cost);
            toNode.addEdge(fromNode, cost);
        }
    }

    public void printLocations() {
        System.out.println("Available Locations:");
        for (String location : nodes.keySet()) {
            System.out.println("- " + location);
        }
    }

    public List<Node> aStar(String start, String end, List<List<double[]>> walls) throws Exception {
        Node startNode = nodes.get(start);
        Node endNode = nodes.get(end);
        if (startNode == null || endNode == null) {
            throw new Exception("Invalid start or end location.");
        }

        PriorityQueue<NodeEntry> frontier = new PriorityQueue<>(Comparator.comparingDouble(e -> e.priority));
        frontier.add(new NodeEntry(startNode, 0));

        Map<Node, Node> cameFrom = new HashMap<>();
        Map<Node, Double> costSoFar = new HashMap<>();
        cameFrom.put(startNode, null);
        costSoFar.put(startNode, 0.0);

        while (!frontier.isEmpty()) {
            Node current = frontier.poll().node;

            if (current.equals(endNode)) break;

            for (Edge edge : current.edges) {
                Node neighbor = edge.to;
                double newCost = costSoFar.get(current) + edge.cost;

                if (crossesWall(current, neighbor, walls)) continue;

                if (!costSoFar.containsKey(neighbor) || newCost < costSoFar.get(neighbor)) {
                    costSoFar.put(neighbor, newCost);
                    double priority = newCost + neighbor.distanceTo(endNode);
                    frontier.add(new NodeEntry(neighbor, priority));
                    cameFrom.put(neighbor, current);
                }
            }
        }

        List<Node> path = new ArrayList<>();
        Node current = endNode;

        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);

        if (path.size() < 2) throw new Exception("No path found.");
        return path;
    }

    private boolean crossesWall(Node current, Node neighbor, List<List<double[]>> walls) {
        for (List<double[]> wall : walls) {
            for (int i = 0; i < wall.size() - 1; i++) {
                if (linesIntersect(current.latitude, current.longitude, neighbor.latitude, neighbor.longitude,
                                   wall.get(i)[1], wall.get(i)[0], wall.get(i + 1)[1], wall.get(i + 1)[0])) {
                    System.out.println("Blocked edge: " + current.name + " -> " + neighbor.name);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean linesIntersect(double x1, double y1, double x2, double y2,
                                   double x3, double y3, double x4, double y4) {
        double den = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (den == 0) return false; // Lines are parallel
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / den;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / den;
        return ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1;
    }

    public List<String> generateDetailedDirections(List<Node> path) {
        List<String> directions = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            Node current = path.get(i);
            Node next = path.get(i + 1);

            String movement = "From " + current.name + " move to " + next.name;

            double distance = current.distanceTo(next) * 111139; // Convert degrees to meters
            movement += " for " + Math.round(distance) + " meters";

            if (current.landmark != null) {
                movement += ", passing by " + current.landmark;
            }

            if (i < path.size() - 2) {
                Node afterNext = path.get(i + 2);
                movement += ", then " + getTurn(current, next, afterNext);
            }

            directions.add(movement);
        }
        return directions;
    }

    private String getTurn(Node prev, Node current, Node next) {
        double angle = calculateAngle(prev.latitude, prev.longitude, current.latitude, current.longitude,
                                      next.latitude, next.longitude);

        if (angle > -45 && angle < 45) return "keep moving forward";
        if (angle >= 45 && angle <= 135) return "take the first right";
        if (angle <= -45 && angle >= -135) return "take the first left";
        return "make a U-turn";
    }

    private double calculateAngle(double x1, double y1, double x2, double y2, double x3, double y3) {
        double angle1 = Math.atan2(y2 - y1, x2 - x1);
        double angle2 = Math.atan2(y3 - y2, x3 - x2);
        return Math.toDegrees(angle2 - angle1);
    }
}

class Node {
    String name;
    double latitude;
    double longitude;
    String landmark;
    List<Edge> edges;

    public Node(String name, double lat, double lon, String landmark) {
        this.name = name;
        this.latitude = lat;
        this.longitude = lon;
        this.landmark = landmark;
        this.edges = new ArrayList<>();
    }

    public void addEdge(Node to, double cost) {
        edges.add(new Edge(this, to, cost));
    }

    public double distanceTo(Node other) {
        double dLat = this.latitude - other.latitude;
        double dLon = this.longitude - other.longitude;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}

class Edge {
    Node from;
    Node to;
    double cost;

    public Edge(Node from, Node to, double cost) {
        this.from = from;
        this.to = to;
        this.cost = cost;
    }
}

class NodeEntry {
    Node node;
    double priority;

    public NodeEntry(Node node, double priority) {
        this.node = node;
        this.priority = priority;
    }
}