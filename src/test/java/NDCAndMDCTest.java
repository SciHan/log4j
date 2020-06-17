import appender.AppenderTest;
import org.apache.log4j.*;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class NDCAndMDCTest {

    /**
     * 有时候，一段相同的代码需要处理不同的请求，从而导致一些看似相同的日志其实是在处理不同的请求。为了避免这种情况，从而使
     * 日志能够提供更多的信息。
     * 要实现这种功能，一个简单的做法每个请求都有一个唯一的ID或Name，从而在处理这样的请求的日志中每次都写入该信息从而区分看
     * 似相同的日志。但是这种做法需要为每个日志打印语句添加相同的代码，而且这个ID或Name信息要一直随着方法调用传递下去，非常
     * 不方便，而且容易出错。Log4J提供了两种机制实现类似的需求：NDC和MDC。NDC是Nested Diagnostic Contexts的简称，它
     * 提供一个线程级别的栈，用户向这个栈中压入信息，这些信息可以通过Layout显示出来。MDC是Mapped Diagnostic Contexts的
     * 简称，它提供了一个线程级别的Map，用户向这个Map中添加键值对信息，这些信息可以通过Layout以指定Key的方式显示出来。
     */

    Logger rootLogger;
    Logger logger;

    @Before
    public void configLogger() {
        rootLogger = Logger.getRootLogger();
        logger = Logger.getLogger(AppenderTest.class);
        rootLogger.removeAllAppenders();
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("%x - %m%n");
        logger.addAppender(new ConsoleAppender(layout));
    }


    /**
     * NDC所有的操作都是针对当前线程的，因而不会影响其他线程。而在NDC实现中，使用一个Hashtable，其Key是线程实例，
     * 这样的实现导致用户需要手动的调用remove方法，移除那些push进去的数据以及移除那些已经过期的线程数据，不然就会
     * 出现内存泄露的情况；另外，如果使用线程池，在没有及时调用remove方法的情况下，容易前一线程的数据影响后一线程的
     * 结果。很奇怪为什么这里没有ThreadLocal或者是WeakReference，这样就可以部分的解决忘记调用remove引起的后果，
     * 可能是出于兼容性的考虑？
     */
    @Test
    public void testNDC() {
        NDC.push("Levin");
        NDC.push("Ding");
        logger.info("Begin to execute testBasic() method");
        logger.info("Executing");

        new Thread(() -> {
            logger.info("noting NDC");
            logger.info("noting NDC");
        }).start();

        try {
            throw new Exception("Deliberately throw an Exception");
        } catch (Exception e) {
            logger.error("Catching an Exception", e);
        }
        logger.info("Execute testBasic() method finished.");

        NDC.pop();
        NDC.pop();
    }

    /**
     * MDC的设计，更类似不同线程也可以传递数据，用的不是threadLocal，用的是InheritableThreadLocal，看下面的例子，
     * 可以知道，在不同的线程也是同样可以得到值的。父线程创建的线程，同样可以使用threadlocal中的变量
     * 具体的差别可以查看testMDC02
     * 如果看懂InheritableThreadLocal的话，下面的代码并不难明白，可以看 https://www.jianshu.com/p/94ba4a918ff5
     */
    @Test
    public void testMDC() {
        MDC.put("ip", "127.0.0.1");
        MDC.put("name", "levin");

        logger.info(MDC.get("ip") + Thread.currentThread().getName());
        logger.info(MDC.get("name") + Thread.currentThread().getName());

        Thread thread = new Thread(() -> {
            logger.info(MDC.get("ip") + Thread.currentThread().getName());
            logger.info("noting MDC" + Thread.currentThread().getName());
        },"testThread");
        thread.start();

//        MDC.remove("ip");
//        MDC.remove("name");

    }

    @Test
    public void testMDC02() {
        Thread thread = new Thread(()->{
            MDC.put("ip", "127.0.0.1");
            MDC.put("name", "levin");
            logger.info(MDC.get("ip") + Thread.currentThread().getName());
            logger.info(MDC.get("name") + Thread.currentThread().getName());
        },"mdc01");

        Thread thread1 = new Thread(()->{
            logger.info(MDC.get("ip") + Thread.currentThread().getName());
            logger.info(MDC.get("name") + Thread.currentThread().getName());
        },"mdc02");

        thread.start();
        thread1.start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
