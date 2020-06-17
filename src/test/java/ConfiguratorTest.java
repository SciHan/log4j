import appender.SocketAppenderTest;
import org.apache.log4j.*;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.ThrowableRenderer;
import org.junit.Test;
import sun.rmi.runtime.Log;

import java.util.ArrayList;

public class ConfiguratorTest {

    /**
     * LoggerRepository从字面上理解，它是一个Logger的容器，它会创建并缓存Logger实例，从而具有相同名字的Logger实例不会多
     * 次创建，以提高性能。它的这种特性有点类似Spring的IOC概念。Log4J支持两种配置文件：properties文件和xml文件。
     * Configurator解析配置文件，并将解析后的信息添加到LoggerRepository中。LogManager最终将LoggerRepository和
     * Configurator整合在一起。
     * <p>
     * {@link org.apache.log4j.spi.LoggerRepository}:LoggerRepository是一个Logger的容器，它负责创建、缓存Logger
     * 实例，同时它也维护了Logger之间的关系，因为在Log4J中，所有Logger都组装成以RootLogger为根的一棵树，树的层次由Logger
     * 的Name来决定，其中以’.’分隔。
     * 除了做为一个Logger容器，它还有一个Threshold属性，用于过滤所有在Threshold级别以下的日志。以及其他和Logger操作相关的方
     * 法和属性。
     * <p>
     * {@link org.apache.log4j.Hierarchy}:Hierarchy是Log4J中默认对LoggerRepository的实现类，它用于表达其内部的Logger
     * 是以层次结构存储的。在对LoggerRepository接口的实现中，getLogger()方法是其最核心的实现，因而首先从这个方法开始。
     * Hierarchy中用一个Hashtable来存储所有Logger实例，它以CategoryKey作为key，Logger作为value，其中CategoryKey是对
     * Logger中Name字符串的封装，之所以要引入这个类是出于性能考虑，因为它会缓存Name字符串的hash code，这样在查找过程中计算
     * hash code时就可以直接取得而不用每次都计算。
     * 同时getLogger()方法中有一个重载函数提供LoggerFactory接口，它用于没有在LoggerRepository中找到Logger实例时创建相应的
     * Logger实例，默认实现直接创建一个Logger实例，用户可以通过自定义LoggerFactory实现创建自己的Logger实例。
     * <p>
     * {@link org.apache.log4j.Hierarchy#getLogger(String, LoggerFactory)}
     * getLogger()方法首先根据传入name创建CategoryKey实例，而后从缓存的hashtable中查找：
     * 1.如果找到对应的Logger实例，则直接返回该实例。
     * 2.如果没有找到任何实例，则使用LoggerFactory创建新的Logger实例，并将该实例缓存到hastable中，同时更新新创建Logger实例的
     * parent属性。更新parent属性最简单的做法是从后往前以’.’为分隔符截取字符串，使用截取后的字符串从ht集合中查找是否存在Logger
     * 实例，如果存在，则新创建的Logger实例的parent即为找到的实例，若在整个遍历过程中都没有找到相应的parent实例，则其parent实例
     * 为root。然而如果一个“x.y.z.w”Logger起初的parent设置为root，而后出现“x.y.z”Logger实例，那么就需要更新“x.y.z.w”Logger
     * 的parent为“x.y.z”Logger实例，此时就会遇到一个如何找到在集合中已经存在的“x.y.z”Logger实例子节点的问题。当然一种简单的做法
     * 是遍历ht集合中所有实例，判断那个实例是不是“x.y.z”Logger实例的子节点，是则更新其parent节点。由于每次的遍历会引起一些性能问
     * 题，因而Log4J使用ProvisionNode事先将所有的可能相关的子节点保存起来，并将ProvisionNode实例添加到ht集合中，这样只要找到对
     * 应的ProvisionNode实例，就可以找到所有相关的子节点了。比如对“x.y.z.w”Logger实例，它会产生三个ProvisionNode实例（当然如
     * 果相应的实例已经存在，则直接添加而无需创建，另外，如果相应节点已经是Logger实例，那么将“x.y.z.w”Logger实例的parent直接指
     * 向它即可）：ProvisionNode(“x”), ProvisionNode(“x.y”), ProvisionNode(“x.y.z”)，他们都存储了“x.y.z.w”Logger实例
     * 作为其子节点。
     * 3.如果找到的是ProvisionNode实例，首先使用factory创建新的Logger实例，将该实例添加到ht集合中，然后更新找到的ProvisionNode
     * 内部所有Logger的parent字段以及新创建Logger的parent字段。更新过程中需要注意ProvisionNode中的Logger实例已经指向了正确的
     * parent了，所以只要更新那些ProvisionNode中Logger实例指向的parent比新创建的Logger本身层次要高的那些parent属性。比如开始
     * 插入“x.y.z”Logger实例，而后插入“x.y.z.w”Logger实例，此时ProvisionNode(“x”)认为“x.y.z”Logger实例和“x.y.z.w”Logger
     * 实例都是它的子节点，而后插入“x”Logger实例，那么只需要更新“x.y.z”Logger的父节点为“x”Logger实例即可，而不用更新“x.y.z.w”
     * Logger实例的父节点。
     * <p>
     * 其他的方法实现则比较简单，对LoggerRepository来说，它也可以像其注册HierarchyEventListener监听器，每当向一个Logger添加或
     * 删除Appender，该监听器就会触发。
     * <p>
     * Hierarchy中保存了threshold字段，用户可以设置threshold。而对root实例，它在够着Hierarchy时就被指定了。getCurrentLoggers()
     * 方法将ht集合中所有的Logger实例取出。shutdown()方法遍历所有Logger实例以及root实例，调用所有附加其上的Appender的close()方法
     * ，并将所有Appender实例从Logger中移除，最后触发AppenderRemove事件。resetConfiguration()方法将root字段初始化、调用
     * shutdown()方法移除Logger中的所有Appender、初始化所有Logger实例当不将其从LoggerRepository中移除、清楚rendererMap和
     * throwableRender中的数据。
     */

    @Test
    public void CreateLoggerTest() {

        Logger testLogger = Logger.getLogger("com.test.ConfiguratorTest");
        Logger testLogger1 = Logger.getLogger("com.test");

    }

    /**
     * 对LoggerRepository来说，它也可以像其注册HierarchyEventListener监听器，每当一个Logger添加或删除Appender，
     * 该监听器就会触发。
     */
    @Test
    public void TestEventListener() {
        LoggerRepository loggerRepository = LogManager.getLoggerRepository();
        //还有一个Threshold属性，用于过滤所有在Threshold级别以下的日志。
        loggerRepository.setThreshold("DEBUG");
        Logger testLogger = Logger.getLogger("com.test");

        loggerRepository.addHierarchyEventListener(new HierarchyEventListener() {
                                                       @Override
                                                       public void addAppenderEvent(Category cat, Appender appender) {
                                                           System.out.println("test add appenderEvent");
                                                       }

                                                       @Override
                                                       public void removeAppenderEvent(Category cat, Appender appender) {
                                                           System.out.println("test remove appenderEvent");
                                                       }
                                                   }
        );
        ConsoleAppender consoleAppender = new ConsoleAppender();

        testLogger.addAppender(consoleAppender);
        testLogger.removeAppender(consoleAppender);
    }

    /**
     * RendererSupport接口支持用户为不同的类设置相应的ObjectRender实例，从而可以从被渲染的类中或许更多的信息而不是默认的调用其
     * toString()方法。
     * 下列即为使用的示例，这里我还是觉得是写的比较好的部分，主要是作用于，如果有非string类的日志内容，默认是调用tosring方法，如果
     * 不想使用默认的tostring方法，那么就可以使用下列的方法，添加Renderer，分别需要两个参数，第一个是class的类型，第二个是这个class
     * 类型如何去渲染日志信息，那么就会调用doRender方法，而不会使用默认的tostring方法；实现原理也不难，debug一下很容易就懂了，对于
     * loggerRepository，内部维护着一个map，key为class类型，value为ObjectRender，当layout格式化日志的时候，会调用loggingEvent
     * 的getRenderedMessage方法，就会去验证传入的日志内容，如果是string类型，直接添加到layout中，如果不是string类型，就会查找
     * 是否有添加的render，通过验证loggerRepository中的map看是否能对应，即RendererMap中的get方法，如果能对应，就使用对应的
     * render，如果不能，就使用默认的tostring
     * {@link SocketAppenderTest#testSocketThrowable()} + {@link SocketAppenderTest#testSimpleSocketServer()}
     * 更具体的用法可以看这两个test，我觉得这也是一种奇妙的设计
     */
    @Test
    public void TestRenderMap() {
        Hierarchy loggerRepository = (Hierarchy) LogManager.getLoggerRepository();
        loggerRepository.setRenderer(ArrayList.class, new ObjectRenderer() {
            @Override
            public String doRender(Object o) {
                ArrayList list = (ArrayList) o;
                StringBuilder str = new StringBuilder();
                for (Object value : list) {
                    String string = (String) value;
                    str.append(value);
                }
                return str.toString();
            }
        });
        Logger testLogger = Logger.getLogger("com.test.ConfiguratorTest");
        ArrayList<String> list = new ArrayList<>();
        list.add("abc");
        list.add("bcd");
        list.add("cde");
        testLogger.info(list);
    }

    /**
     * ThrowableRendererSupport接口用于支持设置和获取ThrowableRenderer，从而用户可以自定义对Throwable对象的渲染。Hierarchy
     * 同样有实现这个接口，具体的用法类似上述的render，可以通过设置
     * 这里应该结合layout中的异常处理一起看，那么就会懂异常处理是什么意思了
     */
    @Test
    public void TestThrowableRendererSupport() {
        Hierarchy loggerRepository = (Hierarchy) LogManager.getLoggerRepository();
        loggerRepository.setThrowableRenderer(new ThrowableRenderer() {
            @Override
            public String[] doRender(Throwable t) {
                return new String[]{"abc", "bcd", "ecd"};
            }
        });

        Logger testLogger = Logger.getLogger("com.test.ConfiguratorTest");
        try {
            throw new Exception("Deliberately throw an Exception");
        } catch (Exception e) {
            testLogger.error("Catching an Exception", e);
        }
    }
}
