package appender;

import layout.TestLayoutDemo;
import org.apache.log4j.*;
import org.apache.log4j.jdbc.JDBCAppender;
import org.apache.log4j.lf5.LF5Appender;
import org.apache.log4j.net.JMSAppender;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.net.SocketAppender;
import org.apache.log4j.net.TelnetAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class AppenderTest {

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

    /**
     * Appender负责定义日志输出的目的地，它可以是控制台（ConsoleAppender）、文件（FileAppender）、
     * JMS服务器（JmsLogAppender）、以Email的形式发送出去（SMTPAppender）等。Appender是一个命名
     * 的实体，另外它还包含了对Layout、ErrorHandler、Filter等引用
     *
     * 简单的，在配置文件中，Appender会注册到Logger中，通常配置文件中的appender都是配置到rootLogger中，
     * Logger在写日志时，通过继承机制遍历所有注册到它本身和其父节点的Appender（在additivity为true的情况下），
     * 调用doAppend()方法，实现日志的写入。在doAppend方法中，若当前Appender注册了Filter，则doAppend还会判
     * 断当前日志时候通过了Filter的过滤，通过了Filter的过滤后，如果当前Appender继承自SkeletonAppender，还会
     * 检查当前日志级别时候要比当前Appender本身的日志级别阀门要打，所有这些都通过后，才会将LoggingEvent实例传递
     * 给Layout实例以格式化成一行日志信息，最后写入相应的目的地，在这些操作中，任何出现的错误都由ErrorHandler字段来处理。
     *
     * {@link FileAppender} FileAppender继承自WriterAppender，它将日志写入文件。主要的日志写入逻辑已经在WriterAppender
     * 中处理，FileAppender主要处理的逻辑主要在于将设置日志输出文件名，并通过设置的文件构建WriterAppender中的QuiteWriter字
     * 段实例。如果Log文件的目录没有创建，在setFile()方法中会先创建目录，再设置日志文件。另外，所有FileAppender字段在调用
     * activateOptions()方法中生效。
     * FileAppender的构造方法参数可以设置五个，分别是layout、filename即文件路径、append（布尔值，默认为true，即将新的文件
     * 内容追加到已有文件上，false即重新创建新文件，抛弃已有内容）、bufferedIO（布尔值，默认是true，即每次写完内容就flush，false
     * 即过一段时间才flush）、bufferSize（bufferSize的设置），在构造方法会通过setFile()方法创建目录，构建WriterAppender中的
     * QuiteWriter。
     *
     * {@link DailyRollingFileAppender} DailyRollingFileAppender继承自FileAppender，DailyRollingFileAppender会在
     * 每隔一段时间可以生成一个新的日志文件，不过这个时间间隔是可以设置的，不仅仅只是每隔一天。时间间隔通过setDatePattern()方法
     * 设置，datePattern必须遵循SimpleDateFormat中的格式。支持的时间间隔有：
     * 1.       每天：’.’YYYY-MM-dd（默认）
     * 2.       每星期：’.’YYYY-ww
     * 3.       每月：’.’YYYY-MM
     * 4.       每隔半天：’.’YYYY-MM-dd-a
     * 5.       每小时：’.’YYYY-MM-dd-HH
     * 6.       每分钟：’.’YYYY-MM-dd-HH-mm
     * DailyRollingFileAppender需要设置的三个属性：layout，datePattern和fileName。其中datePattern用于确定时间间隔以及
     * 当日志文件过了一个时间间隔后用于重命名之前的日志文件；fileName用于设置日志文件的初始名字。在实现过程中，datePattern用于实例化
     * SimpleDateFormat，记录当前时间以及计算下一个时间间隔时间。在每次写日志操作之前先判断当前时间是否已经操作计算出的下一间
     * 隔时间，若是，则将之前的日志文件重命名（向日志文件名尾添加datePattern指定的时间信息），并创新的日志文件，同时重新设置当
     * 前时间以及下一次的时间间隔。
     * 至于将日志按设定的时间间隔分文件进行存储的过程，是在{@link DailyRollingFileAppender#subAppend(LoggingEvent)}方法中
     * 实际上是在{@link WriterAppender#doAppend(LoggingEvent)}方法中调用的，子类没有重写WriterAppender的doAppend方法
     * 这是个父子类方法调用的差别，具体逻辑还是比较清晰的，
     * 文件分隔的方法是{@link DailyRollingFileAppender#rollOver()}方法中，注释已经写的很清楚了
     * ps：存在线程不安全的隐患，具体可以查看{@link DailyRollingFileAppender#rollOver()}中的注释
     *
     * {@link RollingFileAppender}RollingFileAppender继承自FileAppender，不同于DailyRollingFileAppender是基于时间作
     * 为阀值，RollingFileAppender则是基于文件大小作为阀值。当日志文件超过指定大小，日志文件会被重命名成”日志文件名.1”，若此文
     * 件已经存在，则将此文件重命名成”日志文件名.2”，一次类推。若文件数已经超过设置的可备份日志文件最大个数，则将最旧的日志文件删除。
     * 如果要设置不删除任何日志文件，可以将maxBackupIndex设置一个较大的值，如果这样，这里rollover()方法的实现会引起一些性能
     * 问题，因为它要冲最大值开始遍历查找已经备份的日志文件。
     * 这个appender的源码都比较简单，就不细说了，注意的是maxBackupIndex是指备份文件的个数，默认是1，那么就只保存一份备份文件，
     * 举个例子，默认的重命名阈值是10MB，当11MB的时候，前10MB的日志就会被重命名，接下来11～20MB的日志就会写到新的文件中，可是如果
     * 到20.1的时候，由于最大设置可备份文件数为1，那么1～10MB的那个日志就会被删除，所以如果要想不删除日志，maxBackupIndex可以设置
     * 为一个较大的值，可是不能太大，因为它要冲最大值开始遍历查找已经备份的日志文件。遍历是因为需要取名字，比如设定为maxBackupIndex
     * 为5，那么第一份日志即1～10MB的日志名是（filename + 1），当需要备份11～20MB的日志时，他需要从5开始遍历看看结尾5的日志存不存
     * 在，不存在则跳过，当遍历到1发现存在的时候，就取名为 i + 1 即取为2，所以maxBackupIndex不能取太大值
     * PS:好像还是线程不安全的，不过他做了些补偿措施，后续如果改名失败，被其他线程抢先，所有的操作都会经renameSucceeded布尔值判断才
     * 进行，如果修改失败的话就不能进行删除的操作。而且如果被其他线程先完成改名的操作，原文件也不存在了，再想改名的操作也无法完成了。
     *
     * {@linkplain AsyncAppender}AsyncAppender顾名思义，就是异步的调用Appender中的doAppend()方法。常用的方案可以使用生产者
     * 和消费中的模式来实现类似的逻辑。即每一次请求做为一个生产者，将请求放到一个Queue中，而由另外一个或多个消费者读取Queue中的内容
     * 以处理真正的逻辑。
     * 在最新的Java版本中，我们可以使用BlockingQueue类简单的实现类似的需求，然而由于Log4J的存在远早于BlockingQueue的创建，因而
     * 为了实现对以前版本的兼容，它还是自己实现了这样一套生产者消费者模型。
     * AsyncAppender并不会在每一次的doAppend()调用中都直接将消息输出，而是使用了buffer，buffer即一个arraylist队列，只有等到
     * buffer中LoggingEvent实例到达bufferSize个的时候才真正的处理这些消息，当然我们也可以讲bufferSize设置成1，从而实现每一个
     * LoggingEvent实例的请求都会直接执行。如果bufferSize设置过大，在应用程序异常终止时可能会丢失部分日志。
     *
     * 这里其实有一个bug，即当程序停止时只剩下discardMap中有日志信息，而buffer中没有日志信息，由于Dispatcher线程不检查
     * discardMap中的日志信息，因而此时会导致discardMap中的日志信息丢失。即使在生成者中当buffer为空时，它也会激活buffer
     * 锁，然而即使激活后buffer本身大小还是为0，因而不会处理之后的逻辑，因而这个逻辑也处理不了该bug。
     * 对于生产者，它首先处理当消费者线程出现异常而不活动时，此时将同步的输出日志；而后根据配置获取LoggingEvent中的数据；再获得
     * buffer的对象锁，如果buffer还没满，则直接将LoggingEvent实例添加到buffer中，否则如果blocking设置为true，即生产者会等
     * 消费者处理完后再继续下一次接收数据。如果blocking设置为fasle或者消费者线程被打断，那么当前的LoggingEvent实例则会保存在
     * discardMap中，因为此时buffer已满。
     *
     * 总的来说，AsyncAppender的源码并不难懂，就是一个非常标准的生产者消费者模式，同时了做了很多补偿措施，如dispatch线程发生异
     * 常的时候，直接进行同步处理，如生产者的线程被interrupt的时候，通过补偿措施将event存进一个map供dispatch线程使用，可以说
     * 真的是一个非常标准的生产者消费者模式的实现
     *
     * {@link org.apache.log4j.jdbc.JDBCAppender}JDBCAppender将日志保存到数据库的表中，由于数据库保存操作是一个比较费时的
     * 操作，因而JDBCAppender默认使用缓存机制，当然你也可以设置缓存大小为1实现实时向数据库插入日志。JDBCAppender中的Layout默认
     * 只支持PatternLayout，用户可以通过设置自己的PatternLayout，其中ConversionPattern设置成插入数据库的SQL语句或通过
     * setSql()方法设置SQL语句，JDBCAppender内部会创建相应的PatternLayout，如可以设置SQL语句为：
     * insert into LogTable(Thread, Class, Message) values(“%t”, “%c”, “%m”)在doAppend()方法中，JDBCAppender就会通过
     * layout获取SQL语句，将LoggingEvent实例插入到数据库中。整个JDBCAppender的缺点在于，虽然通过缓冲区的操作，让数据库的创造
     * 连接的操作不频繁执行，但是当具体执行的时候，每条日志都会创建一个connection，而不是连接池的操作，解决方法可以是用户可以编写自己
     * 的JDBCAppender，继承自JDBCAppender，重写getConnection()和closeConnection()，实现从数据库连接池中获取connection，
     * 在每次将JDBCAppender缓存中的LoggingEvent列表插入数据库时从连接池中获取缓存，而在该操作完成后将获得的连接释放回连接池。同时如
     * 果想拓展sql，用户也可以重写getLogstatement()以自定义插入LoggingEvent的SQL语句。
     *
     * {@link org.apache.log4j.net.JMSAppender}JMSAppender类将LoggingEvent实例序列化成ObjectMessage，并将其发送到
     * JMS Server的一个指定Topic中。它的实现比较简单，设置相应的connectionFactoryName、topicName、providerURL、userName、
     * password等JMS相应的信息，在activateOptions()方法中创建相应的JMS链接，在doAppend()方法中将LoggingEvent序列化成
     * ObjectMessage发送到JMS Server中，它也可以通过locationInfo字段是否需要计算位置信息。如果想序列化threadName这样的拓展
     * 信息，那么可以在msg.setObject(event)方法中拓展实现
     *
     * {@link org.apache.log4j.net.TelnetAppender}TelnetAppender类将日志消息发送到指定的Socket端口（默认为23），用户可以
     * 使用telnet连接以获取日志信息。这里的实现貌似没有考虑到telnet客户端退出时候清空的问题。另外，在windows中可能默认没有telnet
     * 支持，此时只需要到”控制面板”->”程序和功能”->”打开或关闭windows功能”中大概Telnet服务即可。TelnetAppender使用内部类
     * SocketHandler封装发送日志消息到客户端，如果没有Telnet客户端连接，则日志消息将会直接被抛弃。
     * 发送日志在send方法中，实现不难看懂，就不仔细描述了
     *
     * {@link org.apache.log4j.net.SMTPAppender}SMTPAppender将日志消息以邮件的形式发送出来，默认实现，它会先缓存日志信息，
     * 只有当遇到日志级别是ERROR或ERROR以上的日志消息时才通过邮件的形式发送出来，如果在遇到触发发送的日志发生之前缓存中的日志信息
     * 已满，则最早的日志信息会被覆盖。用户可以通过setEvaluatorClass()方法改变触发发送日志的条件。
     *
     * {@link org.apache.log4j.net.SocketAppender}SocketAppender将日志消息（LoggingEvent序列化实例）发送到指定Host的
     * port端口。在创建SocketAppender时，SocketAppender会根据设置的Host和端口建立和远程服务器的链接，并创建ObjectOutputStream
     * 实例。
     * 具体实现也不难，就是简单的通过ObjectOutputStream发送到具体的连接和端口，与TelnetAppender的区别可能在于，TelnetAppender
     * 是指定端口的，用户可以随意加进来进行连接，而SocketAppender是发给指定的地址和端口。
     * 还有一点，如果创建连接失败，调用fireConnector()方法，创建一个Connector线程，在每间隔reconnectionDelay（默认值为30000ms，
     * 若将其设置为0表示在链接出问题时不创建新的线程检测）的时间里不断重试链接。当链接重新建立后，Connector线程退出并将connector
     * 实例置为null以在下一次链接出现问题时创建的Connector线程检测。而后，在每一次日志记录请求时只需将LoggingEvent实例序列化到之
     * 前创建的ObjectOutputStream中即可，若该操作失败，则会重新建立Connector线程以隔时检测远程日志服务器可以重新链接。
     * 同时，log4j也提供了SocketNode，即日志服务器的使用方法，用于接受SocketAppender传送的日志信息并进行appender的具体使用，
     * 事实上，Log4J提供了两个日志服务器的实现类：SimpleSocketServer和SocketServer。他们都会接收客户端的连接，为每个客户端链接
     * 创建一个SocketNode实例，并根据指定的配置文件打印日志消息。它们的不同在于SimpleSocketServer同时支持xml和properties配置
     * 文件，而SocketServer只支持properties配置文件；另外，SocketServer支持不同客户端使用不同的配置文件（以客户端主机名作为选
     * 择配置文件的方式），而SimpleSocketServer不支持。
     * 最后，使用SocketAppender时，在应用程序退出时，最好显示的调用LoggerManager.shutdown()方法，不然如果是通过垃圾回收器来
     * 隐式的关闭（finalize()方法）SocketAppender，在Windows平台中可能会存在TCP管道中未传输的数据丢失的情况。另外，在网络连接
     * 不可用时，SocketAppender可能会阻塞应用程序，当网络可用，但是远程日志服务器不可用时，相应的日志会被丢失。如果日志传输给远程
     * 日志服务器的速度要慢于日志产生速度，此时会影响应用程序性能。
     *
     * {@link org.apache.log4j.net.SocketHubAppender}SocketHubAppender类似SocketAppender，它也将日志信息（序列化后
     * 的LoggingEvent实例）发送到指定的日志服务器，该日志服务器可以是SocketNode支持的服务器。只是SocketHubAppender不连接到
     * 给定的远程日志服务器，而是接受来自远程日志服务器的连接作为客户端。它可以接受多个连接。当接收到日志事件时，该事件被发送到当前
     * 连接的远程日志服务器集。通过这种方式实现，不需要对配置文件进行任何更新就可以将数据发送到另一个远程日志服务器。远程日志服务器
     * 仅连接到SocketHubAppender运行的主机和端口。
     * 另外，SocketHubAppender还会缓存部分LoggingEvent实力，从而支持在新注册一个日志服务器时，它会先将那些缓存下来的
     * LoggingEvent发送给新注册服务器，然后接受新的LoggingEvent日志打印请求。
     * 具体实现以及注意事项参考SocketAppender。最好补充一点，SocketHubAppender可以和chainsaw一起使用，它好像使用了
     * zeroconf协议，和SocketHubAppender以及SocketAppender中的ZeroConfSupport类相关。具体看 https://blog.csdn.net/iteye_3609/article/details/82063514
     * 总的来说，TelnetAppender是开放自身端口，等待客户端连接，然后进行日志发送；SocketAppender是将日志发送到指定的地质和端口；
     * SocketHubAppender是开放端口，等待远程日志服务器的连接，同时支持zeroconf协议进行远程查看。SocketHubAppender与TelnetAppender
     * 比较类似，不过实现不一样，SocketHubAppender是基于socket的，TelnetAppender使用的是基于Telnet的，socket主要是指传输层的协议，
     * 包括TCP，UDP，和SCTP；而TELNET是应用层协议，是基于传输层协议的上层协议
     *
     * {@link org.apache.log4j.lf5.LF5Appender}将日志显示在Swing窗口中。对Swing不熟，没怎么看代码，不过可以使用一下测试用例
     * 简单的做一些测试，提供一些感觉。
     *
     * 其他Appender类:ExternallyRolledFileAppender类继承自RollingFileAppender，因而它最基本的也是基于日志文件大小来备份
     * 日志文件。然而它同时还支持外界通过Socket发送“RollOver”给它以实现在特定情况下手动备份日志文件。Log4J提供Roller类来实现
     * 这样的功能，其使用方法是：
     * java -cp log4j-1.2.16.jar org.apache.log4j.varia.Roller <hostname> <port>
     * 但是由于任何可以和应用程序运行的服务器连接的代码都能像该服务器发送“RollOver”消息，因而这种方式并不适合production环境在，
     * 在production中最好能加入一些限制信息，比如安全验证等信息。
     *
     * NTEventLogAppender将日志打印到NT事件日志系统中（NT event log system）。顾名思义，它只能用在Windows中，而且需要
     * NTEventLogAppender.dll、NTEventLogAppender.amd64.dll、NTEventLogAppender.ia64.dll或NTEventLogAppender.x86.dll
     * 动态链接库在Windows PATH路径中。在Windows 7中可以通过控制面板->管理工具->查看事件日志中查看相应的日志信息。
     *
     * SyslogAppender将日志打印到syslog中，我对syslog不怎么了解，通过查看网上信息的结果，看起来syslog只是存在于Linux和Unix中。
     * SyslogAppender额外的需要配置Facility和SyslogHost字段。具体可以查看：http://www.cnblogs.com/frankliiu-java/articles/1754835.html
     * 和 http://sjsky.iteye.com/blog/962870。
     *
     * NullAppender作为一个Null Object模式存在，不会输出任何打印日志消息。
     */
    @Test
    public void logTest() throws Exception {
//        rootLogger.addAppender(new FileAppender(layout, "D://logs//myproject.log", false));
//        rootLogger.addAppender(new DailyRollingFileAppender(layout, "D:/logs/dailyFile.log", "’.’YYYY-MM-dd-HH-mm"));
//        rootLogger.addAppender(new RollingFileAppender(layout, "D://logs//myproject.log", false));
//        configAsyncAppender();
//        configJDBCAppender();
//        configJMSAppender();
        configTelnetAppender();
//        configSMTPAppender();

//        configLF5Appender();

        logger.info("Begin to execute testBasic() method");
        logger.info("Executing");
        try {
            throw new Exception("Deliberately throw an Exception");
        } catch (Exception e) {
            logger.error("Catching an Exception", e);
        }
        logger.info("Execute testBasic() method finished.");

    }

    private void configLF5Appender() {
        LF5Appender appender = new LF5Appender();
        appender.setLayout(new TTCCLayout());
        appender.activateOptions();
        rootLogger.addAppender(appender);
    }

    private void configSMTPAppender() {
        SMTPAppender smtpAppender = new SMTPAppender();
        smtpAppender.setBcc("xxx");
        //改变触发发送日志的条件
        smtpAppender.setEvaluatorClass("INFO");
        //然后还需要设置smtp的一些信息，就不仔细描述了，毕竟用的不多
        smtpAppender.activateOptions();
        rootLogger.addAppender(smtpAppender);
    }

    private void configTelnetAppender() {
        TelnetAppender telnetAppender = new TelnetAppender();
        telnetAppender.setPort(8080);
        telnetAppender.activateOptions();
        telnetAppender.setLayout(new SimpleLayout());
        rootLogger.addAppender(telnetAppender);
    }

    private void configJMSAppender() {
        JMSAppender jmsAppender = new JMSAppender();
        jmsAppender.setUserName("xxx");
        jmsAppender.setPassword("xxx");
        jmsAppender.setProviderURL("Xxx");
        jmsAppender.setTopicBindingName("xxx");
        jmsAppender.setTopicConnectionFactoryBindingName("xxx");
        // 等等jms的相关信息
        rootLogger.addAppender(jmsAppender);
        jmsAppender.activateOptions();
        rootLogger.addAppender(jmsAppender);
    }

    private void configJDBCAppender() {
        JDBCAppender jdbcAppender = new JDBCAppender();
        jdbcAppender.setSql("insert into LogTable(Thread, Class, Message) values(“%t”, “%c”, “%m”)");
        jdbcAppender.setBufferSize(1);
        jdbcAppender.setURL("xxx");
        jdbcAppender.setUser("xxx");
        jdbcAppender.setPassword("xxx");
        rootLogger.addAppender(jdbcAppender);
    }

    private void configAsyncAppender() {
        AsyncAppender appender = new AsyncAppender();
        appender.addAppender(new ConsoleAppender(new TTCCLayout()));
        appender.setBufferSize(1);
        appender.setLocationInfo(true);
        appender.activateOptions();
        rootLogger.addAppender(appender);
    }

    @Test
    public void FileTest() {
        File file = new File("/usr/local/wordspace/sourceCode/log4j-1.2.17-sources-master/D:/logs/dailyFile.log");
        if (file.exists()) {
            file.delete();
        }

        File file1 = new File("/usr/local/wordspace/sourceCode/log4j-1.2.17-sources-master/D:/logs/myproject.log");

        file1.renameTo(file);
    }

    @Test
    public void TestFileAppenderSafe() throws Exception {
        Logger logger1 = Logger.getLogger(TestLayoutDemo.class);
        Logger logger2 = Logger.getLogger(SocketAppender.class);
        logger1.setAdditivity(false);
        logger2.setAdditivity(false);

        logger1.addAppender(new DailyRollingFileAppender(layout, "D:/logs/dailyFile.log", "’.’YYYY-MM-dd-HH-mm"));
        logger2.addAppender(new DailyRollingFileAppender(layout, "D:/logs/dailyFile.log", "’.’YYYY-MM-dd-HH-mm"));

        Thread thread1 = new Thread(() -> {
            while (true) {
                logger1.info("aaaaaaaaaaaaaaaaaa");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            while (true) {
                logger2.info("bbbbbbbbbbbbbbbbbb");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread1.start();
        thread2.start();

        TimeUnit.HOURS.sleep(1);
    }
}
