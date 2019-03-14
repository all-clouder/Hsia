package com.Hsia.sharding.aop;

import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import com.Hsia.sharding.dataSource.DataSourceContextHolder;
import com.Hsia.sharding.route.Route;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.Hsia.sharding.dataSource.DatasourceGroup;
import com.Hsia.sharding.dataSource.SqlContextHolder;
import com.Hsia.sharding.exceptions.SqlParserException;
import com.Hsia.sharding.parser.SqlResolve;
import com.Hsia.sharding.route.MybatisShardingRouteFactory;
import com.Hsia.sharding.route.ShardingRule;
import com.Hsia.sharding.utils.ShardingUtil;

/**
 * 注意：使用本拦截器，请将系统中自依赖的mybatis-x.x.x.jar屏蔽掉，否则，拦截器使用的mybatis版本将会被覆盖，导致功能缺失，分库分表无法实现
 * @author qsl
 *
 */
@Intercepts({
	@Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }),
	@Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class }),
	@Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class })
})
public class MybatisExecuterInterceptor implements Interceptor {

	private Logger logger = Logger.getLogger(MybatisExecuterInterceptor.class);

	@Autowired
	private ShardingRule shardingRule;

	@Autowired
	private DataSourceContextHolder dataSourceHolder;

	@Autowired
	private MybatisShardingRouteFactory shardingRouteFactory;

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		Object result = null;
		Object[] params = invocation.getArgs();
		MappedStatement mappedStatement = (MappedStatement) params[0];
		try {

			/* 执行路由检测 */
			if (this.isDataSource(mappedStatement)) {
				@SuppressWarnings("unchecked")
				Map<String, Object> parameterMap = (Map<String, Object>) params[1];
				BoundSql boundSql = mappedStatement.getBoundSql(parameterMap);
				String srcSql = boundSql.getSql();

				if (shardingRule.getDbQuantity() > 1){// 多库 分表
					//创建一个数组
					String routeKey = shardingRule.getRouteKey();
					Object routeValue = parameterMap.get(routeKey);
					if(routeValue == null){
						//如果没有路由主键，数据库将会进行全部扫描，性能低下，建议通过第三方系统确认路由主键
						//本插件将会报错 the route value must not be null
						throw new SqlParserException("the route value must not be null, please set it. ");
					}
					logger.debug("the sql is "+srcSql+" and the route key is : "+routeKey + " and the route value is : "+routeValue);

					Object[] p = new Object[]{srcSql, routeValue}; 

					Route shardingRoute = shardingRouteFactory.getRoute();
					Object[] sql = shardingRoute.route(p, SqlResolve.sqlIsUpdate(srcSql));
					String targetSql = (String)sql[0];// 获取目标sql
					/*
					 * org.apache.ibatis.executor.statement.PreparedStatementHandler.instantiateStatement(Connection connection)
					 */
					//TODO 将sql保存在 ThreadLocal中， 以便修改sql 
					SqlContextHolder.getInstance().setExecuteSql(targetSql);


				} else {// 单库 分表
					final int index = ShardingUtil.getBeginIndex(shardingRule, SqlResolve.sqlIsUpdate(srcSql));
					dataSourceHolder.setDataSourceIndex(index);
				}
			}

			result = invocation.proceed();

		} catch (Exception e) {
			logger.error("there is something wrong...", e);
			throw e;
		}
		return result;

	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {
	}
	/**
	 * 
	 * @Title: isDataSource 
	 * @Description: 路由检测 
	 * @param @param proceedingJoinPoint
	 * @param @return    设定文件 
	 * @return boolean    返回类型 
	 * @throws
	 */
	public synchronized boolean isDataSource(MappedStatement mappedStatement) {
		boolean flag = false;
		try {
			Configuration configuration = mappedStatement.getConfiguration();
			Environment environment = configuration.getEnvironment();
			DataSource dataSource = environment.getDataSource();
			//如果SqlSessionTemplate持有的不是 com.qishiliang.sharding.route.impl.DatasourceGroup 动态数据源,则不进行数据路由操作 
			flag = dataSource instanceof DatasourceGroup;
			logger.debug(flag ? "the datasource IS sharding datasource." : "the datasource IS NOT sharding datasource.");
		} catch (Exception e) {
			throw new SqlParserException("the datasource is wrong.", e);
		}
		return flag;
	}

}