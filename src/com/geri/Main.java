package com.geri;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

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
            String response = "Hello";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
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
}
