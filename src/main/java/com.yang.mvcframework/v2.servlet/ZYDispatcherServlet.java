package com.yang.mvcframework.v2.servlet;

import com.yang.mvcframework.annotation.ZYAutowried;
import com.yang.mvcframework.annotation.ZYCountroller;
import com.yang.mvcframework.annotation.ZYRequestMapping;
import com.yang.mvcframework.annotation.ZYRequestParam;
import com.yang.mvcframework.annotation.ZYService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

public class ZYDispatcherServlet extends HttpServlet {
    private Properties properties = new Properties();
    //所有扫描文件名称
    private List<String> classNames = new ArrayList<String>();
    //ioc容器 beanName对应实例
    private Map<String,Object> ioc = new HashMap<String,Object>();
    //处理器映射器容器
    private Map<String, Method> handlerMapping = new HashMap<String,Method>();

    @Override
    public void init(ServletConfig config)
    {
        //1.加载配置文件
        initConfig(config.getInitParameter("contextConfigLocation"));
        //2.扫描相关的类
        initScanner((String) properties.get("scanPackage"));
        //3.初始化相关的类的实例,并且放到相关的容器中
        initInstance();
        //4.完成依赖注入
        initAutowired();
        //5.完成handerMapping
        initHanderMapping();
        System.out.println("ZYSpringMvc framework is init complete.");
    }

    /**
     * 完成handlerMapping注入
     */
    private void initHanderMapping()
    {
        if (ioc.isEmpty())
        {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet())
        {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(ZYCountroller.class)) {continue;}
            //controller上的映射路径
            String baseUrl = "";
            if (clazz.isAnnotationPresent(ZYRequestMapping.class))
            {
                ZYRequestMapping requestMapping = clazz.getAnnotation(ZYRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods)
            {
                if (!method.isAnnotationPresent(ZYRequestMapping.class)) {continue;}
                ZYRequestMapping methodMapping = method.getAnnotation(ZYRequestMapping.class);
                //类映射路径+方法映射路径 
                String url = ("/" + baseUrl + "/" + methodMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("Mapped" + url + "," + method);
            }
        }
    }

    /**
     * 完成依赖注入
     */
    private void initAutowired()
    {
        if (ioc.isEmpty())
        {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet())
        {
            //拿到实例对象中所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields)
            {
                //是否有autowired注解
                if (!field.isAnnotationPresent(ZYAutowried.class)){continue;}
                ZYAutowried autowried = field.getAnnotation(ZYAutowried.class);
                String beanName = autowried.value().trim();
                if (beanName.equals(""))
                {
                    beanName = field.getType().getName();
                }
                //强吻
                field.setAccessible(true);
                try
                {
                    //完成装配
                    field.set(entry.getValue(), ioc.get(beanName));
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化相关的类的实例,并放到相关的容器中 
     */
    private void initInstance()
    {
        if (classNames.isEmpty()) {return;}
        classNames.forEach(className -> {
            try
            {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(ZYCountroller.class))
                {
                    String beanName = toFirstLowerCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                }
                else if (clazz.isAnnotationPresent(ZYService.class))
                {
                    String beanName = toFirstLowerCase(clazz.getSimpleName());
                    ZYService service = clazz.getAnnotation(ZYService.class);
                    if (! service.value().equals(""))
                    {
                        beanName = service.value();
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    for (Class<?> inface : clazz.getInterfaces())
                    {
                        if (ioc.containsKey(""))
                        {
                            throw new RuntimeException("The beanName is Exists !!");
                        }
                        ioc.put(inface.getName(), instance);
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    /**
     * 类名首字母小写
     * @return
     */
    private String toFirstLowerCase(String simpleName)
    {
        char [] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 扫描相关的类
     */
    private void initScanner(String scannerPackage)
    {
        URL url = this.getClass().getClassLoader().getResource("/" + scannerPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles())
        {
            if (file.isDirectory())
            {
                initScanner(scannerPackage + "." + file.getName());
            }
            else
            {
                if (! file.getName().endsWith(".class")) {continue;}
                String className = (scannerPackage + "." + file.getName()).replace(".class", "");
                classNames.add(className);
            }
        }
    }

    /**
     * 加载配置文件
     */
    private void initConfig(String configFile)
    {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configFile);
        try
        {
            properties.load(inputStream);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        try
        {
            doDispath(req,resp);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            resp.getWriter().write("500 Server error." + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 请求处理方法
     * @throws IOException
     */
    private void doDispath(HttpServletRequest req, HttpServletResponse resp) throws Exception
    {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        //将多个//符号转化为一个文件目录符
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMapping.containsKey(url))
        {
            resp.getWriter().write("404 Not Found!");
            return;
        }

        Method method = handlerMapping.get(url);

        //请求参数列表
        Map<String, String[]> parameterMap = req.getParameterMap();
        //形参类型列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //形参列表
        Parameter[] parameters = method.getParameters();
        //实际参数列表
        Object [] params = new Object[parameters.length];

        for (int i = 0; i < parameterTypes.length; i++)
        {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class)
            {
                params[i] = req;
            }
            else if (parameterType == HttpServletResponse.class)
            {
                params[i] = resp;
            }
            else if (parameterType == String.class)
            {
                //获取对应的参数
                ZYRequestParam requestParam = parameters[i].getAnnotation(ZYRequestParam.class);
                if (parameterMap.containsKey(requestParam.value()))
                {
                    String[] values = parameterMap.get(requestParam.value());
                    //替换掉参数值的[]以及空格
                    params[i] = Arrays.toString(values).replaceAll("\\[|\\]", "").replaceAll("\\s", ",");
                }
            }
        }
        String beanName = toFirstLowerCase(method.getDeclaringClass().getSimpleName());
        //调用method
        method.invoke(ioc.get(beanName), params);
    }
}
