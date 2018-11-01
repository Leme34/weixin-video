package com.lee.utils;

import com.alibaba.druid.util.StringUtils;
import com.google.gson.Gson;
import com.lee.enums.BGMOperatorTypeEnum;
import org.apache.commons.io.FileUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class ZKCuratorClient {

    // zk客户端
    private CuratorFramework client = null;
    final static Logger log = LoggerFactory.getLogger(ZKCuratorClient.class);

//	@Autowired
//	private BgmService bgmService;

//	public static final String ZOOKEEPER_SERVER = "192.168.1.210:2181";

    @Value("${ZOOKEEPER_SERVER}")
    private String ZOOKEEPER_SERVER;
    @Value("${FILE_SPACE}")
    private String FILE_SPACE;
    @Value("${BGM_SERVER}")
    private String BGM_SERVER;

    /**
     * 启动客户端,监听zk节点的事件(实现后台上传bgm同步到此服务器中)
     */
    public void init() {

        if (client != null) {
            return;
        }
        // 重试策略
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);
        // 创建zk客户端
        client = CuratorFrameworkFactory.builder().
                connectString(ZOOKEEPER_SERVER)   //zk所在url
                .sessionTimeoutMs(10000)
                .retryPolicy(retryPolicy)    //重试策略
                .namespace("admin")  //命名空间
                .build();
        // 启动客户端
        client.start();
        try {
//			String testNodeData = new String(client.getData().forPath("/bgm/18052674D26HH3X4"));
//			log.info("测试的节点数据为： {}", testNodeData);

            //监听此节点下的所有子节点
            addChildWatch("/bgm");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听节点
     */
    public void addChildWatch(String nodePath) throws Exception {
        //对此节点下子节点的状态信息进行缓存
        final PathChildrenCache cache = new PathChildrenCache(client, nodePath, true);
        cache.start();

        //添加监听器
        cache.getListenable().addListener((client, event) -> {
            //监听新增子节点事件
            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                log.info("监听到事件 CHILD_ADDED");

                // 1. 从数据库查询bgm对象
                //获取新增子节点事件所在路径path
                String path = event.getData().getPath();
                //获取新增子节点中的json数据(枚举类型的操作值,数据库中bgm的本地相对路径)
                String operatorObjStr = new String(event.getData().getData());
                Gson gson = new Gson();
                Map<String, String> map = gson.fromJson(operatorObjStr, Map.class);
                String operatorType = map.get("operType");
                String songPath = map.get("path");
                //因为ssm后台已删除，所以不能从数据库中查询bgm的本地相对路径
//					Bgm bgm = bgmService.queryBgmById(bgmId);
//					if (bgm == null) {
//						return;
//					}
                // 1.1 bgm所在的相对路径
//					String songPath = bgm.getPath();
                // 2. 定义保存到本地的bgm路径
//					String filePath = "C:\\imooc_videos_dev" + songPath;
                String filePath = FILE_SPACE + songPath;
                // 3. 定义下载的路径（播放url）
                String arrPath[] = songPath.split("\\\\");  //以"\\"分割url,java中转义后是"\\\\"
                String finalPath = "";  //拼接成数据库中保存的url
                // 3.1 处理url的斜杠以及编码
                for (int i = 0; i < arrPath.length; i++) {
                    if (!StringUtils.isEmpty(arrPath[i])) {
                        finalPath += "/";
                        //对含有中文的路径编码
                        finalPath += URLEncoder.encode(arrPath[i], "UTF-8");
                    }
                }
//					String bgmUrl = "http://192.168.1.2:8080/mvc" + finalPath;

                //网络可访问的bgm文件的完整url: http://192.168.1.2:8080/mvc/bgm/music.mp3
                String bgmUrl = BGM_SERVER + finalPath;

                //若新增的节点中存放的操作类型是"1"(添加bgm)
                if (operatorType.equals(BGMOperatorTypeEnum.ADD.type)) {
                    // 下载bgm到spingboot服务器
                    URL url = new URL(bgmUrl);
                    File file = new File(filePath);
                    FileUtils.copyURLToFile(url, file);
                    //删除zk中的此子节点
                    client.delete().forPath(path);
                }
                //若新增的节点中存放的操作类型是"2"(删除bgm)
                else if (operatorType.equals(BGMOperatorTypeEnum.DELETE.type)) {
                    //删除bgm文件
                    File file = new File(filePath);
                    FileUtils.forceDelete(file);
                    //删除zk中的此子节点
                    client.delete().forPath(path);
                }
            }
        });
    }

}
