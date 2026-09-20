package com.courseplatform.backend.ai;

import com.courseplatform.backend.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class AiWorkerClient {
    private final RestClient client;
    private final String token;

    public AiWorkerClient(RestClient.Builder builder,
                          @Value("${app.ai.worker-url}") String workerUrl,
                          @Value("${app.ai.worker-token}") String token) {
        this.client = builder.baseUrl(workerUrl).build();
        this.token = token;
    }

    public WorkerChatResponse chat(long courseId, Long chapterId, Long resourceId, String question) {
        try {
            WorkerChatResponse response = client.post().uri("/chat")
                    .header("X-Worker-Token", token)
                    .body(new WorkerChatRequest(courseId, chapterId, resourceId, question))
                    .retrieve().body(WorkerChatResponse.class);
            if (response == null) {
                throw unavailable(null);
            }
            return response;
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    private BusinessException unavailable(Exception cause) {
        BusinessException exception = new BusinessException(50370, "AI 问答服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        if (cause != null) exception.initCause(cause);
        return exception;
    }

    record WorkerChatRequest(long courseId, Long chapterId, Long resourceId, String question) {
    }

    public record WorkerChatResponse(String answer, List<AiSourceResponse> sources) {
    }
}
