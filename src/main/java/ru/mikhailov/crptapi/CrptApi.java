package ru.mikhailov.crptapi;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final Semaphore rateLimiter;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final long waitTime;
    private final TimeUnit timeUnit;

    public CrptApi(TimeUnit timeUnit, int requestLimit, long waitTime) {
        this.rateLimiter = new Semaphore(requestLimit);
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.timeUnit = timeUnit;
        this.waitTime = waitTime;
    }

    public void createDocument(Object document, String signature) throws Exception {
        if (!rateLimiter.tryAcquire(1, timeUnit)) {
            System.out.println("Превышен лимит запросов");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(document);
            HttpPost post = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
            post.setEntity(new StringEntity(json));
            post.setHeader("Content-type", "application/json");
            post.setHeader("Signature", signature);

            httpClient.execute(post);
        } finally {
            rateLimiter.release();
        }
    }

    public static void main(String[] args) throws Exception {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5, 1); // Пример: 5 запросов в секунду
        Document demoDocument = new Document();
        demoDocument.setDocId("12345");
        demoDocument.setDocStatus("active");

        String demoSignature = "test_signature";
        api.createDocument(demoDocument, demoSignature);

        System.out.println("Документ отправлен.");
    }
}
