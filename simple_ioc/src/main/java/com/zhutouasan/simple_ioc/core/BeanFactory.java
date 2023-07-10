package com.zhutouasan.simple_ioc.core;

/**
 * @desc： bean工厂接口
 * @author: zhutouasan
 * @date： 2023/7/8 16:45
 */
public interface BeanFactory {
    Object getBean(String beanId);
}
