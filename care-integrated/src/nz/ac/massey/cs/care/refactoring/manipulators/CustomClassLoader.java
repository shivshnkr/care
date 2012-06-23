package nz.ac.massey.cs.care.refactoring.manipulators;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
public class CustomClassLoader extends ClassLoader {
    public CustomClassLoader(){
        super(CustomClassLoader.class.getClassLoader());
    }
  
    public Class loadClass(String className) throws ClassNotFoundException {
         return findClass(className);
    }
 
//    public Class findClass(String className){
//        byte classByte[];
//        Class result=null;
//        result = (Class)classes.get(className);
//        if(result != null){
//            return result;
//        }
//        
//        try{
//            return findSystemClass(className);
//        }catch(Exception e){
//        	e.printStackTrace();
//        }
//        try{
//           String classPath =    ((String)ClassLoader.getSystemResource(className.replace('.',File.separatorChar)+".class").getFile()).substring(1);
//           classByte = loadClassData(classPath);
//            result = defineClass(className,classByte,0,classByte.length,null);
//            classes.put(className,result);
//            return result;
//        }catch(Exception e){
//        	e.printStackTrace();
//            return null;
//        } 
//    }
    
    protected Class findClass(String name) throws ClassNotFoundException
    {
      FileInputStream fi = null;

      try
      {
        System.out.println("finding class: " + name);

        String path = name.replace('.', '/');
        fi = new FileInputStream("testing/" + path + ".class");
        byte[] classBytes = new byte[fi.available()];
        fi.read(classBytes);
        definePackage(name, "", "", "", "", "", "", null);
        return defineClass(name, classBytes, 0, classBytes.length);

      }
      catch (Exception e)
      {
        throw new ClassNotFoundException(name);
      }
      finally
      {
        if ( null != fi ) 
        {
          try
          {
            fi.close();
          }
          catch (Exception e){}
        }
      }
    }
 
    private byte[] loadClassData(String className) throws IOException{
 
        File f ;
        f = new File(className);
        int size = (int)f.length();
        byte buff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        dis.readFully(buff);
        dis.close();
        return buff;
    }
 
    private Hashtable classes = new Hashtable();
    
    public static void main(String [] args) throws Exception{
        CustomClassLoader test = new CustomClassLoader();
        Class c = test.loadClass("ocl.ModelProviderClass");
        System.out.println(c.toString());
     }
}
 


