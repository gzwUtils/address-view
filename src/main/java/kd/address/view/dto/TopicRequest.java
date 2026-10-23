package kd.address.view.dto;

public record TopicRequest(String boardCode, Long projectId, Long externalProjectId,
                           String contentType, String title, String body) {
    public TopicRequest(String boardCode, Long projectId, String title, String body) {
        this(boardCode, projectId, null, "discussion", title, body);
    }
}
