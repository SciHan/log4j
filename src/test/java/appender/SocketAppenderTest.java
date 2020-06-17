package appender;

import org.apache.log4j.*;
import org.apache.log4j.net.SocketAppender;
import org.apache.log4j.net.SocketNode;
import org.apache.log4j.spi.ThrowableRenderer;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketAppenderTest {

    Logger rootLogger;
    Logger logger;
    Layout layout;

    @Before
    public void configLogger() {
        rootLogger = Logger.getRootLogger();
        logger = Logger.getLogger(AppenderTest.class);
        rootLogger.removeAllAppenders();
        layout = new EnhancedPatternLayout("[%-5p] %d(%r) --> [%t] %l: %m %x %n");
    }

    @Test
    public void testSocketAppender() throws Exception {
        SocketAppender appender = new SocketAppender(
                InetAddress.getLocalHost(), 8000);
        appender.setLocationInfo(true);
        appender.setApplication("appender.AppenderTest");
        appender.activateOptions();
        rootLogger.addAppender(appender);

        Logger log = Logger.getLogger("levin.log4j.test.TestBasic");
        for (int i = 0; i < 100; i++) {
            Thread.sleep(10000);
            if (i % 2 == 0) {
                log.info("Normal test.");
            } else {
                log.info("Exception test", new Exception());
            }
        }
    }

    @Test
    public void testSimpleSocketServer() throws Exception {
        ConsoleAppender appender = new ConsoleAppender(new TTCCLayout());
        appender.activateOptions();
        rootLogger.addAppender(appender);

        ServerSocket serverSocket = new ServerSocket(8000);
        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new SocketNode(socket,
                    LogManager.getLoggerRepository()),
                    "SimpleSocketServer-" + 8000).start();
        }
    }

    @Test
    public void testSocketThrowable() throws Exception {
        Hierarchy loggerRepository = (Hierarchy) LogManager.getLoggerRepository();
        loggerRepository.setThrowableRenderer(new ThrowableRenderer() {
            @Override
            public String[] doRender(Throwable t) {
                return new String[]{"abc", "bcd", "ecd"};
            }
        });

        SocketAppender appender = new SocketAppender(
                InetAddress.getLocalHost(), 8000);
        appender.setLocationInfo(true);
        appender.setApplication("appender.AppenderTest");
        appender.activateOptions();
        rootLogger.addAppender(appender);



        Logger testLogger = Logger.getLogger("com.test.configuration.ConfiguratorTest");
        try {
            throw new Exception("Deliberately throw an Exception");
        } catch (Exception e) {
            testLogger.error("Catching an Exception", e);
        }
    }

}
