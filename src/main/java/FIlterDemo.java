import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.DenyAllFilter;
import org.apache.log4j.varia.LevelMatchFilter;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.varia.StringMatchFilter;

public class FIlterDemo {

    /**
     * log4j中filter的使用
     * filter如下所示 是与appender绑定在一起的 在配置文件中可以进行设置 每个appender都可以设置filter
     * filter是一个链式结构 每个filter可以设置链着的下一个filter 具体的使用可以参照
     * {@link AppenderSkeleton#doAppend} method.
     *
     * log4j中内置了四种filter 分别是DenyAllFilter、LevelMatchFilter、LevelRangeFilter、StringMatchFilter。
     * 如下所示
     *
     * {@link DenyAllFilter}
     * @see FIlterDemo#addDenyAllFilterToLogger()  （DenyAllFilter）
     * DenyAllFilter只是简单的在decide()中返回DENY值，可以将其应用在Filter链尾，实现如果之前的Filter都没有通过，
     * 则该LoggingEvent没有通过，类似或的操作：只要在前面filter通过了 那么日志就会被记录 这个filter可以放在最后 如果
     * 所有的filter都没有返回accept 则这个filter即可返回deny不记录日志
     *
     * {@link org.apache.log4j.varia.StringMatchFilter}
     * @see FIlterDemo#addStringMatchFilterToLogger （StringMatchFilter示例）
     * StringMatchFilter通过日志消息中的字符串来判断Filter后的状态;StringMatchFilter可以设置匹配的字符串，
     * 当字符串匹配过后可以具体决定filter返回的状态 可以通过设置acceptOnMatch属性决定如果匹配是返回accept还是deny
     *
     * {@link org.apache.log4j.varia.LevelMatchFilter}
     * @see FIlterDemo#addLevelMatchFilterToLogger （LevelMatchFilter示例）
     * LevelMatchFilter判断日志级别是否和设置的级别匹配以决定Filter后的状态
     * 可以为具体logger设定日志级别的拦截 当filter中的日志级别与配置文件中配置的日志级别不一样时 可以选择是否
     * 进行日志的记录 有一个布尔值来决定是否记录 与StringMatchFilter一样决定如果匹配是返回accept还是deny
     *
     * {@link org.apache.log4j.varia.LevelRangeFilter}
     * @see FIlterDemo#addLevelRangeFilterToLogger （LevelRangeFilter示例）
     * LevelRangeFilter判断日志级别是否在设置的级别范围内以决定Filter后的状态
     * 与LevelMatchFilter示例大同小异 这个filter是控制日志级别范围的 低于设定的最小级别则不进行日志记录同样
     * 高过最大级别也不进行日志记录 然后存在一个布尔值决定如果在范围内是返回ACCEPT还是NEUTRAL
     */

    private static Logger logger = Logger.getLogger(FIlterDemo.class);

    public static void main(String[] args) {

//        addDenyAllFilterToLogger();
//        addLevelMatchFilterToLogger();
//        addLevelRangeFilterToLogger();
//        addStringMatchFilterToLogger();

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

    /**
     * 添加Filter的示例
     * 对下列类方法进行断点即可看清具体逻辑
     *
     * @see DenyAllFilter#decide(LoggingEvent)
     */
    private static void addDenyAllFilterToLogger() {
        //取第一个appender作示例 配置文件中第一个配置的是ConsoleAppender 可迭代获取所有appender
        Appender appender = (Appender) logger.getParent().getAllAppenders().nextElement();
        appender.addFilter(new DenyAllFilter());
    }

    /**
     * 添加Filter的示例
     * 对下列类方法进行断点即可看清具体逻辑
     *
     * @see LevelRangeFilter#decide(LoggingEvent)
     */
    private static void addLevelRangeFilterToLogger() {
        LevelRangeFilter levelRangeFilter = new LevelRangeFilter();
        levelRangeFilter.setLevelMax(Level.ALL);
        levelRangeFilter.setLevelMin(Level.INFO);
        //取第一个appender作示例 配置文件中第一个配置的是ConsoleAppender 可迭代获取所有appender
        Appender appender = (Appender) logger.getParent().getAllAppenders().nextElement();
        appender.addFilter(levelRangeFilter);
    }

    /**
     * 添加Filter的示例
     * 对下列类方法进行断点即可看清具体逻辑
     *
     * @see LevelMatchFilter#decide(LoggingEvent)
     */
    private static void addLevelMatchFilterToLogger() {
        LevelMatchFilter levelMatchFilter = new LevelMatchFilter();
        levelMatchFilter.setLevelToMatch("DEBUG");
        //取第一个appender作示例 配置文件中第一个配置的是ConsoleAppender 可迭代获取所有appender
        Appender appender = (Appender) logger.getParent().getAllAppenders().nextElement();
        appender.addFilter(levelMatchFilter);
    }

    /**
     * 添加Filter的示例
     * 对下列类方法进行断点即可看清具体逻辑
     *
     * @see StringMatchFilter#decide(LoggingEvent)
     */
    private static void addStringMatchFilterToLogger() {
        StringMatchFilter filter = new StringMatchFilter();
        filter.setStringToMatch("helloworld");
        //取第一个appender作示例 配置文件中第一个配置的是ConsoleAppender 可迭代获取所有appender
        Appender appender = (Appender) logger.getParent().getAllAppenders().nextElement();
        appender.addFilter(filter);
    }
}
