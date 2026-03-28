package kd.address.view.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {

    private List<T> records;
    private long total;
    private int page;
    private int size;

    public static <T> PageResponse<T> of(List<T> records, long total, int page, int size) {
        PageResponse<T> response = new PageResponse<>();
        response.setRecords(records);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }
}
