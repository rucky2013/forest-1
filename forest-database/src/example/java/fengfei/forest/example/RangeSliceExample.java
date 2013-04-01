package fengfei.forest.example;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import fengfei.forest.database.dbutils.ForestGrower;
import fengfei.forest.database.dbutils.impl.DefaultForestGrower;
import fengfei.forest.database.pool.TomcatPoolableDataSourceFactory;
import fengfei.forest.slice.Equalizer;
import fengfei.forest.slice.OverflowType;
import fengfei.forest.slice.Resource;
import fengfei.forest.slice.Router;
import fengfei.forest.slice.SelectType;
import fengfei.forest.slice.SliceResource;
import fengfei.forest.slice.SliceResource.Function;
import fengfei.forest.slice.database.MysqlConnectonUrlMaker;
import fengfei.forest.slice.database.PoolableDatabaseRouter;
import fengfei.forest.slice.impl.NavigableRouter;

public class RangeSliceExample {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		PoolableDatabaseRouter<Long> router = new PoolableDatabaseRouter<>(
				new NavigableRouter<Long>(), new MysqlConnectonUrlMaker(),
				new TomcatPoolableDataSourceFactory());

		router.setSelectType(SelectType.Hash);
		router.setOverflowType(OverflowType.Last);
		router.setEqualizer(new Equalizer<Long>() {
			@Override
			public long get(Long key, int sliceSize) {
				return key;
			}
		});
		
		
		setupGroup(router);

		System.out.println(router);
		System.out.println(router.locate(101l).getDataSource().toString());
		System.out.println(router.locate(2l));
		System.out.println(router.locate(2635l));
		System.out.println(router.locate(20000l));
		System.out.println(router.locate(12l, Function.Read));
		System.out.println(router.locate(2896l, Function.Read));
		System.out.println(router.locate(19l, Function.Read));
		System.out.println(router.locate(24642l, Function.Read));
		System.out.println(router.locate(9912l, Function.Write));
		System.out.println(router.locate(9720l, Function.Write));
		System.out.println(router.locate(9701l, Function.Write));
		System.out.println(router.locate(11000l, Function.Write));
	}

	private static void setupGroup(Router<Long> router) {
		int ip = 2;
		int maxMod = 1024;
		int sliceSize = 3;
		for (int i = 0; i < sliceSize; i++) {
			Long sliceId = Long.valueOf((i + 1) * 1980);
			for (int j = 0; j < 6; j++) {
				// String name = "192.168.1." + (ip++) + ":3306";
				String name = "127.0.0.1:3306";
				Resource resource = new Resource(name);
				SliceResource sliceResource = new SliceResource(resource);
				sliceResource.setFunction(j < 2 ? Function.Write
						: Function.Read);
				sliceResource.addParams(extraInfo());
				router.register(sliceId, String.valueOf(i), sliceResource);
				// System.out.println(sliceResource);
			}
		}
	}

	private static Map<String, String> extraInfo() {
		Map<String, String> extraInfo = new HashMap<String, String>();
		extraInfo.put("driverClass", "com.mysql.jdbc.Driver");
		extraInfo.put("user", "root");
		extraInfo.put("password", "");
		extraInfo.put("host", "127.0.0.1");
		extraInfo.put("port", "3306");
		// pool config
		extraInfo.put("maxActive", "10");
		extraInfo.put("maxIdle", "10");
		extraInfo.put("minIdle", "1");
		extraInfo.put("initialSize", "2");
		extraInfo.put("maxWait", "30000");
		extraInfo.put("testOnBorrow", "true");
		return extraInfo;
	}
}
