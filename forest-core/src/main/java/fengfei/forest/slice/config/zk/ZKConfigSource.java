package fengfei.forest.slice.config.zk;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fengfei.berain.client.BerainEntry;
import fengfei.berain.client.BerainWatchedEvent;
import fengfei.berain.client.Wather;
import fengfei.berain.client.zk.ZkBerainClient;
import fengfei.forest.slice.config.ConfigSource;
import fengfei.forest.slice.utils.ResourcesUtils;

public class ZKConfigSource implements ConfigSource {

    static Logger log = LoggerFactory.getLogger(ZKConfigSource.class);
    ZkBerainClient client;
    String namespace = "";

    public ZKConfigSource() {
        this(null);
    }

    public ZKConfigSource(String zkProperties) {

        try {
            Properties properties = null;
            if (zkProperties == null) {
                properties = ResourcesUtils.getResourceAsProperties("zk.properties");
            } else {
                properties = new Properties();
                properties.load(new FileInputStream(zkProperties));
            }

            init(properties);
        } catch (IOException e) {
            log.error("init zookeeper error.", e);
        }

    }

    void init(Properties properties) {
        client = new ZkBerainClient(properties);
        client.start();
        namespace = client.getClient().getNamespace();
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public List<BerainEntry> children(String parentPath) throws Exception {
        return client.nextChildren(parentPath);
    }

    @Override
    public List<String> listChildrenPath(String parentPath) throws Exception {
        return client.getClient().getChildren().forPath(parentPath);
    }

    @Override
    public Map<String ,Map<String, String>> listChildren(String parentPath) throws Exception {
        Map<String ,Map<String, String>> entries = new HashMap<>();
        List<String> list = listChildrenPath(parentPath);
        if (list != null)
            for (String path : list) {
                Map<String, String> entry = new HashMap<>();
                //
                String pvalue = get(path);
                //
                entry.put(ValueKey, pvalue);
                List<BerainEntry> childNodes = children(path);
                if (childNodes != null)
                    for (BerainEntry be : childNodes) {
                        entry.put(be.getKey(), be.getValue());
                    }
                entries.put(path, entry);
            }
        return entries;
    }

    @Override
    public String get(String path) throws Exception {
        return client.get(path);
    }

    @Override
    public BerainEntry getFull(String path) throws Exception {
        return client.getFull(path);
    }

    @Override
    public boolean exists(String path) throws Exception {
        return client.exists(path);
    }

    @Override
    public void addMonitor(String path, final Monitor monitor) throws Exception {
        client.addNodeChangedWatcher(path, new Wather() {

            @Override
            public void call(BerainWatchedEvent event) {
                monitor.onDataChanged(event);
            }
        });
        client.addChildrenChangedWatcher(path, new Wather() {

            @Override
            public void call(BerainWatchedEvent event) {
                monitor.onChildrenChanged(event);
            }
        });

    }

}
