package fengfei.forest.slice.database;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fengfei.forest.database.pool.BonePoolableDataSourceFactory;
import fengfei.forest.database.pool.DbcpPoolableDataSourceFactory;
import fengfei.forest.database.pool.DruidPoolableDataSourceFactory;
import fengfei.forest.database.pool.PoolableDataSourceFactory;
import fengfei.forest.database.pool.TomcatPoolableDataSourceFactory;
import fengfei.forest.slice.Equalizer;
import fengfei.forest.slice.Router;
import fengfei.forest.slice.config.Config;
import fengfei.forest.slice.config.Config.RouterConfig;
import fengfei.forest.slice.config.DefaultRouterFactory;
import fengfei.forest.slice.exception.NonExistedSliceException;

public class DatabaseRouterFactory extends DefaultRouterFactory {

	public DatabaseRouterFactory(Config config) {
		super(config);
	}

	private static final Logger logger = LoggerFactory
			.getLogger(DatabaseRouterFactory.class);
	public static final String Database = "database";
	public static final String POOL_NAME = "poolName";
	public static final String POOL_BONECP = "BoneCP";
	public static final String POOL_DBCP = "DBCP";
	public static final String POOL_TOMCAT_JDBC = "TomcatJDBC";
	public static final String POOL_DRUID = "Druid";
	protected static Map<String, Class<? extends ConnectonUrlMaker>> connectonUrlMakerClazz = new HashMap<>();
	protected Map<String, PoolableDatabaseRouter<?>> poolableRouterCache = new HashMap<>();
	protected Map<String, DatabaseRouter<?>> databaseRouterCache = new HashMap<>();
	static {

		registerDriver("oracle.jdbc.driver.OracleDriver",
				OracleThinConnectonUrlMaker.class);
		registerDriver("org.gjt.mm.mysql.Driver", MysqlConnectonUrlMaker.class);
		registerDriver("com.mysql.jdbc.Driver", MysqlConnectonUrlMaker.class);
		registerDriver("org.postgresql.Driver",
				PostgreSQLConnectonUrlMaker.class);

	}

	public Map<String, DatabaseRouter<?>> allDatabaseRouters() {
		return databaseRouterCache;
	}

	public Map<String, PoolableDatabaseRouter<?>> allPoolableRouters() {
		return poolableRouterCache;
	}

	public <Key> DatabaseRouter<Key> getDatabaseRouter(Equalizer<Key> plotter,
			String routerName) {
		@SuppressWarnings("unchecked")
		DatabaseRouter<Key> router = (DatabaseRouter<Key>) databaseRouterCache
				.get(routerName);
		if (router == null) {
			Router<Key> origin = getRouter(routerName);
			router = new DatabaseRouter<>(origin);
			databaseRouterCache.put(routerName, router);
		}

		return router;
	}

	public <Key> DatabaseRouter<Key> getDatabaseRouter(String routerName) {

		@SuppressWarnings("unchecked")
		DatabaseRouter<Key> router = (DatabaseRouter<Key>) databaseRouterCache
				.get(routerName);
		if (router == null) {
			Router<Key> origin = getRouter(routerName);
			router = new DatabaseRouter<>(origin);
			databaseRouterCache.put(routerName, router);
		}

		return router;
	}

	public <Key> PoolableDatabaseRouter<Key> getPoolableRouter(
			ConnectonUrlMaker urlMaker,
			PoolableDataSourceFactory poolableDataSourceFactory,
			Equalizer<Key> equalizer, String routerName) {
		@SuppressWarnings("unchecked")
		PoolableDatabaseRouter<Key> router = (PoolableDatabaseRouter<Key>) poolableRouterCache
				.get(routerName);
		if (router == null) {
			Router<Key> origin = getRouter(routerName);
			router = new PoolableDatabaseRouter<>(origin, urlMaker,
					poolableDataSourceFactory);
			router.setEqualizer(equalizer);
			poolableRouterCache.put(routerName, router);
		}

		return router;
	}

	public <Key> PoolableDatabaseRouter<Key> getPoolableRouter(
			ConnectonUrlMaker urlMaker,
			PoolableDataSourceFactory poolableDataSourceFactory,
			String routerName) {
		@SuppressWarnings("unchecked")
		PoolableDatabaseRouter<Key> router = (PoolableDatabaseRouter<Key>) poolableRouterCache
				.get(routerName);
		if (router == null) {
			Router<Key> origin = getRouter(routerName);
			router = new PoolableDatabaseRouter<>(origin, urlMaker,
					poolableDataSourceFactory);

			poolableRouterCache.put(routerName, router);
		}

		return router;
	}

	public <Key> PoolableDatabaseRouter<Key> getPoolableRouter(
			PoolableDataSourceFactory poolableDataSourceFactory,
			String routerName) {
		@SuppressWarnings("unchecked")
		PoolableDatabaseRouter<Key> router = (PoolableDatabaseRouter<Key>) poolableRouterCache
				.get(routerName);
		if (router == null) {
			ConnectonUrlMaker urlMaker = getConnectonUrlMaker(routerName);
			Router<Key> origin = getRouter(routerName);
			router = new PoolableDatabaseRouter<>(origin, urlMaker,
					poolableDataSourceFactory);
			poolableRouterCache.put(routerName, router);
		}

		return router;
	}

	public <Key> PoolableDatabaseRouter<Key> getPoolableRouter(String routerName) {
		@SuppressWarnings("unchecked")
		PoolableDatabaseRouter<Key> router = (PoolableDatabaseRouter<Key>) poolableRouterCache
				.get(routerName);
		if (router == null) {
			PoolableDataSourceFactory poolableDataSourceFactory = getPoolableDataSourceFactory(routerName);
			ConnectonUrlMaker urlMaker = getConnectonUrlMaker(routerName);
			Router<Key> origin = getRouter(routerName);
			router = new PoolableDatabaseRouter<>(origin, urlMaker,
					poolableDataSourceFactory);
			poolableRouterCache.put(routerName, router);
		}

		return router;
	}

	//
	public PoolableDataSourceFactory getPoolableDataSourceFactory(
			String routerName) {
		RouterConfig router = routerConfigCache.get(routerName);

		if (router == null) {
			throw new NonExistedSliceException("routerName=" + routerName);
		}
		Map<String, String> info = router.defaultExtraInfo;
		String poolName = info.get(POOL_NAME);
		if (null == poolName || "".equals(poolName)) {
			poolName = POOL_DRUID;
		}
		switch (poolName) {
		case POOL_BONECP:
			return new BonePoolableDataSourceFactory();
		case POOL_DBCP:
			return new DbcpPoolableDataSourceFactory();
		case POOL_TOMCAT_JDBC:
			return new TomcatPoolableDataSourceFactory();
		case POOL_DRUID:
			return new DruidPoolableDataSourceFactory();
		default:
			logger.warn("Can't supported pool, default using Druid.");
			return new DruidPoolableDataSourceFactory();
		}

	}

	public static void registerDriver(String driverName,
			Class<? extends ConnectonUrlMaker> clazz) {
		connectonUrlMakerClazz.put(driverName, clazz);
	}

	public ConnectonUrlMaker getConnectonUrlMaker(String routerName) {
		RouterConfig routerConfig = routerConfigCache.get(routerName);

		if (routerConfig == null) {
			throw new NonExistedSliceException("routerName=" + routerName);
		}
		Map<String, String> info = routerConfig.defaultExtraInfo;
		String driverName = info.get(ServerResource.KEY_DRIVER_CLASS);
		Class<? extends ConnectonUrlMaker> clazz = connectonUrlMakerClazz
				.get(driverName);
		if (null == clazz) {
			String msg = "Not registered ConnectonUrlMaker for driver: "
					+ driverName;
			logger.error(msg);
			throw new RuntimeException(msg);
		}
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error("create ConnectonUrlMaker error", e);
			throw new RuntimeException(e);
		}

	}
}