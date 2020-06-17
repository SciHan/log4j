package LayoutStudy;

import org.apache.log4j.*;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.XMLLayout;

/**
 * Layout类是所有Log4J中Layout的基类，它是一个抽象类，定义了Layout的接口。以下是具体的方法：
 * 1.format()方法：将LoggingEvent类中的信息格式化成一行日志。
 * @see Layout#format(LoggingEvent)
 *
 * 2.getContentType()：定义日志文件的内容类型，目前在Log4J中只是在SMTPAppender中用到，用于设置发送邮件的邮件内容类型。
 * 而Layout本身也只有HTMLLayout实现了它。
 * @see Layout#getContentType()
 *
 * 3.getHeader()：定义日志文件的头，目前在Log4J中只是在HTMLLayout中实现了它。
 * @see Layout#getHeader()
 *
 * 4.getFooter()：定义日志文件的尾，目前在Log4J中只是HTMLLayout中实现了它。
 * @see Layout#getFooter()
 *
 * 5.ignoresThrowable()：定义当前layout是否处理异常类型。在Log4J中，不支持处理异常类型的有：
 * TTCLayout、PatternLayout、SimpleLayout。
 * @see Layout#ignoresThrowable()
 * 对于这个方法 如果返回fasle的话 代表异常类型的处理由自己进行处理 如果返回true的话 则会进行默认的处理 比如
 * SimpleLayout中异常的处理就是输出异常的堆栈信息 而比如HTMLLayout的处理 是不会进行堆栈信息的输出的 因为他
 * 有自己处理异常的逻辑，不需要使用默认的，堆栈信息已经输出到自己的html页面中了 具体可以查看
 * @see WriterAppender 的 subAppend(LoggingEvent) 方法进行了解  （其他的appender方法待看）
 *
 * 6.实现OptionHandler接口，该接口定义了一个activateOptions()方法，用于配置文件解析完后，同时应用所有配置，
 * 以解决有些配置存在依赖的情况。该接口将在配置文件相关的小节中详细介绍。
 * @see Layout#activateOptions()
 */
public class LayoutDemo {

    private static Logger rootLogger = LogManager.getRootLogger();
    private static Logger logger = Logger.getLogger(LayoutDemo.class);

    /**
     * Layout负责将LoggingEvent中的信息格式化成一行日志信息。对不同格式的日志可能还需要提供头和尾等信息。
     * 另外有些Layout不会处理异常信息，此时ignoresThrowable()方法返回false，并且异常信息需要Appender
     * 来处理，如PatternLayout。
     *
     * Log4J自身实现了7个Layout类，可以通过继承自Layout类以实现用户自定义的日志消息格式。
     *
     * 1.
     * {@link SimpleLayout}:SimpleLayout是最简单的Layout，它只是打印消息级别和渲染后的消息，并且不处理异常信息。
     * 格式如下
     * INFO - Begin to execute testBasic() method
     * INFO - Executing
     *
     * 2.
     * {@link HTMLLayout}:HTMLLayout将日志消息打印成HTML格式，Log4J中HTMLLayout的实现中将每一条日志信息打印成表
     * 格中的一行，因而包含了一些Header和Footer信息。并且HTMLLayout类还支持配置是否打印位置信息和自定义title。
     * 详细的格式可以参照package中的HTML文件
     * 对于消息头，即HTML的header内容 可以对 {@link HTMLLayout#getHeader()}进行断点 然后查看方法堆栈 可以找到
     * @see WriterAppender 的writeHeader()方法 此处会将layout得到的header写进outputStream中 随后在进行日志
     * 输出的时候就会一并进行输出 所以header只会输出一次
     *
     * 3.
     * {@link org.apache.log4j.xml.XMLLayout}:XMLLayout将日志消息打印成XML文件格式，打印出的XML文件不是一个
     * 完整的XML文件，它可以外部实体引入到一个格式正确的XML文件中。如XML文件的输出名为abc，则可以通过以下方式引入：
     * <?xml version="1.0" ?>
     * <!DOCTYPE log4j:eventSet PUBLIC "-//APACHE//DTD LOG4J 1.2//EN" "log4j.dtd" [<!ENTITY data SYSTEM "abc">]>
     * <log4j:eventSet version="1.2" xmlns:log4j="http://jakarta.apache.org/log4j/">
     * &data;
     * </log4j:eventSet>
     * 详细的格式可以参照package中的XML文件
     * XMLLayout同样支持设置是否支持打印位置信息以及MDC（Mapped Diagnostic Context）信息，他们的默认值都为false：
     * MDC即一些运行时设置的信息 应该是用户自定义的 参考 https://blog.csdn.net/userwyh/article/details/52862216
     *
     * 4.
     * {@link TTCCLayout}:有三个属性可以设置是否打印拓展信息 如线程、上下文、logger名称，详细可以看
     * @see LayoutDemo#configTTCCLayout() 中的注释,包括了日志的输出格式；同时TTCCLayout不处理异常信息，
     * TTCCLayout中的重点方法是 {@link org.apache.log4j.helpers.DateLayout#dateFormat(StringBuffer,LoggingEvent)}
     * 这个方法在 {@link TTCCLayout#format(LoggingEvent)} 方法中，也是最主要的逻辑，主要是格式化时间，支持几种格式化时间格式，
     * 如ISO8601、DATE_AND_TIME、ABS_TIME等等，具体的区别这里不多说了可以自己查阅，Log4J推荐使用自己定义的DateFormat，但文
     * 档上说Log4J中定义的DateFormat信息有更好的性能。
     *
     * 5.
     * {@link PatternLayout}:PatternLayout是Log4J中最常用也是最复杂的Layout,PatternLayout的设计理念是LoggingEvent实例
     * 中所有的信息是否显示、以何种格式显示都是可以自定义的。可以说是类似正则表达式，根据字符的格式设定是否输出某些内容，他可以通过字
     * 符的格式设置达到TTCCLayout的格式，也可以进行内容的删减，是一种非常灵活的日志格式化模式。支持的字符如下：
     *       格式字符               结果
     *          c                 显示logger name，可以配置精度，如%c{2}，从后开始截取。
     *          C                 显示日志写入接口的雷鸣，可以配置精度，如%C{1}，从后开始截取。注：会影响性能，慎用。
     *          d                 显示时间信息，后可定义格式，如%d{HH:mm:ss,SSS}，或Log4J中定义的格式，如%d{ISO8601}，
     *                            %d{ABSOLUTE}，Log4J中定义的时间格式有更好的性能。
     *          F                 显示文件名，会影响性能，慎用。
     *          l                 显示日志打印是的详细位置信息，一般格式为full.qualified.caller.class.method(filename:lineNumber)。
     *                            注：该参数会极大的影响性能，慎用。
     *          L                 显示日志打印所在源文件的行号。注：该参数会极大的影响性能，慎用。
     *          m                 显示渲染后的日志消息。
     *          M                 显示打印日志所在的方法名。注：该参数会极大的影响性能，慎用。
     *          n                 输出平台相关的换行符。
     *          p                 显示日志Level
     *          r                 显示相对时间，即从程序开始（实际上是初始化LoggingEvent类）到日志打印的时间间隔，以毫秒为单位。
     *          t                 显示打印日志对应的线程名称。
     *          x                 显示与当前线程相关联的NDC（Nested Diagnostic Context）信息。
     *          X                 显示和当前想成相关联的MDC（Mapped Diagnostic Context）信息。
     *          %                 %%表达显示%字符
     * 而且PatternLayout还支持在格式字符串前加入精度信息：%-min.max[conversionChar]，如%-20.30c表示显示日志名，左对齐，最短
     * 20个字符，最长30个字符，不足用空格补齐，超过的截取（从后往前截取）
     * 因而PatternLayout实现中，最主要要解决的是如何解析上述定义的格式。实现上述格式的解析，一种最直观的方法是每次遍历格式字符串，
     * 当遇到’%’，则进入解析模式，根据’%’后不同的字符做不同的解析，对其他字符，则直接作为输出的字符。这种代码会比较直观，但是它每
     * 次都要遍历格式字符串，会引起一些性能问题，而且如果在将来引入新的格式字符，需要直接改动PatternLayout代码，不利于可扩展性。
     * 因此PatternLayout利用了解释器模式来完成解析 其中利用了有限状态机的知识.
     * {@link PatternLayout#setConversionPattern}方法中设定了字符patternConverters串后，就会进入到{@link PatternParser#parse()}方法，
     * 这个方法就是通过有限状态机解析字符串，解析为一条链，这条链在后面的日志记录即layout中就会对每条日志进行链式处理 格式化日志进行
     * 记录。{@link PatternParser#parse()} 有详细一点的parser解析注释
     *
     * 6.
     * {@link EnhancedPatternLayout}:在Log4J文档中指出PatternLayout中存在同步问题以及其他问题，因而推荐使用EnhancedPatternLayout
     * 来替换它。对于这句话的理解，appender中的doappend方法，是通过synchronized来控制并发的，正常是不会有线程安全问题的，不过
     * EnhancedPatternLayout和PatternLayout的区别在于EnhancedPatternLayout的format方法，重新new了一个StringBuffer，而不是
     * 使用类中的成员变量.
     * 另外的区别在于EnhancedPatternLayout似乎支持异常处理，还有setConversionPattern方法的逻辑，EnhancedPatternLayout使用
     * BridgePatternConverter解析出关于patternConverters和patternFields两个相对独立的集合，一个是装载了具体支持解析的字符串，
     * 另一个是装载对应字符串的大小限制，即min值和max值，之前说过PatternLayout是支持控制大小的，遍历这两个集合，构建出两个对应的数组，
     * 以在以后的解析中使用。大体上，EnhancedPatternLayout还是类似PatternLayout的设计。
     * 至于线程不安全的说法，暂时没有头绪，因为log4j是串行打印日志，除非有多层级结构，即looger的parent不是rootLogger，那么
     * 可能会出现并发问题
     * 更新：{@link LayoutThreadTest#main(String[])}复现了线程不安全的状况，详细可跳转过去看
     */
    public static void main(String[] args) {
//        configRootLogger(rootLogger, new SimpleLayout());
//        configHTMLLayout();
//        configXMLLayout();
//        configTTCCLayout();
//        configPatternLayout();
//        configEnhancedPatternLayout();

        logger.info("Begin to execute testBasic() method");
        logger.info("Executing");
        try {
            throw new Exception("Deliberately throw an Exception");
        } catch (Exception e) {
            logger.error("Catching an Exception", e);
        }
        logger.info("Execute testBasic() method finished.");
    }

    private static void configEnhancedPatternLayout() {
        EnhancedPatternLayout enhancedPatternLayout = new EnhancedPatternLayout();
        enhancedPatternLayout.setConversionPattern("%r [%t] %p %c %x - %m%n");
        configRootLogger(enhancedPatternLayout);
    }

    private static void configPatternLayout() {
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setConversionPattern("%r [%t] %p %c %x - %m%n");
        configRootLogger(patternLayout);
    }

    private static void configTTCCLayout() {
        TTCCLayout ttccLayout = new TTCCLayout();
        ttccLayout.setDateFormat("ISO8601");
        //2020-06-14 11:35:53,791 INFO - Begin to execute testBasic() method
        //2020-06-14 11:36:13,128 [main] INFO LayoutStudy.LayoutDemo - Begin to execute testBasic() method
        /*
          下列三个属性是可以设置的
          第一个为是否输出线程的名字
          第二个为是否输出category的class名 即logger的class name
          第三个为是否输出上下文信息
         */
        ttccLayout.setThreadPrinting(false);
        ttccLayout.setCategoryPrefixing(false);
        ttccLayout.setContextPrinting(false);
        configRootLogger(ttccLayout);
    }

    private static void configXMLLayout() {
        XMLLayout xmlLayout = new XMLLayout();
        //是否输出位置信息和MDC
        xmlLayout.setLocationInfo(true);
        xmlLayout.setProperties(true);
        configRootLogger(xmlLayout);
    }

    private static void configHTMLLayout() {
        HTMLLayout layout = new HTMLLayout();
        //html中是否输出文件名字和代码所在的行数
        layout.setLocationInfo(true);
        //自定义title
        layout.setTitle("Log4J Log Messages HTMLLayout test");
        configRootLogger(layout);
    }

    private static void configRootLogger(Layout layout) {
        rootLogger.removeAllAppenders();
        rootLogger.addAppender(new ConsoleAppender(layout));
    }

}
