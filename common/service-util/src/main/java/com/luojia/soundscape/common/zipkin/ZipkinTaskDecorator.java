package com.luojia.soundscape.common.zipkin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;

/**

 * @Author: chenyangu


 * @Description: zipkin装饰器

 */

@Slf4j
public class ZipkinTaskDecorator implements TaskDecorator {

    private ZipkinHelper zipkinHelper;

    public ZipkinTaskDecorator(ZipkinHelper zipkinHelper) {
        this.zipkinHelper = zipkinHelper;

    }

    @Override

    public Runnable decorate(Runnable runnable) {

        return zipkinHelper.wrap(runnable);

    }

}
