package degif;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author Crimson
 */
public class Load {
    private URLClassLoader classLoader;
    private Class classToLoad;
    //private Constructor constructor;
    private Method[] methods;
    private Method method;
    private Object instance;
    
    public Load(String patch, String class_name, String method_name, Class type) {

        try {
            File jar = new File(patch);
            URL[] urls = new URL[]{jar.toURI().toURL()};
            classLoader = new URLClassLoader(urls, getClass().getClassLoader());
            classToLoad = classLoader.loadClass(class_name);
            //constructor = classToLoad.getConstructor(); // OK
            methods = classToLoad.getMethods();
            for (Method mt : methods) {
                System.out.println(mt.getName()+"("+mt.getParameterTypes().toString()+")");
            }
            method = methods[0];
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
    public Object invoke(Object params) {
        try {
            instance = method.invoke(classLoader,params);
            String ret = instance.toString();
            //System.out.println(ret);
            return ret;
        } catch (Exception ex) {
            //ex.printStackTrace();
            return null;
        }
    }
    
    
}
