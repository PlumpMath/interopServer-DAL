/*
 * dalserver-interop library - implementation of DAL server for interoperability
 * Copyright (C) 2015  Diversity Arrays Technology
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.diversityarrays.dal.db.bms;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.pearcan.util.StringUtil;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.ClosureUtils;

import com.diversityarrays.dal.db.AbstractDalDatabase;
import com.diversityarrays.dal.db.AuthenticationException;
import com.diversityarrays.dal.db.CollectionEntityIterator;
import com.diversityarrays.dal.db.DalDatabaseUtil;
import com.diversityarrays.dal.db.DalDbException;
import com.diversityarrays.dal.db.DalResponseBuilder;
import com.diversityarrays.dal.db.DbUtil;
import com.diversityarrays.dal.db.EntityIterator;
import com.diversityarrays.dal.db.EntityProvider;
import com.diversityarrays.dal.db.RecordCountCache;
import com.diversityarrays.dal.db.RecordCountCacheEntry;
import com.diversityarrays.dal.db.RecordCountCacheImpl;
import com.diversityarrays.dal.db.ResultSetEntityIterator;
import com.diversityarrays.dal.db.SystemGroupInfo;
import com.diversityarrays.dal.db.UserInfo;
import com.diversityarrays.dal.entity.DalEntity;
import com.diversityarrays.dal.entity.Genotype;
import com.diversityarrays.dal.entity.GenotypeAlias;
import com.diversityarrays.dal.entity.Genus;
import com.diversityarrays.dal.ops.DalOperation;
import com.diversityarrays.dal.server.DalSession;
import com.diversityarrays.dal.service.DalDbNotYetImplementedException;
import com.diversityarrays.dal.sqldb.JdbcConnectionParameters;
import com.diversityarrays.dal.sqldb.ResultSetVisitor;
import com.diversityarrays.dal.sqldb.SqlUtil;
import com.diversityarrays.dalclient.SessionExpiryOption;
import com.diversityarrays.util.Continue;
import com.diversityarrays.util.Either;

/**
 * Provides an implementation of DalDatabase that understands the
 * old BMS database schema (GMS).
 * @author brian
 *
 */
public class BMS_DalDatabase extends AbstractDalDatabase {
	
	private static final String DATABASE_VERSION = "0.1";
	
	static class BMS_SystemGroupInfo implements SystemGroupInfo {
		
		private final String groupId;
		private final String groupName;
		private final boolean groupOwner;

		BMS_SystemGroupInfo(String groupId, String groupName, boolean groupOwner) {
			this.groupId = groupId;
			this.groupName = groupName;
			this.groupOwner = groupOwner;
		}

		@Override
		public String getGroupId() {
			return groupId;
		}

		@Override
		public String getGroupName() {
			return groupName;
		}

		@Override
		public boolean isGroupOwner() {
			return groupOwner;
		}
		
	}
	
	static class BMS_UserInfo implements UserInfo {

		private String userName;
		private String userId;
		
		public final int instalid;
		public final int ustatus;
		public final int utype;
		public final int personid;
		public final String adate;
		
		BMS_UserInfo(String nm, int id, 
				int instalid, int ustatus, int uaccess, int utype, int pid, String adate) 
		{
			this.userName = nm;
			this.userId = Integer.toString(id);
			
			this.instalid = instalid;
			this.ustatus = ustatus;
			this.utype = utype;
			this.personid = pid;
			this.adate = adate;
		}

		@Override
		public String getUserName() {
			return userName;
		}

		@Override
		public String getUserId() {
			return userId;
		}
		
	}
	
	// GenotypeAliasType:
	// SELECT fcode, fname, fldno, COUNT(*) FROM UDFLDS
	// WHERE ftable='NAMES' AND ftype='NAME'
	
	// Need to choose one as the primary name
	// Use ordered from the parameter
		

	public static final Closure<String> REPORT_PROGRESS = new Closure<String>() {
		String prefix = BMS_DalDatabase.class.getSimpleName() + ": ";
		@Override
		public void execute(String msg) {
			System.out.println(prefix + msg);
		}
	};
	

	static DalDbException getDalDbException(Either<Throwable,?> either) {
		if (either.isRight()) {
			return new DalDbException("Internal error: getDalDbException() called with no error");
		}
		Throwable t = either.left();
		if (t instanceof DalDbException) {
			return (DalDbException) t;
		}
		return new DalDbException(t);
	}
	
	private final JdbcConnectionParameters localParams;
	private final JdbcConnectionParameters centralParams;
	
	private BmsConnectionInfo bmsConnections;

	private List<DalOperation> operations;
	
	private Map<String,BMS_UserInfo> userInfoBySessionId = new HashMap<String, BMS_UserInfo>();
	
	private Map<String,Class<? extends DalEntity>> entityClassByName = new HashMap<String,Class<? extends DalEntity>>();

	private EntityProvider<Genus> genusProvider = new EntityProvider<Genus>() {
		
		@Override
		public Genus getEntity(String id, String filterClause) throws DalDbNotYetImplementedException {
			if (filterClause != null) {
				throw new DalDbNotYetImplementedException("Filtering clause for genus");
			}
			// TODO use the genus table
			return bmsConnections.genusStore.getGenusById(id);
		}
		
		@Override
		public EntityIterator<? extends Genus> createIterator(int firstRecord, int nRecords, String filterClause) throws DalDbNotYetImplementedException {
			if (filterClause != null) {
				throw new DalDbNotYetImplementedException("Filtering clause for genus");
			}
			// TODO use the genus table
			return new CollectionEntityIterator<Genus>(bmsConnections.genusStore.getGenusValues());
		}

		@Override
		public int getEntityCount(String filterClause) throws DalDbNotYetImplementedException {
			if (filterClause != null) {
				throw new DalDbNotYetImplementedException("Filtering clause for genus");
			}
			// TODO use the genus table
			return bmsConnections.genusStore.getGenusCount();
		}

		@Override
		public EntityIterator<? extends Genus> createIdIterator(String id,
				int firstRecord, int nRecords, String filterClause)
		throws DalDbException {
			throw new UnsupportedOperationException();
		}
	};
	
	private EntityProvider<Genotype> genotypeProvider = new EntityProvider<Genotype>() {

		private GenotypeFactory createFactory() {
			return new GenotypeFactory(bmsConnections.genusStore);
		}

		@Override
		public int getEntityCount(String filterClause) throws DalDbException {
			String sql = createFactory().createCountQuery(filterClause);
			int total = 0;
			for (Connection c : bmsConnections.getConnections()) {
				total += SqlUtil.getSingleInteger(c, sql);
			}
			return total;
		}
		
		@Override
		public Genotype getEntity(String id, String filterClause) throws DalDbException {

			final GenotypeFactory factory = createFactory();

			try {
				final Genotype[] result = new Genotype[1];

				ResultSetVisitor visitor = new ResultSetVisitor() {
					@Override
					public Continue visit(ResultSet rs) {
						try {
							result[0] = factory.createEntity(rs);
						} catch (DalDbException e) {
							return Continue.error(e);
						}
						return Continue.STOP; // only the first
					}
				};
				
				String sql = factory.createGetQuery(id, filterClause);
				
				Connection c = bmsConnections.getConnectionFor(id);
				if (c != null) {
					Continue cont = SqlUtil.performQuery(c, sql, visitor);
					if (cont.isError()) {
						Throwable t = cont.throwable;
						if (t instanceof DalDbException) {
							throw ((DalDbException) t);
						}
						throw new DalDbException(t);
					}
				}

				return result[0];
			}
			finally {
				try { factory.close(); } 
				catch (IOException ignore) { }
			}
		}
		
		@Override
		public EntityIterator<? extends Genotype> createIterator(int firstRecord, int nRecords, String filterClause) throws DalDbException {
			
			String whereClause = GenotypeFactory.buildWhereAndLimit(filterClause, nRecords, firstRecord);
			
			StringBuilder sb = GenotypeFactory.createBaseQuery("g", "a", bmsConnections.getFldnoForGenus(), whereClause);
			
			String sql = sb.toString();
			
			GenotypeFactory factory = createFactory();
			
			try {
				Statement stmt = SqlUtil.createQueryStatement(bmsConnections.centralConnection);
				ResultSet rs = stmt.executeQuery(sql);
				
				
				return new ResultSetEntityIterator<Genotype>(stmt, rs, factory);
			} catch (SQLException e) {
				throw new DalDbException(e);
			} finally {
				try { factory.close(); } 
				catch (IOException ignore) { }
			}
		}

		@Override
		public EntityIterator<? extends Genotype> createIdIterator(String id,
				int firstRecord, int nRecords, String filterClause)
		throws DalDbException {
			throw new UnsupportedOperationException();
		}

	};

	private EntityProvider<GenotypeAlias> genotypeAliasProvider = new EntityProvider<GenotypeAlias>() {
		
		GenotypeAliasFactory genotypeAliasFactory = new GenotypeAliasFactory();
		
		@Override
		public int getEntityCount(String filterClause) throws DalDbException {
			String sql = genotypeAliasFactory.createCountQuery(filterClause);
			// TODO count across both connections
			return SqlUtil.getSingleInteger(bmsConnections.centralConnection, sql);
		}
		
		@Override
		public GenotypeAlias getEntity(String id, String filterClause) throws DalDbException {
		
			final GenotypeAlias[] result = new GenotypeAlias[1];

			ResultSetVisitor visitor = new ResultSetVisitor() {
				@Override
				public Continue visit(ResultSet rs) {
					try {
						result[0] = genotypeAliasFactory.createEntity(rs);
					} catch (DalDbException e) {
						return Continue.error(e);
					}
					return Continue.STOP; // only the first
				}
			};

			String sql = genotypeAliasFactory.createGetQuery(id, filterClause);

			// TODO query across both? but what about the JOIN?
			Continue cont = SqlUtil.performQuery(bmsConnections.centralConnection,
					sql,
					visitor);

			if (cont.isError()) {
				Throwable t = cont.throwable;
				if (t instanceof DalDbException) {
					throw ((DalDbException) t);
				}
				throw new DalDbException(t);
			}

			return result[0];
		}
		
		@Override
		public EntityIterator<? extends GenotypeAlias> createIterator(
				int firstRecord, int nRecords, String filterClause)
		throws DalDbException {

			String sql = genotypeAliasFactory.createPagedListQuery(firstRecord, nRecords, filterClause);
			
			try {
				// TODO query across both? but what about the JOIN?
				Statement stmt = SqlUtil.createQueryStatement(bmsConnections.centralConnection);
				ResultSet rs = stmt.executeQuery(sql);
				
				return new ResultSetEntityIterator<GenotypeAlias>(stmt, rs, genotypeAliasFactory);
			} catch (SQLException e) {
				throw new DalDbException(e);
			}
		}

		@Override
		public EntityIterator<? extends GenotypeAlias> createIdIterator(
				String id, int firstRecord, int nRecords, String filterClause)
		throws DalDbException {
			
			String sql = genotypeAliasFactory.createListAliasQuery(id, firstRecord, nRecords, filterClause);
			
			try {
				// TODO query across both? but what about the JOIN?
				Statement stmt = SqlUtil.createQueryStatement(bmsConnections.centralConnection);
				ResultSet rs = stmt.executeQuery(sql);
				
				return new ResultSetEntityIterator<GenotypeAlias>(stmt, rs, genotypeAliasFactory);
			} catch (SQLException e) {
				throw new DalDbException(e);
			}
		}
	};

	public BMS_DalDatabase(Closure<String> progress, boolean initialise, JdbcConnectionParameters localParams, JdbcConnectionParameters centralParams) throws DalDbException {
		super("BMS-Interop[Central=" + centralParams + " Local=" + localParams + "]");
		
		this.localParams = localParams;
		this.centralParams = centralParams;
		
		if (localParams != null) {
			String local   = StringUtil.substringBefore(localParams.connectionUrl, "?");
			String central = StringUtil.substringBefore(centralParams.connectionUrl, "?");
			if (local.equals(central)) {
				throw new DalDbException("Local and Central connectionUrls may NOT point at the same database:: " + local);
			}
		}

		
		entityClassByName.put("genus", Genus.class);
		entityClassByName.put("genotype", Genotype.class);
		
		if (initialise) {
			getBmsConnections(progress, true);
		}
	}



	@Override
	public boolean isInitialiseRequired() {
		return true;
	}

	@Override
	public void initialise(Closure<String> progress)
	throws DalDbException {
		getBmsConnections(progress, true);
	}
	
	private BmsConnectionInfo getBmsConnections(Closure<String> progress, boolean createIfNotPresent) throws DalDbException {
		if (bmsConnections == null && createIfNotPresent) {
			bmsConnections = new BmsConnectionInfo(localParams, centralParams, progress);
		}
		return bmsConnections;
	}

	@Override
	public String getDatabaseVersion(DalSession session) {
		return DATABASE_VERSION;
	}

	@Override
	public List<DalOperation> getOperations() {
		if (operations==null) {
			synchronized (this) {
				if (operations == null) {
					List<DalOperation> tmp = new ArrayList<DalOperation>();
					
					tmp.add(createOperation("get/genus/_id", Genus.class, genusProvider));
					tmp.add(createOperation("list/genus", Genus.class, genusProvider));
					
					tmp.add(createOperation("get/genotype/_id", Genotype.class, genotypeProvider));
					tmp.add(createOperation("list/genotype/_nperpage/page/_num", Genotype.class, genotypeProvider));
					
					tmp.add(createOperation("get/genotypealias/_id", GenotypeAlias.class, genotypeAliasProvider));
					tmp.add(createOperation("list/genotypealias/_nperpage/page/_num", GenotypeAlias.class, genotypeAliasProvider));

//					tmp.add(createOperation("genus/_genusid/list/genotype", Genotype.class, genusGenotypeProvider));
//					tmp.add(createOperation("genus/_genusid/list/genotype/_nperpage/page/_num", Genotype.class, genusGenotypeProvider));
					
					tmp.add(createOperation("genotype/_genoid/list/alias", GenotypeAlias.class, genotypeAliasProvider));
					
					operations = tmp;
				}
			}
		}
		return operations;
	}


	private Map<Pattern, MatcherToOperation> factoryByPattern;
	
	private Collection<String> entityNames;

	@SuppressWarnings("unchecked")
	private Closure<String> defaultProgress = ClosureUtils.nopClosure();
	
	static interface MatcherToOperation {
		public DalOperation makeOperation(Matcher m, Class<? extends DalEntity> entityClass, EntityProvider<? extends DalEntity> provider);
	}
	
	
	protected DalOperation createOperation(String template, 
			Class<? extends DalEntity> entityClass, 
			EntityProvider<? extends DalEntity> provider) 
	{
		if (factoryByPattern==null) {
			factoryByPattern = createFactoryByPattern();
		}
		
		DalOperation result = null;

		for (Pattern p : factoryByPattern.keySet()) {
			Matcher m = p.matcher(template);
			if (m.matches()) {
				result = factoryByPattern.get(p).makeOperation(m, entityClass, provider);
				break;
			}
		}
		
		if (result == null) {
			throw new IllegalArgumentException("Unsupported operation template: '" + template + "'");
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public void setDefaultProgress(Closure<String> p) {
		this.defaultProgress = p!=null ? p : ClosureUtils.nopClosure();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map<Pattern, MatcherToOperation> createFactoryByPattern() {
		
		Map<Pattern,MatcherToOperation> map = new HashMap<Pattern,MatcherToOperation>();
		
		map.put(GetOperation.PATTERN, new MatcherToOperation() {
			@Override
			public DalOperation makeOperation(Matcher m, Class<? extends DalEntity> entityClass, EntityProvider<? extends DalEntity> provider) {
				String entity = GetOperation.getEntityName(m);
				return new GetOperation(BMS_DalDatabase.this, entity, "get/" + entity + "/_id", entityClass, provider);
			}
		});
		
		map.put(SimpleListOperation.PATTERN,  new MatcherToOperation() {
			@Override
			public DalOperation makeOperation(Matcher m, Class<? extends DalEntity> entityClass, EntityProvider<? extends DalEntity> provider) {
				String entity = SimpleListOperation.getEntityName(m);
				return new SimpleListOperation(BMS_DalDatabase.this, entity, entityClass, provider);
			}	
		});
		
		map.put(PagedListOperation.PATTERN, new MatcherToOperation() {
			@Override
			public DalOperation makeOperation(Matcher m, Class<? extends DalEntity> entityClass, EntityProvider<? extends DalEntity> provider) {
				String entity = PagedListOperation.getEntityName(m);
				return new PagedListOperation(BMS_DalDatabase.this, entity, entityClass, provider);
			}
		});
		
		map.put(GenotypeListAliasOperation.PATTERN, new MatcherToOperation() {
			@Override
			public DalOperation makeOperation(Matcher m, Class<? extends DalEntity> entityClass, EntityProvider<? extends DalEntity> provider) {
				return new GenotypeListAliasOperation(BMS_DalDatabase.this, (EntityProvider<GenotypeAlias>) provider);
			}
		});
		
//		map.put(GenotypeListSpecimenOperation.PATTERN, new MatcherToOperation() {
//			@Override
//			public DalOperation makeOperation(Matcher m, Class<? extends DalEntity> entityClass, EntityProvider<? extends DalEntity> provider) {
//				return new GenotypeListSpecimenOperation(BMS_DalDatabase.this, (EntityProvider<Specimen>) provider);
//			}
//		});
		
		return map;
	}

	@Override
	public Collection<String> getEntityNames() {
		if  (entityNames == null) {
			Set<String> set = new HashSet<String>();
			for (DalOperation op : getOperations()) {
				set.add(op.getEntityName());
			}
			entityNames = set;
		}
		return entityNames;
	}

	@Override
	public SystemGroupInfo getSystemGroupInfo(DalSession session) throws DalDbException {
		
		BMS_UserInfo userInfo = userInfoBySessionId.get(session.sessionId);
		
		if (userInfo == null) {
			throw new DalDbException("Not logged in");
		}

		UdfldsRecord udfldsRecord = bmsConnections.userTypesByFldno.get(userInfo.utype);
		if (udfldsRecord == null) {
			throw new DalDbException("Missing UDFLDS record for utype=" + userInfo.utype);
		}
		
		boolean owner = udfldsRecord.fname.contains("ADMINISTRATOR");
		
		BMS_SystemGroupInfo result = new BMS_SystemGroupInfo(session.getGroupId(), udfldsRecord.fname, owner);
		
		return result;
	}
	
	@Override
	public void performListAllGroup(DalSession session, DalResponseBuilder builder,
			String[] returnSql) throws DalDbException
	{
		BMS_UserInfo userInfo = userInfoBySessionId.get(session.sessionId);
		if (userInfo == null) {
			throw new DalDbException("Not logged in");
		}

		builder.addResponseMeta("SystemGroup");

		for (UdfldsRecord r : bmsConnections.userTypesByFldno.values()) {
			builder.startTag("SystemGroup")
				.attribute("SystemGroupId", Integer.toString(r.fldno))
				.attribute("SystemGroupName", r.fcode)
				.attribute("SystemGroupDescription", r.fname)
			.endTag();
		}
	}

	@Override
	public void performListGroup(DalSession session, DalResponseBuilder builder, String[] returnSql) 
	throws DalDbException 
	{
		BMS_UserInfo userInfo = userInfoBySessionId.get(session.sessionId);
		if (userInfo == null) {
			throw new DalDbException("Not logged in");
		}

		builder.addResponseMeta("SystemGroup");

		UdfldsRecord r = bmsConnections.userTypesByFldno.get(userInfo.utype);
		if (r == null) {
			System.err.println("WARNING: Missing UDFLDS record for utype=" + userInfo.utype);

			builder.startTag("SystemGroup")
			.attribute("SystemGroupId", "0")
			.attribute("SystemGroupName", "Unknown-" + userInfo.utype)
			.attribute("SystemGroupDescription", "Missing UDFLDS record for utype=" + userInfo.utype)
			.endTag();
		}
		else {
			builder.startTag("SystemGroup")
			.attribute("SystemGroupId", Integer.toString(r.fldno))
			.attribute("SystemGroupName", r.fcode)
			.attribute("SystemGroupDescription", r.fname)
			.endTag();
		}
	}
	
	@Override
	public UserInfo doLogin(String newSessionId, final String userName, SessionExpiryOption seo,
			final Map<String, String> parms) throws AuthenticationException 
	{
		String sql = "SELECT userid, upswd, instalid, ustatus, uaccess, utype, personid, adate FROM users WHERE uname = '" + DbUtil.doubleUpSingleQuote(userName.toUpperCase()) + "'";

		final BMS_UserInfo[] result = new BMS_UserInfo[1];
		ResultSetVisitor visitor = new ResultSetVisitor() {
			@Override
			public Continue visit(ResultSet rs) {
				try {
					int userid = rs.getInt(1);
					String pswd = rs.getString(2);
					
					
					int instalid = rs.getInt(3);
					int ustatus = rs.getInt(4);
					int uaccess = rs.getInt(5);
					int utype = rs.getInt(6);
					int pid = rs.getInt(7);
					String adate = rs.getString(8);
					
					String errmsg = DalDatabaseUtil.getUsernamePasswordErrorMessage(userName, pswd, parms);
					
					if (errmsg != null) {
						return Continue.error(new AuthenticationException(errmsg));
					}
					
					result[0] = new BMS_UserInfo(userName, userid,
							instalid, ustatus, uaccess, utype, pid, adate);
					
					return Continue.STOP; // Only want the first match?
				} catch (SQLException e) {
					return Continue.error(e);
				}
			}
		};

		Continue qResult = null;

		try {
			qResult= SqlUtil.performQuery(getBmsConnections(defaultProgress, true).centralConnection, sql, visitor);
		} catch (DalDbException e) {
			throw new AuthenticationException(e);
		}
		
		if (qResult.isError()) {
			Throwable t = qResult.throwable;
			if (t instanceof AuthenticationException) {
				throw (AuthenticationException) t;
			}
			throw new AuthenticationException("Internal error", qResult.throwable);
		}
		
		BMS_UserInfo ui = result[0];
		
		if (ui == null) {
			throw new AuthenticationException("Invalid username or password");
		}
		
		userInfoBySessionId.put(newSessionId, ui);
		
		return ui;	
	}

	@Override
	public String getDatabasePath() {
		StringBuilder sb = new StringBuilder();
		sb.append(centralParams)
			.append("$$")
			.append(localParams);
		return sb.toString();
	}

	@Override
	public void shutdown() throws DalDbException {
		if (bmsConnections != null) {
			try {
				bmsConnections.closeConnections();
			} finally {
				bmsConnections = null;
			}
		}
	}

	@Override
	public Class<? extends DalEntity> getEntityClass(String tname) {
		Class<? extends DalEntity> result = null;
		for (String name : entityClassByName.keySet()) {
			if (name.equalsIgnoreCase(tname)) {
				result = entityClassByName.get(name);
				break;
			}
		}
		return result;
	}

	@Override
	public void performListField(DalSession session, String tableName,
			DalResponseBuilder responseBuilder) 
	throws DalDbException {
		
		Class<? extends DalEntity> entityClass = getEntityClass(tableName);
		if (entityClass == null) {
			throw new DalDbException("Unknown tableName: '" + tableName + "'");
		}
		
		DalDatabaseUtil.addEntityFields(entityClass, responseBuilder);
	}
	
	@Override
	public void doLogout(DalSession session) {
		recordCountCache.removeEntriesFor(session);
		BMS_UserInfo ui = userInfoBySessionId.remove(session.sessionId);
		if (ui != null) {
			// Do something else maybe ?
		}
	}

	private final RecordCountCache recordCountCache = new RecordCountCacheImpl();

	public RecordCountCacheEntry getRecordCountCacheEntry(DalSession session, Class<?> entityClass) {
		return recordCountCache.getEntry(session, entityClass);
	}

	public void setRecordCountCacheEntry(DalSession session, Class<?> entityClass, String filterClause, int count) {
		recordCountCache.setEntry(session, entityClass, filterClause, count);
	}
}
