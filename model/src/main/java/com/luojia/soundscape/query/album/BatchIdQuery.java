package com.luojia.soundscape.query.album;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchIdQuery implements Serializable {

    @NotEmpty(message = "ids不能为空")
    @Size(max = 100, message = "单次最多查询100个ID")
    private List<Long> ids;
}
