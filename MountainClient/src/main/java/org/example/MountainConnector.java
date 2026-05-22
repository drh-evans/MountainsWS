package org.example;

// imports
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MountainConnector {
    // constants and global variables
    private static final String BY_CONTINENT_AND_COUNTRY = "%s/%s";
    private static final String BY_NAME = "%s/%s/%s";
    private static final String BY_ALTITUDE = "?altitude=%d";
    private static final String BY_ID = "?id=%d";
    private static final String RESOURCE = "resource/";
    private static final int NOT_FOUND = 404;
    private static final int OK = 200;
    private static final String JSON_ERROR = "Error mapping the Mountain to JSON";
    private static final String ERROR_MESSAGE = "Error!";
    private static String uriVal;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient client = HttpClient.newHttpClient();

    /**
     * A constructor for the MountainConnector class.
     * @param uri The URI value.
     */
    public MountainConnector(String uri) {
        // add resource to the URL for requests, allows swagger to work.
        uriVal = uri + RESOURCE;
    }

    /**
     * A method to add a new mountain to the server.
     * @param mountain The desired mountain to be added.
     * @return The response provided by the server.
     */
    public Optional<Response> addMountain(Mountain mountain) {
        try {
            String mountainAsJson = objectMapper.writeValueAsString(mountain);
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(uriVal))
                    .POST(HttpRequest.BodyPublishers.ofString(mountainAsJson))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
            return Optional.of(new Response(new ArrayList<>(), response));
        } catch(JsonProcessingException jme) {
            System.err.println(JSON_ERROR);
            return Optional.empty();
        } catch (Exception e) {
            System.err.println(ERROR_MESSAGE);
            return Optional.empty();
        }
    }

    /**
     * Get method to get mountains from url.
     * @param path The url of the mountains being queried.
     * @return The response of the request.
     */
    private Optional<Response> get(final String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(uriVal + path))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == OK) {
                List<Mountain> mountainsList = objectMapper.readValue(response.body(),
                        new TypeReference<>() {});
                return Optional.of(new Response(mountainsList, response));
            } else if (response.statusCode() == NOT_FOUND) {
                return Optional.of(new Response(new ArrayList<>(), response));
            } else {
                return Optional.empty();
            }
        } catch (IOException ioException) {
            System.err.println(ioException.getMessage());
            return Optional.empty();
        } catch (InterruptedException interruptedException) {
            System.err.println(interruptedException.getMessage());
            return Optional.empty();
        } catch (URISyntaxException uriSyntaxException) {
            System.err.println(uriSyntaxException.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * A method to get all the mountains that are present on the server.
     * @return The response provided by the server.
     */
    public Optional<Response> getAll() {
        return get("");
    }

    /**
     * Gets a mountain by its continent.
     * @param continent The mountains continent.
     * @return The results of this request.
     */
    public Optional<Response> getByContinent(final String continent) {
        return get(continent);
    }

    /**
     * Gets a mountain by its continent and country.
     * @param continent The mountains continent.
     * @param country The mountains country.
     * @return The result of this request.
     */
    public Optional<Response> getByContinentAndCountry(final String continent, final String country) {
        return get(String.format(BY_CONTINENT_AND_COUNTRY, continent, country));
    }

    /**
     * Get a mountain by its name.
     * @param continent The mountains continent.
     * @param country The mountains country.
     * @param name The mountains name.
     * @return The result of this request.
     */
    public Optional<Response> getByName(final String continent, final String country, final String name) {
        return get(String.format(BY_NAME, continent, country, name));
    }

    /**
     * Get a mountain by its ID.
     * @param id The mountains ID.
     * @return The result of this request.
     */
    public Optional<Response> getById(final int id) {
        return get(String.format(BY_ID, id));
    }

    /**
     * Get the mountains with an altitude ≥ the value.
     * @param altitude The minimum altitude.
     * @return The results of this request.
     */
    public Optional<Response> getByAltitude(final int altitude) {
        return get(String.format(BY_ALTITUDE, altitude));
    }

    /**
     * Delete a mountain based off of ID.
     * @param id The mountains ID.
     * @return The result of the delete request.
     */
    public Optional<Response> deleteMountain(final int id) {
        return delete(String.format(BY_ID, id)) ;
    }

    /**
     * The delete request method.
     * @param path The path of the mountain being deleted.
     * @return The response of the request.
     */
    private Optional<Response> delete(String path) {
        try {
            URI getUri = new URI(uriVal + path);
            HttpRequest request = HttpRequest.newBuilder().uri(getUri)
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return Optional.of(new Response(new ArrayList<>(), response));
        } catch (URISyntaxException ure) {
            System.out.println(ure.getMessage());
            return Optional.empty();
        } catch (IOException io) {
            System.out.println(io.getMessage());
            return Optional.empty();
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
            return Optional.empty();
        }
    }


     // Removes all the data from the server.
    public void zapData() {
        try {
            // use base "/" uri for deleting everything.
            URI getUri = new URI(uriVal);
            HttpRequest request = HttpRequest.newBuilder().uri(getUri)
                    .DELETE()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (URISyntaxException ure) {
            System.out.println(ure.getMessage());
        } catch (IOException io) {
            System.out.println(io.getMessage());
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }
    }
}
