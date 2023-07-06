package com.zhutouasan.simple_ioc.core;

import com.zhutouasan.simple_ioc.bean.BeanDefinition;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @desc： spring ioc容器，用来创建类，管理类与类之间的关系
 * @author: zhutouasan
 * @date： 2023/7/6 15:33
 */
public class BeanContainer {

    // 使用Map存放所有的BeanDefinition, String为类路径classpapth。
    // ConcurrentHashMap保证线程安全
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

    public BeanContainer(String configFile) {
        loadBeanDefinitions(configFile);
    }

    /**
         * @Auther zhutouasan
         * @Desc 将xml文件中对bean的描述，转换成具体的对象并存放到beanDefinitionMap
         * @Date 2023/7/6 16:27
         * @param configFile xml文件的路径
         * @Return
    **/
    private void loadBeanDefinitions(String configFile) {
        InputStream inputStream = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            inputStream = classLoader.getResourceAsStream(configFile);

            SAXReader reader = new SAXReader();
            Document doc = reader.read(inputStream);
            // <beans>
            Element root = doc.getRootElement();
            Iterator<Element> iterator = root.elementIterator();
            while (iterator.hasNext()) {
                Element next = iterator.next();
                String id = next.attributeValue("id");
                String beanClassName = next.attributeValue("class");
                BeanDefinition beanDefinition = new BeanDefinition(id, beanClassName);
                this.beanDefinitionMap.put(id, beanDefinition);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
         * @Auther zhutouasan
         * @Desc  获取bean属性对象
         * @Date 2023/7/6 16:53
         * @param beanId
         * @Return {@link BeanDefinition}
    **/
    public BeanDefinition getBeanDefinition(String beanId) {
        return this.beanDefinitionMap.get(beanId);
    }

    /**
         * @Auther zhutouasan
         * @Desc  创建实例对象
         * @Date 2023/7/6 17:35
         * @param beanId
         * @Return {@link Object}
    **/
    public Object getBean(String beanId) {
        BeanDefinition beanDefinition = this.getBeanDefinition(beanId);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String classPath = beanDefinition.getBeanClassName();

        try {
            // 通过类加载器，根据classPath得到类对象
            Class<?> clazz = classLoader.loadClass(classPath);

            // 通过类对象利用反射创建Bean实例
            return clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

}
