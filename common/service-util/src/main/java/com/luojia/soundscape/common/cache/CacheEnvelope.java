package com.luojia.soundscape.common.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheEnvelope implements Serializable {
    private Object data;
    private boolean nullValue;
    private long freshUntil;
    private long staleUntil;

    public boolean isFresh(long now) {
        return now < freshUntil;
    }

    public boolean isUsable(long now) {
        return now < staleUntil;
    }

    public Object unwrap() {
        return nullValue ? null : data;
    }
}
