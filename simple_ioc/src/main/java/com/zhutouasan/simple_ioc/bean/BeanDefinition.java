package com.zhutouasan.simple_ioc.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @desc： 类的基本结构，用来描述需要ioc容器管理的对象
 * @author: zhutouasan
 * @date： 2023/7/6 12:13
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class BeanDefinition {

    // 类名
    private String id;

    // 类路径
    private String beanClassName;

    public String getClassPath() {
        return this.beanClassName;
    }

}