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
package com.diversityarrays.dal.sqldb;

import java.util.Properties;

public class JdbcConnectionParameters {
	public final String connectionUrl;
	
	public final String userName;
	public final String password;
	public final Properties properties;
	
	public JdbcConnectionParameters(String url, String uname, String pword) {
		this(url, uname, pword, null);
	}
	
	public JdbcConnectionParameters(String url, String uname, String pword, Properties props) {
		this.connectionUrl = url;
		this.userName = uname;
		this.password = pword;
		this.properties = props;
	}
	
	@Override
	public String toString() {
		return connectionUrl;
	}
}