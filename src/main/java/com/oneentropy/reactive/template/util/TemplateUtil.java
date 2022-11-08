package com.oneentropy.reactive.template.util;

import java.util.List;
import java.util.Map;

public class TemplateUtil {

    public static boolean hasContent(Object value) {
        if (value == null)
            return false;

        if (value instanceof String) {
            if (((String) value).matches("\\s") || ((String) value).equals(""))
                return false;
        } else if (value instanceof Map) {
            if (((Map) value).size() < 1)
                return false;
        }else if(value instanceof List){
            if(((List) value).size()<1)
                return false;
        }
        return true;
    }

}
