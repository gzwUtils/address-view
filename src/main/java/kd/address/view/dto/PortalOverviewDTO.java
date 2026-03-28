package kd.address.view.dto;

import lombok.Data;

import java.util.List;

@Data
public class PortalOverviewDTO {
    private List<PortalMetricDTO> metrics;
    private List<String> todayFocus;
    private List<PortalSignalCardDTO> signalCards;
    private List<PortalContentScopeDTO> contentScopes;
    private List<PortalAiCapabilityDTO> aiCapabilities;
    private List<PortalAiSceneDTO> aiScenes;
    private List<String> warmPills;
}
