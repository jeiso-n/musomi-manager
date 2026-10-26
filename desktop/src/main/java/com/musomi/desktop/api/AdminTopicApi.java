package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import com.musomi.desktop.model.TopicResponse;

public class AdminTopicApi {

    private final ApiClient client = ApiClient.getInstance();

    public List<TopicResponse> getTopics(Long subjectId) {
        TopicResponse[] topics = client.get(
                "/admin/subjects/" + subjectId + "/topics", TopicResponse[].class);
        return topics == null ? List.of() : Arrays.asList(topics);
    }

    public TopicResponse createTopic(Long subjectId, String name) {
        return createTopic(new CreateTopicRequest(subjectId, name, null, 0));
    }

    public TopicResponse createTopic(CreateTopicRequest request) {
        return client.post("/admin/subjects/" + request.subjectId() + "/topics",
                request, TopicResponse.class);
    }

    public TopicResponse updateTopic(Long subjectId, Long topicId, String name) {
        return updateTopic(topicId, new UpdateTopicRequest(subjectId, name, null, 0));
    }

    public TopicResponse updateTopic(Long topicId, UpdateTopicRequest request) {
        return client.put("/admin/subjects/" + request.subjectId() + "/topics/" + topicId,
                request, TopicResponse.class);
    }

    public void deactivateTopic(Long topicId) {
        client.put("/admin/topics/" + topicId + "/deactivate", null, Void.class);
    }

    public record CreateTopicRequest(
            Long subjectId, String name, String description, Integer sortOrder) {
    }

    public record UpdateTopicRequest(
            Long subjectId, String name, String description, Integer sortOrder) {
    }
}
