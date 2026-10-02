package kd.address.view.service;

import kd.address.view.common.NotFoundException;
import kd.address.view.entity.ExternalSource;
import kd.address.view.mapper.ExternalSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalSourceService {
    private final ExternalSourceMapper sources;

    public List<ExternalSource> list() {
        return sources.findAll();
    }

    public ExternalSource get(Long id) {
        ExternalSource source = sources.findById(id);
        if (source == null) throw new NotFoundException("来源不存在");
        return source;
    }

    public ExternalSource save(ExternalSource input) {
        if (input == null) throw new IllegalArgumentException("来源配置不能为空");
        String type = trim(input.getSourceType());
        if (!"github_search".equals(type) && !"rss".equals(type))
            throw new IllegalArgumentException("不支持的来源类型");
        String name = trim(input.getDisplayName());
        if (name.isBlank() || name.length() > 80) throw new IllegalArgumentException("来源名称长度应为 1–80 字");
        int count = bounded(input.getMaxItems(), 1, 10, "每次收录数量");
        int hours = bounded(input.getIntervalHours(), 1, 720, "更新间隔");
        ExternalSource source = input.getId() == null ? new ExternalSource() : get(input.getId());
        source.setDisplayName(name);
        source.setSourceType(type);
        source.setMaxItems(count);
        source.setIntervalHours(hours);
        source.setEnabled(!Boolean.FALSE.equals(input.getEnabled()));
        if ("rss".equals(type)) {
            source.setFeedUrl(PublicFeedUrl.parse(input.getFeedUrl()).toString());
            source.setQueryText(null);
            source.setPeriodDays(7);
            source.setMinStars(0);
        } else {
            String query = trim(input.getQueryText());
            if (query.length() > 100) throw new IllegalArgumentException("搜索条件最多 100 字");
            source.setFeedUrl(null);
            source.setQueryText(query);
            source.setPeriodDays(bounded(input.getPeriodDays(), 1, 30, "检索天数"));
            source.setMinStars(bounded(input.getMinStars(), 0, 1_000_000, "最低星标数"));
        }
        if (source.getId() == null) {
            source.setCode("src-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            sources.insert(source);
        } else if (sources.update(source) == 0) {
            throw new NotFoundException("来源不存在");
        }
        return source;
    }

    public void delete(Long id) {
        if (sources.softDelete(id) == 0) throw new NotFoundException("来源不存在");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static int bounded(Integer value, int min, int max, String label) {
        if (value == null || value < min || value > max)
            throw new IllegalArgumentException(label + "应在 " + min + "–" + max + " 之间");
        return value;
    }
}
