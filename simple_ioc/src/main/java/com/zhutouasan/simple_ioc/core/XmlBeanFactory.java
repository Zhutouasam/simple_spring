package com.zhutouasan.simple_ioc.core;

import com.zhutouasan.simple_ioc.bean.BeanDefinition;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @desc： BeanFactory的实现类，支持懒加载，
 * 使用语法显示提供资源对象，不支持基于依赖的注解
 * 优点：应用启动的时候占用资源很少，对资源要求较高的应用，比较有优势
 * 缺点：运行速度会相对来说慢一些。而且有可能会出现空指针异常的错误
 * 而且通过Bean工厂创建的Bean生命周期会简单一点
 * @author: zhutouasan
 * @date： 2023/7/8 16:46
 */
public class XmlBeanFactory implements BeanFactory {

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

    // 使用ConcurrentHashMap存放所有单例Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(64);

    public XmlBeanFactory(String configFile) {
        loadBeanDefinitions(configFile);
    }

    /**
     * @param configFile xml文件的路径
     * @Auther zhutouasan
     * @Desc 将xml文件中对bean的描述，转换成具体的对象并存放到beanDefinitionMap
     * @Date 2023/7/6 16:27
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

            // 实例化容器中的类
            while (iterator.hasNext()) {
                Element next = iterator.next();
                String id = next.attributeValue("id");
                String beanClassName = next.attributeValue("class");
                BeanDefinition beanDefinition = new BeanDefinition(id, beanClassName);
                parsePropertyElement(next, beanDefinition);
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
     * @param beanElem       标签信息
     * @param beanDefinition 定义类信息
     * @Auther zhutouasan
     * @Desc 获取bean的所有属性名
     * @Date 2023/7/7 15:08
     * @Return
     **/
    public void parsePropertyElement(Element beanElem, BeanDefinition beanDefinition) {
        Iterator<Element> iterator = beanElem.elementIterator("property");
        while (iterator.hasNext()) {
            Element next = iterator.next();
            String propertyName = next.attributeValue("name");
            if (!StringUtils.hasLength(propertyName)) {
                return;
            }

            beanDefinition.getPropertyNames().add(propertyName);
        }
    }

    /**
     * 单例模式获取bean
     *
     * @param beanId
     * @return
     */
    @Override
    public Object getBean(String beanId) {
        BeanDefinition beanDefinition = this.getBeanDefinition(beanId);
        Object bean = this.getSingleton(beanId);
        if (bean == null) {
            bean = createBean(beanDefinition);
            this.registerSingleton(beanId, bean);
        }
        return bean;
    }

    /**
         * @Auther zhutouasan
         * @Desc  将更新的bean更新到Map中
         * @Date 2023/7/8 17:08
         * @param beanId
            @param bean
         * @Return
    **/
    private void registerSingleton(String beanId, Object bean) {
        Object object = this.singletonObjects.get(beanId);
        if (object != null) {
            System.out.println("error, " + object + "had already registered");
        }
        this.singletonObjects.put(beanId, bean);
    }

    /**
     * 创建Bean对象
     *
     * @param beanDefinition
     * @return
     */
    private Object createBean(BeanDefinition beanDefinition) {

        // bean实例化
        Object bean = createBeanInstance(beanDefinition);
        // 给bean赋值
        populateBean(beanDefinition, bean);

        return bean;
    }

    /**
         * @Auther zhutouasan
         * @Desc  使用setter给Bean赋值
         * @Date 2023/7/8 17:06
         * @param beanDefinition
            @param bean
         * @Return
    **/
    public void populateBean(BeanDefinition beanDefinition, Object bean) {

        // 获取bean的变量名
        List<String> propertyNames = beanDefinition.getPropertyNames();

        try {
            // 通过反射获取当前类所有的方法信息（使用Method）
            Method[] methods = bean.getClass().getDeclaredMethods();

            // 配对
            for (String propertyName : propertyNames) {
                for (Method method : methods) {
                    if (method.getName().equals("set" + upperCaseFirstChar(propertyName))) {

                        // 获得方法参数实例
                        Object properyBean = getBean(propertyName);

                        // 通过反射执行调用setter()方法
                        method.invoke(bean, properyBean);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @Auther zhutouasan
     * @Desc  将字符串第一个字符大写
     * @Date 2023/7/7 17:48
     * @param propertyName
     * @Return {@link String}
     **/
    private String upperCaseFirstChar(String propertyName) {
        StringBuilder sb = new StringBuilder();
        sb.append(propertyName.substring(0, 1).toUpperCase());
        sb.append(propertyName.substring(1));
        return sb.toString();
    }

    /**
     * @param beanDefinition
     * @Auther zhutouasan
     * @Desc 实例化Bean
     * @Date 2023/7/6 17:35
     * @Return {@link Object}
     **/
    public Object createBeanInstance(BeanDefinition beanDefinition) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String className = beanDefinition.getBeanClassName();

        try {
            // 通过类加载器，根据classPath得到类对象
            // 直接通过类加载器加载的类，只有将.class文件加载到jvm中，不会执行static中的内容
            // Class.forName的到的类是已经初始化完成
            Class<?> clazz = classLoader.loadClass(className);

            // 通过类对象利用反射创建Bean实例
            return clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取单例Bean
     * @param beanName
     * @return
     */
    public Object getSingleton(String beanName) {
        return this.singletonObjects.get(beanName);
    }

    /**
     * @param beanId
     * @Auther zhutouasan
     * @Desc 获取bean属性对象
     * @Date 2023/7/6 16:53
     * @Return {@link BeanDefinition}
     **/
    public BeanDefinition getBeanDefinition(String beanId) {
        return this.beanDefinitionMap.get(beanId);
    }
}
