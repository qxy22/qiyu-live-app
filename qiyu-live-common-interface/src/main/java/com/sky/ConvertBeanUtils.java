package com.sky;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanInstantiationException;

import java.util.ArrayList;
import java.util.List;

public class ConvertBeanUtils {


    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T t = newInstance(targetClass);
        BeanUtils.copyProperties(source, t);
        return t;
    }

    public static <K, T> List<T> convertList(List<K> sourceList, Class<T> targetClass) {
        if (sourceList == null) {
            return null;
        }
        List targetList = new ArrayList((int)(sourceList.size()/0.75) + 1);
        for (K source : sourceList) {
            targetList.add(convert(source, targetClass));
        }
        return targetList;
    }

    private static <T> T newInstance(Class<T> targetClass) {
        if (targetClass == null) {
            throw new IllegalArgumentException("targetClass cannot be null");
        }
        try {
            // 兼容 Java 8+，替代已废弃的 Class.newInstance()
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new BeanInstantiationException(targetClass, "instantiation error", e);
        }
    }

}
