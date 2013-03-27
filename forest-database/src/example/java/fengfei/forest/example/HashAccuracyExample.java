package fengfei.forest.example;

import java.util.HashMap;
import java.util.Map;

import fengfei.forest.database.pool.TomcatPoolableDataSourceFactory;
import fengfei.forest.slice.OverflowType;
import fengfei.forest.slice.SliceResource;
import fengfei.forest.slice.SliceResource.Function;
import fengfei.forest.slice.Router;
import fengfei.forest.slice.SelectType;
import fengfei.forest.slice.database.MysqlConnectonUrlMaker;
import fengfei.forest.slice.database.PoolableDatabaseRouter;
import fengfei.forest.slice.impl.AccuracyRouter;
import fengfei.forest.slice.impl.LongEqualizer;

public class HashAccuracyExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AccuracyRouter<Long> faced = new AccuracyRouter<>(new LongEqualizer());
		PoolableDatabaseRouter<Long> router = new PoolableDatabaseRouter<>(
				faced, new MysqlConnectonUrlMaker(),
				new TomcatPoolableDataSourceFactory());
		setupGroup(router);
		router.setSelectType(SelectType.Hash);
		router.setOverflowType(OverflowType.Last);
		System.out.println(router);
		//
		System.out.println(router.locate(1l));
		System.out.println(router.locate(2l));
		System.out.println(router.locate(1l));
		System.out.println(router.locate(2l));
		System.out.println(router.locate(1l, Function.Read));
		System.out.println(router.locate(2l, Function.Read));
		System.out.println(router.locate(1l, Function.Read));
		System.out.println(router.locate(2l, Function.Read));
		System.out.println(router.locate(1l, Function.Write));
		System.out.println(router.locate(2l, Function.Write));
		System.out.println(router.locate(11l, Function.Write));
		System.out.println(router.locate(11l, Function.Write));
	}

	private static void setupGroup(Router<Long> router) {
		int ip = 2;
		for (int i = 0; i < 50; i++) {

			for (int j = 0; j < 3; j++) {
				String name = "192.168.1." + (ip++) + ":8002";
				SliceResource resource = new SliceResource(name);
				resource.setFunction(j == 0 ? Function.Write : Function.Read);
				resource.addParams(extraInfo());
				router.register(Long.valueOf(i), resource);
			}

		}
	}

	private static Map<String, String> extraInfo() {
		Map<String, String> extraInfo = new HashMap<String, String>();

		extraInfo.put("driverClass", "com.mysql.jdbc.Driver");
		extraInfo.put("user", "root");
		extraInfo.put("password", "");
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