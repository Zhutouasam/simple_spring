package com.zhutouasan.simple_ioc.bean;

import lombok.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @desc： 类的基本结构，用来描述需要ioc容器管理的对象
 * @author: zhutouasan
 * @date： 2023/7/6 12:13
 */
@Data
@ToString
@Setter
@Getter
public class BeanDefinition {

    // 对象名
    private String id;

    // 类路径
    private String beanClassName;

    // 存放Bean所有属性的名称，用于获取Bean
    private final List<String> propertyNames = new ArrayList<>();

    // 存放Bean构造器所需属性的参数
    private final List<String> constructorArguments = new LinkedList<>();

    public BeanDefinition(String id, String beanClassName) {
        this.id = id;
        this.beanClassName = beanClassName;
    }

    public boolean hasConstructorArgumentValues() {
        return !this.constructorArguments.isEmpty();
    }

}