package ru.mikhailov.crptapi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final int requestLimit;
    private final Duration timeUnitDuration;
    private final Semaphore semaphore;
    private final HttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeUnitDuration = Duration.ofMillis(timeUnit.toMillis(1));
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newHttpClient();
    }

    public void createDocument(String jsonDocument, String signature) {
        Runnable task = () -> {
            try {
                if (semaphore.tryAcquire(timeUnitDuration.toMillis(), TimeUnit.MILLISECONDS)) {
                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                                .build();

                        try {
                            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                            System.out.println("Response status code: " + response.statusCode());
                            System.out.println("Response body: " + response.body());
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    } finally {
                        semaphore.release();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        new Thread(task).start();
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10); // 10 requests per second

        // Example usage
        String jsonDocument =
                "{\"description\": { \"participantInn\": \"string\" }, " +
                        "\"doc_id\": \"string\", " +
                        "\"doc_status\": \"string\", " +
                        "\"doc_type\": \"LP_INTRODUCE_GOODS\", 109 " +
                        "\"importRequest\": true, " +
                        "\"owner_inn\": \"string\", " +
                        "\"participant_inn\": \"string\", " +
                        "\"producer_inn\": \"string\", " +
                        "\"production_date\": \"2020-01-23\"," +
                        " \"production_type\": \"string\"," +
                        " \"products\": [{ \"certificate_document\": \"string\"," +
                        " \"certificate_document_date\": \"2020-01-23\", " +
                        "\"certificate_document_number\": \"string\", " +
                        "\"owner_inn\": \"string\", " +
                        "\"producer_inn\": \"string\"," +
                        " \"production_date\": \"2020-01-23\", " +
                        "\"tnved_code\": \"string\", " +
                        "\"uit_code\": \"string\", " +
                        "\"uitu_code\": \"string\" } ], " +
                        "\"reg_date\": \"2020-01-23\", " +
                        "\"reg_number\": \"string\"}  ";

        String signature = "YourSignatureHere";

        api.createDocument(jsonDocument, signature);
    }
}

