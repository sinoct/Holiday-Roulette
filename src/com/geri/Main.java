package com.geri;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/employee/vacation", new MyHandler());
        server.setExecutor(null);
        Map<String, LocationType> potentialLocations = new HashMap<String, LocationType>();
        potentialLocations.put("New York", LocationType.CITY);
        potentialLocations.put("Las Vegas", LocationType.CITY);
        potentialLocations.put("Los Angeles", LocationType.SEASIDE);
        potentialLocations.put("Berlin", LocationType.CITY);
        potentialLocations.put("Tokyo", LocationType.CITY);
        potentialLocations.put("Rio de Janeiro", LocationType.SEASIDE);
        potentialLocations.put("Sydney", LocationType.SEASIDE);
        potentialLocations.put("Lusaka", LocationType.CITY);
        potentialLocations.put("Cape Town", LocationType.SEASIDE);
        potentialLocations.put("West Palm Beach", LocationType.SEASIDE);
        Employee[] employees = {
                new Employee("Teszt Elek", "Lusaka", "New York"),
        };
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



}
