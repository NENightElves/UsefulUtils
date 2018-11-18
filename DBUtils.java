package mytools;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;

import com.mchange.v2.c3p0.ComboPooledDataSource;



public class DBUtils {
	/**
	 * 使用数据库连接池
	 */
	private static DataSource dataSource = null;
	static {

		try {
			Properties prop = new Properties();
			String path = DBUtils.class.getResource("/").getPath();
//			System.out.println("asd " + path);
			InputStream in;
			in = new FileInputStream(path + "jdbc.properties");
			prop.load(in);
			ComboPooledDataSource com = new ComboPooledDataSource();
			com.setUser(prop.getProperty("user"));
			com.setJdbcUrl(prop.getProperty("jdbcUrl"));
			com.setPassword(prop.getProperty("password"));
			com.setDriverClass(prop.getProperty("driverClass"));
			com.setInitialPoolSize(Integer.parseInt(prop.getProperty("initialPoolSize")));
			com.setMaxPoolSize(Integer.parseInt(prop.getProperty("maxPoolSize")));
			com.setMinPoolSize(Integer.parseInt(prop.getProperty("minPoolSize")));
			com.setMaxIdleTime(Integer.parseInt(prop.getProperty("maxIdleTime")));
			com.setAcquireIncrement(Integer.parseInt(prop.getProperty("acquireIncrement")));
			com.setMaxStatements(Integer.parseInt(prop.getProperty("maxStatements")));
			com.setMaxStatementsPerConnection(Integer.parseInt(prop.getProperty("maxStatementsPerConnection")));
			dataSource = com;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * des 得到数据库的连接
	 * 
	 * @return
	 * @throws Exception
	 */
	public static Connection getConnection() throws Exception {
		return dataSource.getConnection();
	}

	/**
	 * @des 关闭数据库的连接
	 * @param conn
	 * @param pre
	 * @param rs
	 */
	public static void close(Connection conn, Statement sta, ResultSet rs) {
		try {
			if (rs != null)
				rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (sta != null)
				sta.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (conn != null)
				conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param sql语句（增删改）
	 * @param 任意多个参数
	 * @return 受影响的条数
	 * @throws Exception
	 */
	public static int executeUpdate(String sql, Object... args) {
		Connection conn = null;
		PreparedStatement pre = null;
		int count = 0;
		try {
			conn = getConnection();
			pre = conn.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				pre.setObject(i + 1, args[i]);
			}
			count = pre.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(conn, pre, null);
		}
		return count;
	}

	/**
	 * 将一个对象插入到数据库中
	 * 
	 * @param obj
	 * @return
	 */
	public static int insertObject(Object obj) {
		System.out.println(obj.getClass().getSimpleName());
		String tablename = obj.getClass().getSimpleName();
		Class<?> clazz = obj.getClass();
		Field[] fields = clazz.getDeclaredFields();

		StringBuffer sql = new StringBuffer("insert into " + tablename + "(");
		for (int i = 0; i < fields.length; i++) {
			sql.append(fields[i].getName());
			if (i != fields.length - 1)
				sql.append(",");
		}
		sql.append(") values ( ");
		for (int i = 0; i < fields.length; i++) {
			try {
				sql.append("'" + BeanUtils.getProperty(obj, fields[i].getName()) + "'");
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (i != fields.length - 1)
				sql.append(",");
		}
		sql.append(" )");
		System.out.println(sql.toString());
		return executeUpdate(sql.toString());
	}

	/**
	 * 
	 * @param sql语句（增删改）
	 * @param 任意多个参数
	 * @return 受影响的条数
	 * @throws Exception
	 */
	public static int executeUpdateSupportTransaction(Connection conn, String sql, Object... args) {
		PreparedStatement pre = null;
		int count = 0;
		try {
			pre = conn.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				pre.setObject(i + 1, args[i]);
			}
			count = pre.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(null, pre, null);
		}
		return count;
	}

	/**
	 * @des 插入数据，返回的是自增长主键的id
	 * @param sql
	 * @param args
	 * @return
	 */
	public static int insertAndGetAutoId(String sql, Object... args) {
		Connection conn = null;
		PreparedStatement pre = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			pre = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			for (int i = 0; i < args.length; i++) {
				pre.setObject(i + 1, args[i]);
			}
			pre.executeUpdate();
			rs = pre.getGeneratedKeys();
			if (rs.next())
				return rs.getInt(1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(conn, pre, rs);
		}
		return 0;
	}

	/**
	 * @require javabean中的属性名必须和数据表中的字段名完全一样
	 * @param c
	 * @param sql
	 * @param args
	 * @return Object or null
	 */
	public static <T> T getOneData(Class<T> c, String sql, Object... args) {
		Connection conn = null;
		PreparedStatement pre = null;
		ResultSet rs = null;
		T entity = null;
		try {
			conn = getConnection();
			pre = conn.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				pre.setObject(i + 1, args[i]);
			}
			rs = pre.executeQuery();
			if (rs.next()) {
				entity = c.newInstance();
				// 获得元数据,包含结果集的列数，属性名，以及别名等信息。
				ResultSetMetaData md = rs.getMetaData();
				int columnCount = md.getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					String key = md.getColumnName(i);
					Object value = rs.getObject(key);
					// Field field = c.getDeclaredField(key);
					// field.setAccessible(true);
					// field.set(entity, value);
					BeanUtils.setProperty(entity, key, value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(conn, pre, rs);
		}
		return entity;
	}

	/**
	 * @require javabean中的属性名必须和数据表中的字段名完全一样
	 * @param c
	 * @param sql
	 * @param args
	 * @return list
	 */
	public static <T> List<T> getListData(Class<T> c, String sql, Object... args) {
		Connection conn = null;
		PreparedStatement pre = null;
		ResultSet rs = null;
		List<T> list = new ArrayList<>();
		try {
			conn = getConnection();
			pre = conn.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				pre.setObject(i + 1, args[i]);
			}
			rs = pre.executeQuery();
			ResultSetMetaData md = rs.getMetaData();

			while (rs.next()) {
				T entity = c.newInstance();
				// 获得元数据,包含结果集的列数，属性名，以及别名等信息。
				int columnCount = md.getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					String key = md.getColumnName(i);
					Object value = rs.getObject(key);
					// Field field = c.getDeclaredField(key);
					// field.setAccessible(true);
					// field.set(entity, value);
					BeanUtils.setProperty(entity, key, value);

				}
				list.add(entity);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(conn, pre, rs);
		}
		return list;
	}

	/**
	 * 
	 * @param sql
	 * @param args
	 * @return 得到某一列的值如count(*),max(),min()之类的
	 */
	public static Object getOneColum(String sql, Object... args) {
		Connection conn = null;
		PreparedStatement pre = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			pre = conn.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				pre.setObject(i + 1, args[i]);
			}
			rs = pre.executeQuery();
			if (rs.next()) {
				return rs.getObject(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(conn, pre, rs);
		}
		return null;
	}
	/**
	 * 
	 * @param sql
	 * @param args
	 * @return 得到某张图片
	 */
	public static Blob getOnePic(String sql, Object... args) {
		Connection conn = null;
		PreparedStatement pre = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			pre = conn.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				pre.setObject(i + 1, args[i]);
			}
			rs = pre.executeQuery();
			if (rs.next()) {
				return rs.getBlob(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(conn, pre, rs);
		}
		return null;
	}
	public static void main(String[] args) throws ClassNotFoundException {
		String sql = "select * from user where id = ?";
		//System.out.println(DBUtils.getOneData(User.class, sql, 1));
	}
}
