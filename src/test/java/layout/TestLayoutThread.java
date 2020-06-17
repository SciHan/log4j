package layout;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TestLayoutThread {

    /**
     * 复现为什么PatternLayout可以说线程不安全的问题
     * 正常来说，是很难出现线程不安全的问题的，条件可以说是很苛刻的，首先要弄清楚，线程不安全是怎么发生的，具体是在
     * {@link Category#callAppenders(LoggingEvent)}方法中发生的，但是这个方法是发生在synchronized中的，同时，他
     * 加锁的对象一般来说，在looger自身没有配置appender的时候，都是加锁在rootLogger上的，几乎所有的looger都是不配置属于
     * 自己的appender的，而是使用rootlogger中的appender，因为配置文件中的appender是装载在rootlogger上的，然而log4j提供了
     * 一个属性，即logger1.setAdditivity(false);这个属性可以不装载rootLogger的appender，可以自己灵活的配置appender，
     * 这样就会导致加锁的时候，锁的是自身，而不是正常所使用的rootlogger，此时，这只是第一个满足的条件，要想复现线程不安全的条件还
     * 有一个，那就是两个appender在运行的时候，使用的是同一个layout对象，那么就会发生线程不安全，因为共用了同一个stringbuffer，
     * 在PatternLayout中的stringbuffer是成员变量，在共用的时候，是可能出现线程不安全的情况的，而EnhancedPatternLayout，
     * stringbuffer是方法变量，并不是共享的，因此不会出现线程安全的问题
     * <p>
     * 疑问: 配置文件中使用的，是同一个layout吗
     * 更新 答：{@link LayoutThreadTest#TestConfigLayout(Logger, Logger)} 通过这个方法，还是会出现线程不安全的情况，应该
     * 证实，是同一个layout
     * <p>
     * 总的来说，感觉这个出现条件，太苛刻了，大多场景都是不会出现的，并不用过于在意。
     */
    @Test
    public void TestThreadProblem() {
        Logger logger1 = Logger.getLogger(TestLayoutThread.class);
        Logger logger2 = Logger.getLogger(Logger.class);

        PatternLayout patternLayout = new PatternLayout("%r [%t] %p %c %x - %m%n");
        EnhancedPatternLayout enhancedPatternLayout = new EnhancedPatternLayout("%r [%t] %p %c %x - %m%n");

        logger1.setAdditivity(false);
        ConsoleAppender consoleAppender = new ConsoleAppender(patternLayout, "System.out");
        logger1.addAppender(consoleAppender);

        logger2.setAdditivity(false);
        ConsoleAppender consoleAppender2 = new ConsoleAppender(patternLayout, "System.out");
        logger2.addAppender(consoleAppender2);

        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                logger1.info("aaaaa");
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                logger2.info("bbbbb");
            }
        });

        thread1.start();
        thread2.start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void TestConfigLayout(Logger logger1, Logger logger2) throws InterruptedException {
        Logger rootLogger = Logger.getRootLogger();
        ConsoleAppender appender= (ConsoleAppender) rootLogger.getAllAppenders().nextElement();
        Layout layout = appender.getLayout();

        logger1.setAdditivity(false);
        ConsoleAppender consoleAppender = new ConsoleAppender(layout, "System.out");
        logger1.addAppender(consoleAppender);

        logger2.setAdditivity(false);
        ConsoleAppender consoleAppender2 = new ConsoleAppender(layout, "System.out");
        logger2.addAppender(consoleAppender2);

        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                logger1.info("aaaaa");
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                logger2.info("bbbbb");
            }
        });

        thread1.start();
        thread2.start();

        TimeUnit.SECONDS.sleep(5);
    }
}
