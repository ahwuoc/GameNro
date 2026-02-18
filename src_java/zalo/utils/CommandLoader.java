package zalo.utils;

/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */


import zalo.interfaces.Command;
import java.io.File;
import java.net.URL;
import java.util.*;

public class CommandLoader {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static final String COMMANDS_PACKAGE = "zalo.commands";
    
    public static Map<String, Command> loadCommands() {
        return loadCommandsFromPackage(COMMANDS_PACKAGE);
    }
    
    public static Map<String, Command> loadCommandsFromPackage(String packageName) {
        Map<String, Command> commands = new HashMap<>();
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            URL resource = classLoader.getResource(path);
            
            if (resource == null) {
                System.err.println("Package not found: " + packageName);
                return commands;
            }
            
            File packageDir = new File(resource.getFile());
            if (!packageDir.exists() || !packageDir.isDirectory()) {
                System.err.println("Package directory not found: " + packageName);
                return commands;
            }
            
            File[] files = packageDir.listFiles();
            if (files == null) {
                return commands;
            }
            
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = file.getName().replace(".class", "");
                    String fullClassName = packageName + "." + className;
                    
                    try {
                        Class<?> clazz = Class.forName(fullClassName);
                        if (Command.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            Command cmd = (Command) clazz.getDeclaredConstructor().newInstance();
                            String key = cmd.getName().toLowerCase();
                            commands.put(key, cmd);
                            System.out.println(className.toUpperCase() + " | LOADED");
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println(className.toUpperCase() + " | CLASS NOT FOUND: " + fullClassName);
                    } catch (Exception e) {
                        System.err.println(className.toUpperCase() + " | ERROR: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("LOAD COMMANDS FROM PACKAGE | ERROR: " + e.getMessage());
        }
        
        return commands;
    }
}

