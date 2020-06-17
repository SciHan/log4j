log.info(...)将信息打印到配置的目的地，如果想要将所有的异常信息也打印到日志文件中，
那么就得要捕获异常如下
try{
    ...
}catch(Exception e){
    log.error(e.getMessage,e)
}
但不可能手动捕获所有异常，一般都结合spring aop统一捕获，在配置类中写入 log.error(e.getMessage,e)即可。