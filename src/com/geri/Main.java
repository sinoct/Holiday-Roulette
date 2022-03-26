package com.geri;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {

    static Map<String, Location> potentialLocations = new HashMap<String, Location>();
    static Map<String, Employee> employeeMap = new HashMap<String, Employee>();
    static Location juhePuszta = new Location("Juhépuszta", LocationType.CITY, 46.413759, 18.497811);


    public static void main(String[] args) throws Exception {
        readCities();
        readEmployees();
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/employee/vacation", new MyHandler());
        server.setExecutor(null);
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query == null) {
                exchange.sendResponseHeaders(422, "Employee name is needed".length());
                OutputStream os = exchange.getResponseBody();
                os.write("Employee name is needed".getBytes());
                os.close();
                return;
            }

            Map<String, String> queryParams = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    queryParams.put(entry[0], entry[1]);
                }else{
                    queryParams.put(entry[0], "");
                }
            }

            String employeeParam = queryParams.get("employeeName");
            if (employeeParam == null) {
                exchange.sendResponseHeaders(422, "Employee name is needed".length());
                OutputStream os = exchange.getResponseBody();
                os.write("Employee name is needed".getBytes());
                os.close();
                return;
            }

            Employee currentEmployee = employeeMap.get(employeeParam);
            if (currentEmployee == null) {
                exchange.sendResponseHeaders(422, "Employee not found".length());
                OutputStream os = exchange.getResponseBody();
                os.write("Employee not found".getBytes());
                os.close();
                return;
            }

            Location nextLocation = null;
            Double longestDistance = 0.0;
            for (var entry : potentialLocations.entrySet()) {
                Location currentLocation = entry.getValue();
                Boolean isAccepted = validateCity(currentLocation.getName(), currentEmployee);
                if (isAccepted) {
                    Double distance = calculateDistance(juhePuszta.getLatitude(),
                            juhePuszta.getLongitude(),
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude());
                    if(distance > longestDistance) {
                        nextLocation = currentLocation;
                    }
                }
            }
            JSONObject responseObj = new JSONObject();

            if(nextLocation == null) {
                responseObj.put("message", "No suitable location found. Better luck next time.");
            } else {
                responseObj.put("cityName", nextLocation.getName());
            }
            exchange.sendResponseHeaders(200, responseObj.toString().length());
            OutputStream os = exchange.getResponseBody();
            os.write(responseObj.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static double averageTemperature(JSONArray temperatureArray) {
        double sum = 0.0;
        for (int i = 0; i < temperatureArray.size(); i++) {
            sum = sum + Double.parseDouble(temperatureArray.get(i).toString());
        }
        return sum / temperatureArray.size();
    }

    static void readCities() {
        try {
            Scanner sc = new Scanner(new File(Main.class.getResource("cities.csv").getFile()));
            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split(",");
                potentialLocations.put(line[0], new Location(line[0],
                        line[1] == "city" ? LocationType.CITY : LocationType.SEASIDE,
                        Double.parseDouble(line[2]),
                        Double.parseDouble(line[3])));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static void readEmployees() {
        try {
            Scanner sc = new Scanner(new File(Main.class.getResource("employees.csv").getFile()));
            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split(",");
                employeeMap.put(line[0], new Employee(line[0],line[1],line[2]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static Boolean validateCity(String cityName, Employee employee) {
        Location city = potentialLocations.get(cityName);
        if (employee.getLastVacation().equals(cityName)
                || employee.getSecondLasVacation().equals(cityName)) {
            return false;
        }
        HttpClient client = HttpClient.newHttpClient();
        String apiUrl = "https://api.open-meteo.com/v1/forecast?latitude="+city.getLatitude()+"&longitude="+city.getLongitude()+"&hourly=temperature_2m,precipitation";
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("accept", "application/json")
                .uri(URI.create(apiUrl))
                .build();
        HttpResponse<String> res = null;
        try {
            res = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JSONObject jsonResponse = null;
        try {
            jsonResponse = (JSONObject) new JSONParser().parse(res.body());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        JSONObject hourlyData = (JSONObject) jsonResponse.get("hourly");
        JSONArray tempArray = (JSONArray) hourlyData.get("temperature_2m");
        Boolean tempAccept = checkForTemperature(cityName, tempArray);
        if (!tempAccept) {
            return false;
        }
        JSONArray hourlyPrecipitation = (JSONArray) hourlyData.get("precipitation");
        Boolean precipAccept = checkForPrecipitation(hourlyPrecipitation);
        if (!precipAccept) {
            return false;
        }
        return true;
    }

    static Boolean checkForTemperature(String cityName, JSONArray tempArray) {
        Location city = potentialLocations.get(cityName);
        double averageTemp = averageTemperature(tempArray);
        if(averageTemp < 10) {
            return false;
        } else if(averageTemp > 30 && city.getType() != LocationType.SEASIDE) {
            return false;
        }
        return true;
    }

    static Boolean checkForPrecipitation(JSONArray precipArray) {
        Double[] dailyPrecipitation = new Double[7];
        for (int i = 0; i < 7; i++) {
            Double dailySum = 0.0;
            for (int j = i*24; j < i*24+24; j++) {
                dailySum = dailySum + Double.parseDouble(precipArray.get(j).toString());
            }
            dailyPrecipitation[i] = dailySum;
        }
        Integer consecutiveRainyDays = 0;
        for (int i = 0; i < 7; i++) {
            if(dailyPrecipitation[i] > 5) {
                consecutiveRainyDays++;
            } else {
                consecutiveRainyDays = 0;
            }
            if(consecutiveRainyDays > 2) {
                return false;
            }
        }
        return true;
    }

    static Double calculateDistance(Double latitude1, Double longitude1, Double latitude2, Double longitude2) {
        Double theta = longitude1 - longitude2;
        Double dist = Math.sin(Math.toRadians(latitude1)) * Math.sin(Math.toRadians(latitude2))
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 111.18957696;

        return dist;
    }
}
