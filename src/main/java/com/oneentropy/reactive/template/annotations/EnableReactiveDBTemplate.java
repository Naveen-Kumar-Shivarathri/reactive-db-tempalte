package com.oneentropy.reactive.template.annotations;

import com.oneentropy.reactive.template.conf.ConnectionsConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ConnectionsConfig.class})
public @interface EnableReactiveDBTemplate {
}
