import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.net.SyslogAppender;

public class LoggerDemo {

    private static Logger logger = Logger.getLogger(LoggerDemo.class);

    /**
     * 一些具体logger的拓展用法
     *
     * @param args
     */
    public static void main(String[] args) {
        /**
         * 这里有一个很重要的点 每个logger为了有默认值的存在 每个logger都会有parent属性
         * parent都为rootLogger 同时配置文件中的属性都是放置在rootLogger中的 如appender 有时候
         * 希望有一些logger不要使用rootLogger中的所有appender 则可以使用setAdditivity(false); 不继承
         * 父类的appender 那么在日志记录的时候 就不会使用rootLogger的appender进行记录 否则默认是都会使用rootLogger
         * 中的appender来记录
         *
         * ps：在Log4J中，所有Logger实例组成一个单根的树状结构，由于Logger实例的根节点有一点特殊：它的名字为“root”，
         * 它没有父节点，它的Level字段必须设值以防止其他Logger实例都没有设置Level值的情况。基于这些考虑，Log4J通过
         * 继承Logger类实现了RootLogger类，它用于表达所有Logger实例的根节点：
         */
        //parent即为rootLogger
        Category parent = logger.getParent();
        //不继承父类的appender
        logger.setAdditivity(false);
        System.out.println(logger.getAllAppenders());
        //对特定的logger添加appender
        logger.addAppender(new SyslogAppender());

        try {
            System.out.println(1);
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
