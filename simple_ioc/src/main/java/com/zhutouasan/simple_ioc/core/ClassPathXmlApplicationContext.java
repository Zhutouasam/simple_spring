package com.zhutouasan.simple_ioc.core;

import com.zhutouasan.simple_ioc.bean.BeanDefinition;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @desc： Ioc容器，支持即时加载，自己创建和管理资源对象，支持基于依赖的注解
 *          优点：所有的Bean在启动的时候都进行了加载，系统运行的速度快，
 *              在系统启动的时候，可以发现系统中的配置问题
 *          缺点：把费时的操作放在系统启动中完成，所有的对象都可以预加载，缺点就是内存占用较大
 * @author: zhutouasan
 * @date： 2023/7/8 17:10
 */
public class ClassPathXmlApplicationContext implements BeanFactory {

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

    // 使用ConcurrentHashMap存放所有单例Bean，String为beanId
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(64);

    public ClassPathXmlApplicationContext(String configFile) {
        loadBeanDefinitions(configFile);
        prepareBeanRegister();
    }

    /**
         * @Auther zhutouasan
         * @Desc  第一次加载就注入所有bean到容器
         * @Date 2023/7/8 17:19
         * @param
         * @Return
    **/
    private void prepareBeanRegister() {
        for (String beanId : beanDefinitionMap.keySet()) {
            this.getBean(beanId);
        }
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
                parseConstructorArgElement(next, beanDefinition);
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
     * @Auther zhutouasan
     * @Desc  获取构造方法的参数名
     * @Date 2023/7/7 12:24
     * @param beanElem 标签信息
     @param beanDefinition 类信息类
      * @Return
     **/
    public void parseConstructorArgElement(Element beanElem, BeanDefinition beanDefinition) {
        Iterator<Element> iterator = beanElem.elementIterator("constructor-arg");
        while (iterator.hasNext()) {
            Element next = iterator.next();
            String argumentName = next.attributeValue("ref");
            if (!StringUtils.hasLength(argumentName)) {
                return;
            }

            beanDefinition.getConstructorArguments().add(argumentName);
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
                        Object propertyBean = getBean(propertyName);

                        // 通过反射执行调用setter()方法
                        method.invoke(bean, propertyBean);
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
     * @Auther zhutouasan
     * @Desc  通过构造方法实现依赖注入
     * @Date 2023/7/7 15:27
     * @param beanDefinition
     * @Return {@link Object}
     **/
    private Object autowireConstructor(final BeanDefinition beanDefinition) {

        // 代表最终匹配的Constructor对象
        Constructor<?> constructor = null;
        // 代表将依赖注入的对象
        Object[] args = null;

        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(beanDefinition.getBeanClassName());

            // 通过反射回去当前类的所有Constructor对象
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (int i = 0; i < constructors.length; i++) {
                Class<?>[] parameterTypes = constructors[i].getParameterTypes();
                if (parameterTypes.length != beanDefinition.getConstructorArguments().size()) {
                    continue;
                }

                // 参数数量与扫描到的构造器参数数量相同
                // 设置构造方法参数实例
                args = new Object[parameterTypes.length];
                valuesMatchTypes(beanDefinition.getConstructorArguments(), args);
                constructor = constructors[i];
                break;
            }

            // 使用带有参数的构造方法对象实现实例化Bean
            return constructor.newInstance(args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @Auther zhutouasan
     * @Desc  给每个属性赋值
     * @Date 2023/7/7 17:22
     * @param constructorArgs 构造器参数
     @param args 实参
      * @Return
     **/
    private void valuesMatchTypes(List<String> constructorArgs, Object[] args) {
        for (int i = 0; i < constructorArgs.size(); i++) {
            Object argBean = getBean(constructorArgs.get(i));
            args[i] = argBean;
        }
    }

    /**
     * @Auther zhutouasan
     * @Desc  实例化Bean
     * @Date 2023/7/6 17:35
     * @param beanDefinition
     * @Return {@link Object}
     **/
    public Object createBeanInstance(BeanDefinition beanDefinition) {

        // 使用构造方法创建对象
        if (beanDefinition.hasConstructorArgumentValues()) {
            return autowireConstructor(beanDefinition);

            // 使用setter创建对象
        } else {
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
