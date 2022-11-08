package com.oneentropy.reactive.template.model;

import com.oneentropy.reactive.template.util.TemplateUtil;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionData {

    private String connName;
    private String sql;
    private Object[] params;
    private Map<String, Object> namedParams;

    public boolean isNamedParamsExecution(){
        return TemplateUtil.hasContent(this.namedParams);
    }

}
