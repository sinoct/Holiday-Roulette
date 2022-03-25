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
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            if (query == null) {
                t.sendResponseHeaders(422, "Employee name is needed".length());
                OutputStream os = t.getResponseBody();
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
                t.sendResponseHeaders(422, "Employee name is needed".length());
                OutputStream os = t.getResponseBody();
                os.write("Employee name is needed".getBytes());
                os.close();
                return;
            }

            Employee currentEmployee = employeeMap.get(employeeParam);
            if (currentEmployee == null) {
                t.sendResponseHeaders(422, "Employee not found".length());
                OutputStream os = t.getResponseBody();
                os.write("Employee not found".getBytes());
                os.close();
                return;
            }

            validateCity("Tokyo", currentEmployee);

            JSONObject obj = new JSONObject();

            obj.put("name", currentEmployee.getName());
            obj.put("lastCity", currentEmployee.getLastVacation());
            obj.put("secondLastCity", currentEmployee.getSecondLasVacation());
            t.sendResponseHeaders(200, obj.toString().length());
            OutputStream os = t.getResponseBody();
            os.write(obj.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static double averageTemperature(JSONArray temperatureArray) {
        double sum = 0.0;
        for (int i = 0; i < temperatureArray.size(); i++) {
            sum = sum + Double.parseDouble(temperatureArray.get(i).toString());
            System.out.println(Double.parseDouble(temperatureArray.get(i).toString()));
        }
        return sum / temperatureArray.size();
    }

    static void readCities() {
        try {
            Scanner sc = new Scanner(new File(Main.class.getResource("cities.csv").getFile()));
            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split(",");
                potentialLocations.put(line[0], new Location(line[0], line[1] == "city" ? LocationType.CITY : LocationType.SEASIDE, Double.parseDouble(line[2]), Double.parseDouble(line[3])));
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
        JSONObject hourly = (JSONObject) jsonResponse.get("hourly");
        JSONArray array = (JSONArray) hourly.get("temperature_2m");
        double average = averageTemperature(array);
        System.out.println(average);
        return true;
    }
}
