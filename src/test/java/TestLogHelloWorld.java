import org.apache.log4j.Logger;
import org.junit.Test;

public class TestLogHelloWorld {

    /**
     * log4j基础知识：
     * Log4j是Apache的开源项目一个功能强大的日志组件,提供方便的日志记录。
     * 日志记录器(Logger)是日志处理的核心组件
     * Log4j建议只使用四个级别，优先级从高到低分别是FATAL, ERROR、WARN、INFO、DEBUG。通过在这里定义的级别，
     * 您可以控制到应用程序中相应级别的日志信息的开关。比如在这里定义了INFO级别，则应用程序中所有DEBUG级别的日
     * 志信息将不被打印出来。程序会打印高于或等于所设置级别的日志，设置的日志等级越高，打印出来的日志就越少。
     * 如果设置级别为INFO，则优先级高于等于INFO级别（如：INFO、WARN、ERROR）的日志信息将可以被输出,小于该级别
     * 的如DEBUG将不会被输出。
     *
     * 下列级别从低到高:
     * DEBUG： Level指出细粒度信息事件对调试应用程序是非常有帮助的。
     * INFO： level表明 消息在粗粒度级别上突出强调应用程序的运行过程。
     * WARN： level表明会出现潜在错误的情形。
     * ERROR： level指出虽然发生错误事件，但仍然不影响系统的继续运行。
     * FATAL： level指出每个严重的错误事件将会导致应用程序的退出。
     * fatal： 致命的；重大的；毁灭性的；命中注定的
     *
     * log4j的日志级别一般是在 log4j.properties 文件中修改。
     *
     * 源码相关
     * 参考博客 ：
     * 1.http://www.blogjava.net/DLevin/archive/2012/06/12/380647.html （模拟log4j）
     * 2.http://www.blogjava.net/DLevin/archive/2012/06/28/381667.html （初步解析log4j）
     * 3.http://www.blogjava.net/DLevin/archive/2012/07/04/382131.html （解析Layout）
     * 4.http://www.blogjava.net/DLevin/archive/2012/07/10/382676.html （解析Appender）
     * 5.http://www.blogjava.net/DLevin/archive/2012/07/10/382678.html （解析LoggerRepository和Configurator）
     */

    //这行关联此类并加载了日志所有的配置，代码实现在LogManager的static {}中
    //logger继承了Category，Category有一些通用的属性设置 比如appender等等等等
    private static Logger logger = Logger.getLogger(TestLogHelloWorld.class);

    @Test
    public void logHelloWorld() {
        try {
            int a = 10 / 0;
            System.out.println(a);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.toString());
            logger.info(e.getCause());
            logger.info(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }
}
